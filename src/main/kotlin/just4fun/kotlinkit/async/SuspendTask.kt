package just4fun.kotlinkit.async

import just4fun.kotlinkit.async.ResultTaskState.*
import just4fun.kotlinkit.Result
import just4fun.kotlinkit.Safely
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.EmptyCoroutineContext as Empty
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.createCoroutineUnchecked


/* interface */

interface SuspensionExecutor {
	fun <R, T> suspension(receiver: R, context: CoroutineContext? = null, code: suspend R.() -> T): AsyncResult<T> = SuspendTask<T>().start(receiver, context, code)
	
	fun <T> suspension(context: CoroutineContext? = null, code: suspend TaskContext.() -> T): AsyncResult<T> = SuspendTask<T>().start(context, code)
	
	suspend fun <R, T> suspended(receiver: R, context: CoroutineContext? = null, code: suspend R.() -> T): Result<T> = SuspendTask<T>().startSuspended(receiver, context, code)
	
	suspend fun <T> suspended(context: CoroutineContext? = null, code: suspend TaskContext.() -> T): Result<T> = SuspendTask<T>().startSuspended(context, code)
}




// TODO leaks interceptor thread to child and parent. But lacks the custom executor to redirect resumption.

open class SuspendTask<T>: ResultTask<T>(), Continuation<T> {
	
	companion object {
		fun <R, T> async(receiver: R, context: CoroutineContext? = null, code: suspend R.() -> T): AsyncResult<T> = SuspendTask<T>().start(receiver, context, code)
		
		fun <T> async(context: CoroutineContext? = null, code: suspend TaskContext.() -> T): AsyncResult<T> = SuspendTask<T>().start(context, code)
		
		suspend operator fun <R, T> invoke(receiver: R, context: CoroutineContext? = null, code: suspend R.() -> T): Result<T> = SuspendTask<T>().startSuspended(receiver, context, code)
		
		suspend operator fun <T> invoke(context: CoroutineContext? = null, code: suspend TaskContext.() -> T): Result<T> = SuspendTask<T>().startSuspended(context, code)
	}
	
	/* instance code */
	
	internal var parent: SuspendTask<*>? = null
	internal var child: SuspendTask<*>? = null
	internal var thread: Thread? = null
	override var context: CoroutineContext = Empty
	private var continuation: Continuation<Result<T>>? = null
	private lateinit var interceptor: Interceptor
	@PublishedApi internal var activeTask: ActiveTask? = null
	private var active: SuspendTask<*>
		set(value) = run { activeTask!!.active = value }
		get() = activeTask!!.active
	
	suspend fun preStartSuspended(context: CoroutineContext?): Result<T> = suspendCoroutine { cont ->
		val option = synchronized(lock) {
			if (state == CREATED) if (init(context, cont)) 0 else 1 else 2
		}
		if (option == 0) Unit
		else if (option == 1) cancel()
		else cont.resume(Result(IllegalStateException("The task can be pre-started once.")))
	}
	
	suspend /*inline*/ fun <S: SuspendTask<T>> preStartSuspended(context: CoroutineContext?, /*crossinline*/ config: (self: S) -> Unit): Result<T> = suspendCoroutine { cont ->
		val option = synchronized(lock) {
			if (state == CREATED) if (init(context, cont)) 0 else 1 else 2
		}
		if (option == 0) Safely({ config(this as S) }, { cancel() })
		else if (option == 1) cancel()
		else cont.resume(Result(IllegalStateException("The task can be pre-started once.")))
	}
	
	suspend fun startSuspended(context: CoroutineContext?, code: suspend TaskContext.() -> T): Result<T> = startSuspended(this, context, code)
	
	suspend fun <R> startSuspended(receiver: R, context: CoroutineContext?, code: suspend R.() -> T): Result<T> {
		return suspendCoroutine { cont ->
			val option = synchronized(lock) {
				if (state == CREATED) if (init(context, cont)) 0 else 1 else 2
			}
			if (option == 0) execute(receiver, code)
			else if (option == 1) cancel()
			else cont.resume(Result(IllegalStateException("The task can be pre-started once.")))
		}
	}
	
	fun start(context: CoroutineContext?, code: suspend TaskContext.() -> T) = start(this, context, code)
	
	fun <R> start(receiver: R, context: CoroutineContext?, code: suspend R.() -> T): SuspendTask<T> {
		val option = synchronized(lock) {
			if (state == CREATED) init(context, null) else state == INITED
		}
		if (option) execute(receiver, code)
		return this
	}
	
	private fun <R> execute(receiver: R, code: suspend R.() -> T) {
		val option = synchronized(lock) {
			if (state == INITED) if (setActive()) {
				state = RUNNING
				code.createCoroutineUnchecked(receiver, this)
			} else 1 else 2
		}
		if (option == 1) cancel()
		else if (option == 2) Unit//  cancelled and continued
		else (option as Continuation<Unit>).resume(Unit)
	}
	
	@PublishedApi internal fun init(c: CoroutineContext?, cont: Continuation<Result<T>>?): Boolean {
		val orig = cont?.context
		activeTask = if (orig == null) ActiveTask(this).apply { context = this }
		else (orig as? ActiveTask)?.apply { context = this }
		  ?: orig[ActiveTask]?.apply { context = orig }
		  ?: ActiveTask(this).apply { context = orig + this }
		val icpr = if (c == null) null else c as? ContinuationInterceptor ?: c[ContinuationInterceptor]?.apply { context += c }
		interceptor = if (icpr == null) Interceptor() else Interceptor1(icpr)
		context += interceptor
		continuation = cont
		state = INITED
		val inited = setParent()
		lock = activeTask!!
		return inited
	}
	
	open protected fun onComplete(result: Result<T>): Unit = Unit
	
	
	final override fun onComplete(executor: Executor?, precede: Boolean, reaction: Reaction<T>): SuspendTask<T> {
		super.onComplete(executor, precede, reaction)
		return this
	}
	
	final override fun cancel(value: T, interrupt: Boolean): Unit = complete(Result(value), true, interrupt)
	final override fun cancel(cause: Throwable, interrupt: Boolean) = complete(Result(cause), true, interrupt)
	final override fun resume(value: T) = complete(Result(value), false, false)
	final override fun resumeWithException(exception: Throwable) = complete(Result(exception), false, false)
	
	override final fun complete(res: Result<T>, cancelled: Boolean, interrupt: Boolean) {
		val prevState = state
		synchronized(lock) {
			// even if task was cancelled, if it wasn't intercepted to other thread it should be tracked here to detect actual resume
			if (state == RUNNING && (parent == null || !parent!!.isComplete)) {
				if (cancelled && interrupt) active.thread?.interrupt()
				active = parent ?: this
			}
			if (!cancelled) {
				if (active === parent && !active.isComplete && active.thread == null) active.thread = Thread.currentThread()
				if (state == CANCELLED) Thread.interrupted()
			}
			//			log(this, "${if (state == RUNNING && (parent == null || !parent!!.isComplete)) "▲" else "△"}  parent [$parent]   active [$active];   $state;   cancelled? $cancelled;   activeThr? ${active.thread != null}")
			if (state > RUNNING) return
			state = if (cancelled) CANCELLED else EXECUTED
			result = res
		}
		if (cancelled) child?.cancel(res.exception!!, interrupt)
		if (reaction != null) Safely { reaction!!.invoke(res) }
		onComplete(res)
		// doesn't resume right away if wasn't intercepted to other thread. Should wait actual resume..
		continuation?.resume(res)
		child = null
	}
	
	/* active tracking */
	
	private fun setParent(): Boolean {
		return if (active === this) true
		else if (Thread.currentThread() == active.thread) {
			active.child = this
			parent = active
			//			log(this, "▶  parent [$active]${if (active.isComplete) "  parent complete" else ""}")
			!active.isComplete
		} else false
	}
	
	private fun setActive(): Boolean {
		return if (active === this) true
		else if (parent === active && !active.isComplete) {
			active.child = this
			active = this
			//			log(this, "▼")
			true
		} else false
	}
	
	
	
	
	
	
	
	/* INTERCEPTORs */
	
	internal inner open class Interceptor: AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor, Continuation<Any?> {
		override val context = this@SuspendTask.context
		open val interceptor: ContinuationInterceptor? = null
		protected lateinit var continuation: Continuation<Any?>
		
		// WARN due to glitch in Coroutine API this method can be called twice
		override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
			synchronized(lock) { if (this@SuspendTask === active) parent?.thread = null }
			//			if (parent != null && parent!!.thread != thread) parent!!.thread = null
			this.continuation = continuation as Continuation<Any?>
			return this
		}
		
		override fun resume(value: Any?) {
			thread = Thread.currentThread()
			continuation.resume(value)
			thread = null
		}
		
		override fun resumeWithException(exception: Throwable) {
			continuation.resumeWithException(exception)
		}
	}
	
	
	internal inner class Interceptor1(override val interceptor: ContinuationInterceptor): Interceptor() {
		private lateinit var interception: Continuation<Any?>
		private var inited = false
		private var expect = false
		
		override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
			if (!inited) interception = interceptor.interceptContinuation(this)// track second callback and call once
			return super.interceptContinuation(continuation)
		}
		
		override fun resume(value: Any?) {
			if (!expect && interceptor !== (if (inited) child else parent)?.interceptor?.interceptor) {
				expect = true
				Safely({ interception.resume(value) }, { resumeWithException(it) })
				return
			}
			expect = false; inited = true
			super.resume(value)
		}
		
		override fun resumeWithException(exception: Throwable) {
			expect = false; inited = true
			super.resumeWithException(exception)
		}
	}
	
}





/* ACTIVE CONTEXT */

internal class ActiveTask(var active: SuspendTask<*>): AbstractCoroutineContextElement(ActiveTask) {
	companion object Key: CoroutineContext.Key<ActiveTask>
}


/**
△▷▽◁◈◾
▶ - init child coroutine
▼ - start coroutine. interecept initial resumption
▽ - interecept resumption from suspension (if thread was changed)
○ - coroutine body code
▲ - complete coroutine
◀ - resume parent coroutine or exit
◦
▶▼
◦○
◦▶▼
◦◦○
◦◦▶▼
◦◦◦○
◦◦◀▲
◦◦▽
◦◦○
◦◦▶▼
◦◦◦○
◦◦◦▶▼
◦◦◦◦○
◦◦◦◀▲
◦◦◦▽
◦◦◦○
◦◦◀▲
◦◦▽
◦◦○
◦◀▲
◦▽
◦○
◀▲
 
 */