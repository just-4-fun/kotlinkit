package just4fun.kotlinkit.async

import just4fun.kotlinkit.async.ResultTaskState.CANCELLED
import just4fun.kotlinkit.async.ResultTaskState.RUNNING
import just4fun.kotlinkit.async.ResultTaskState.EXECUTED
import just4fun.kotlinkit.Result
import just4fun.kotlinkit.Safely
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import kotlin.concurrent.thread


/* AsyncResult */

/** Provides information about whether the task in which execution takes place is cancelled. */
interface TaskContext {
	/** Provides information about whether the task in which execution takes place is cancelled. */
	val isCancelled: Boolean
}

/** The [Result] which may not be currently available but can be obtained on completion of some async execution via the [onComplete] callback.
 */
interface AsyncResult<T>: TaskContext {
	/** Checks if the result is ready. */
	val isComplete: Boolean
	
	/** Receives the result as soon as it's ready and handles it via the [reaction]. There can be more than one reaction which form the cueue.
	 * The [executor] defines which thread the [reaction] will run in. If `null`, the [reaction] runs in the task execution thread.
	 * [precede] places the [reaction] on the top of the chain of reactions.
	 */
	fun onComplete(executor: Executor? = null, precede: Boolean = false, reaction: (Result<T>) -> Unit): AsyncResult<T>
	
	/** Cancels the current task with the [cause] */
	fun cancel(cause: Throwable = CancellationException(), interrupt: Boolean = false) = Unit
	
	/** Cancels the current task with the [value] */
	fun cancel(value: T, interrupt: Boolean = false) = Unit
}


/** Creates complete [AsyncResult] with the [result] */
class ReadyAsyncResult<T>(private val result: Result<T>): AsyncResult<T> {
	override val isCancelled = false
	override val isComplete = true
	override fun onComplete(executor: Executor?, precede: Boolean, reaction: (Result<T>) -> Unit) = apply { reaction(result) }
}




/* ResultTask */

typealias Reaction<T> = (Result<T>) -> Unit

enum class ResultTaskState { CREATED, INITED, RUNNING, CANCELLED, EXECUTED }


/** [AsyncResult] with implemented [onComplete] method */
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
	
	protected open fun complete(res: Result<T>, cancelled: Boolean = false, interrupt: Boolean = false): Unit {
		synchronized(lock) {
			state = EXECUTED
			result = res
		}
		reaction?.let { Safely { it.invoke(res) } }
	}
	
	
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




/* Basic ResultTask implementations */

/** [AsyncResult] of the [code] execution that runs in separate thread. */
class ThreadTask<T>(private val code: () -> T): ResultTask<T>() {
	init {
		thread { complete(Result { code() }) }
	}
}



/** [AsyncResult] of the [code] execution that runs in separate thread. */
class ThreadRTask<T>(private val code: () -> Result<T>): ResultTask<T>() {
	init {
		thread { complete(code()) }
	}
}
