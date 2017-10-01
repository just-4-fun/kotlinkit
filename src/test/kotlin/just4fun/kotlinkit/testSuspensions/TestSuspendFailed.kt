package just4fun.kotlinkit.testSuspensions

import just4fun.kotlinkit.async.SuspendTask
import just4fun.kotlinkit.logL
import testSuspensions.Oops
import kotlin.concurrent.thread
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor


fun main(args: Array<String>) {
	testFailure()
}

fun testFailure() {
	SuspendTask<Int>().start(FailInterceptor) {
		logL(1, "0", "Before throw")
		throw Oops()
		1
	}.onComplete {
		println("COMPLETE: $it")
	}
	logL(1, "Main", "after all")
}

object FailInterceptor: AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
	override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = object: Continuation<T> {
		override val context = continuation.context
		override fun resume(value: T) {
			thread {
				logL(1, "Interceptor", "Before resume value $value")
				continuation.resume(value)// WARN:  this call is always safe guarding by Coroutine framework.
				logL(1, "Interceptor", "After resume value $value")
			}
		}
		override fun resumeWithException(exception: Throwable) {
			logL(1, "Interceptor", "Before resumeWithException  $exception")
			continuation.resumeWithException(exception)
			logL(1, "Interceptor", "After resumeWithException  $exception")
		}
	}
}
