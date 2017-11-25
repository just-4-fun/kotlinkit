package just4fun.kotlinkit.async

import java.util.concurrent.Executor
import just4fun.kotlinkit.async.ResultTaskState.*
import just4fun.kotlinkit.Result
import just4fun.kotlinkit.lazyVar
import just4fun.kotlinkit.Safely
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.lang.System.currentTimeMillis as now

/** [AsyncResult] of the [code] execution.
 * Can be delayed by [delayMs] milliseconds.
 * Can run in the [executor] thread if one is specified. Otherwise runs in the [AsyncTask.sharedContext].
 */
open class AsyncTask<T>(val delayMs: Int = 0, val executor: Executor? = null, val code: TaskContext.() -> T): ResultTask<T>(), Runnable, Comparable<AsyncTask<*>> {
	
	companion object {
		var sharedContext: ThreadContext by lazyVar { DefaultThreadContext(1000) }
		private var SEQ = 0L
		private val nextSeqId get() = SEQ++
	}
	
	
	val runTime: Long = delayMs + now()
	private var thread: Thread? = null
	private var runNow = executor == null
	internal var index = -1// optimizes task removal
	private val seqId = nextSeqId// helps compare tasks with equal runTime when order matters
	var wrapper: Runnable? = null// used to cancel the task scheduled by [ScheduledExecutorService]
	
	init {
		schedule()
	}
	
	private fun schedule() = executor.let {
		when (it) {
			is ThreadContext -> run { runNow = true; it.schedule(this) }
			is ScheduledThreadPoolExecutor -> run { runNow = true; it.schedule(this, delayMs.toLong(), TimeUnit.MILLISECONDS) }
			else -> sharedContext.schedule(this)
		}
	}
	
	override fun cancel(value: T, interrupt: Boolean): Unit = complete(Result(value), true, interrupt)
	override fun cancel(cause: Throwable, interrupt: Boolean) = complete(Result(cause), true, interrupt)
	
	override fun run() {
		if (runNow) run { runNow(); return }
		synchronized(lock) { if (state > CREATED) return else state = INITED }
		try {
			runNow = true
			executor?.execute(this)
		} catch (x: Throwable) {
			complete(Result<T>(x), false, false)
		}
	}
	
	private fun runNow() {
		synchronized(lock) { if (state > INITED) return else state = RUNNING }
		thread = Thread.currentThread()
		val res = try {
			Result(code(this))
		} catch (x: Throwable) {
			Result<T>(x)
		}
		thread = null
		complete(res, false, false)
		Thread.interrupted()// clears interrupted status if any
	}
	
	override final fun complete(res: Result<T>, cancelled: Boolean, interrupt: Boolean): Unit {
		synchronized(lock) {
			if (state > RUNNING) return
			val remove = cancelled && state == CREATED
			state = if (cancelled) CANCELLED else EXECUTED
			result = res
			if (interrupt) thread?.interrupt()
			if (remove) onRemove()
		}
		reaction?.let { Safely { it.invoke(res) } }
	}
	
	fun onRemove() = executor.let {
		when (it) {
			is ThreadContext -> it.remove(this)
			is ScheduledThreadPoolExecutor -> it.remove(this)
			else -> sharedContext.remove(this)
		}
	}
	
	override fun onComplete(executor: Executor?, precede: Boolean, reaction: Reaction<T>): AsyncTask<T> {
		super.onComplete(executor, precede, reaction)
		return this
	}
	
	override fun compareTo(other: AsyncTask<*>): Int {
		if (this === other) return 0
		val diff = runTime - other.runTime
		return if (diff < 0) -1 else if (diff > 0) 1 else if (seqId < other.seqId) -1 else 1
	}
	
	fun runCopy(delay: Int = -1) = AsyncTask(if (delay < 0) delayMs else delay, executor, code).also { if (reaction != null) it.onComplete(null, false, reaction!!) }
}

