package just4fun.kotlinkit.async

import java.util.concurrent.Executor
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor


/** Combines an interface of [AsyncTask] scheduler with [ContinuationInterceptor] and [Executor].
 * May have an owner [ownerToken] which is used in the combination with [requestShutdown] method to check whether the caller has the authority to shut down the context object.
 */
abstract class ThreadContext: AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor, Executor {
	/** Owner which is authorized to shut the context down. */
	var ownerToken: Any? = null
	
	/** Schedules the [task] */
	abstract fun schedule(task: AsyncTask<*>): Unit
	
	/** Removes the [task] */
	abstract fun remove(task: AsyncTask<*>): Unit
	
	/** Shuts down this context within [await] milliseconds */
	abstract fun shutdown(await: Int = 0): Unit
	
	/** Shuts down this context within [await] milliseconds if the [callerToken] is equal to the [ownerToken]. */
	fun requestShutdown(callerToken: Any, await: Int = 0) {
		if (ownerToken === callerToken) shutdown(await)
	}
	
	
	override fun <T> interceptContinuation(continuation: Continuation<T>) = object: Continuation<T> {
		override val context = continuation.context
		override fun resume(value: T) = execute { continuation.resume(value) }
		override fun resumeWithException(exception: Throwable) = continuation.resumeWithException(exception)
	}
}

