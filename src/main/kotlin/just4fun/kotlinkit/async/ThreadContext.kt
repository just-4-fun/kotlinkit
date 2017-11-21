package just4fun.kotlinkit.async

import java.util.concurrent.Executor
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor



abstract class ThreadContext: AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor, Executor {
	/** Assigned by framework. */
	var ownerToken: Any? = null
	
	/** Called by framework. */
	abstract fun schedule(task: AsyncTask<*>): Unit
	
	/** Called by framework. */
	abstract fun remove(task: AsyncTask<*>): Unit
	
	abstract fun shutdown(await: Int = 0): Unit
	
	/** Called by framework. */
	fun requestShutdown(callerToken: Any, await: Int = 0) {
		if (ownerToken === callerToken) shutdown(await)
	}
	
	override fun <T> interceptContinuation(continuation: Continuation<T>) = object: Continuation<T> {
		override val context = continuation.context
		override fun resume(value: T) = execute { continuation.resume(value) }
		override fun resumeWithException(exception: Throwable) = continuation.resumeWithException(exception)
	}
}

