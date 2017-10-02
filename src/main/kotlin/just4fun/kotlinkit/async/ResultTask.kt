package just4fun.kotlinkit.async

import just4fun.kotlinkit.async.ResultTaskState.CANCELLED
import just4fun.kotlinkit.async.ResultTaskState.RUNNING
import just4fun.kotlinkit.Result
import just4fun.kotlinkit.Safely
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor


typealias Reaction<T> = (Result<T>) -> Unit


interface AsyncResult<T>: TaskContext {
	val isComplete: Boolean
	fun onComplete(executor: Executor? = null, precede: Boolean = false, reaction: (Result<T>) -> Unit): AsyncResult<T>
	fun cancel(cause: Throwable = CancellationException(), interrupt: Boolean = false): Unit
	fun cancel(value: T, interrupt: Boolean = false): Unit
}


interface TaskContext {
	val isCancelled: Boolean
}


enum class ResultTaskState {CREATED, INITED, RUNNING, CANCELLED, EXECUTED }


abstract class ResultTask<T>: AsyncResult<T> {
	var state: ResultTaskState = ResultTaskState.CREATED
		protected set(value) = run { field = value }
		get() = synchronized(lock) { field }
	var result: Result<T>? = null
		protected set(value) = run { field = value }
		get() = synchronized(lock) { field }
	final override val isComplete get() = synchronized(lock) { state > RUNNING }
	final override val isCancelled get() = synchronized(lock) { state == CANCELLED }
	protected var reaction: Reaction<T>? = null
	@Suppress("LeakingThis") @PublishedApi internal var lock: Any = this
	
	inline protected fun <R> synchronize(code: () -> R): R = synchronized(lock) { code() }
	
	override fun onComplete(executor: Executor?, precede: Boolean, reaction: Reaction<T>): ResultTask<T> {
		synchronized(lock) {
			if (state > RUNNING) return@synchronized
			val prevReaction = this.reaction
			this.reaction = if (prevReaction == null) {
				if (executor == null) reaction
				else { res -> executor.execute { reaction(res) } }
			} else { res ->
				if (executor == null) {
					if (precede) run { Safely { reaction(res) }; prevReaction(res) }
					else run { Safely { prevReaction(res) }; reaction(res) }
				} else {
					if (precede) run { Safely { executor.execute { reaction(res) } }; prevReaction(res) }
					else run { Safely { prevReaction(res) }; executor.execute { reaction(res) } }
				}
			}
			return this
		}
		reaction.invoke(result!!)
		return this
	}
}



class FailedAsyncResult<T>(val exception: Throwable): AsyncResult<T> {
	override val isCancelled = false
	override val isComplete = true
	override fun cancel(value: T, interrupt: Boolean) = Unit
	override fun cancel(cause: Throwable, interrupt: Boolean) = Unit
	override fun onComplete(executor: Executor?, precede: Boolean, reaction: (Result<T>) -> Unit) = apply { reaction(Result(exception)) }
}