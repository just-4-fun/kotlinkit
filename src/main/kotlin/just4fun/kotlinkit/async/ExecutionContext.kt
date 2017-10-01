package just4fun.kotlinkit.async

import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor


open class ExecutionContextBuilder {
	val NONE: ExecutionContext? = null
	var SHARED: ExecutionContext
		get() = AsyncTask.sharedContext
		set(value) = run { AsyncTask.sharedContext = value.apply { owner = this } }
	open val newDEFAULT: ExecutionContext = DefaultExecutionContext()
	open fun newPOOL(scheduler: ScheduledThreadPoolExecutor): ExecutionContext = PoolExecutionContext(scheduler)
	open fun newPOOL(corePoolSize: Int = 1): ExecutionContext = PoolExecutionContext(ScheduledThreadPoolExecutor(corePoolSize, ThreadPoolExecutor.CallerRunsPolicy()))
}



abstract class ExecutionContext: AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor, Executor {
	/** Assigned by framework. */
	var owner: Any? = null
	
	/** Called by framework. */
	abstract fun onSchedule(task: AsyncTask<*>): Unit
	
	/** Called by framework. */
	abstract fun onRemove(task: AsyncTask<*>): Unit
	
	abstract fun resume(): Unit
	abstract fun pause(): Unit
	abstract fun shutdown(await: Int = 0): Unit
	
	/** Called by framework. */
	fun requestResume(callerToken: Any) {
		if (owner === callerToken) resume()
	}
	
	/** Called by framework. */
	fun requestPause(callerToken: Any) {
		if (owner === callerToken) pause()
	}
	
	/** Called by framework. */
	fun requestShutdown(callerToken: Any, await: Int = 0) {
		if (owner === callerToken) shutdown(await)
	}
	
	override fun <T> interceptContinuation(continuation: Continuation<T>) = object: Continuation<T> {
		override val context = continuation.context
		override fun resume(value: T) = execute { continuation.resume(value) }
		override fun resumeWithException(exception: Throwable) = continuation.resumeWithException(exception)
	}
}

