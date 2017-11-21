package testSuspensions

import just4fun.kotlinkit.Result
import just4fun.kotlinkit.async.AsyncResult
import just4fun.kotlinkit.async.SuspendTask
import just4fun.kotlinkit.async.TaskContext
import just4fun.kotlinkit.startTime
import just4fun.kotlinkit.N
import just4fun.kotlinkit.async.DefaultThreadContext
import just4fun.kotlinkit.logL
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.experimental.*



fun main(args: Array<String>) {
	test2()
//	test3()
//	test4()
//	test5()
//	testInterceptor()
}


//var executor = Executors.newFixedThreadPool(10)
//val testInterceptor = TestInterceptor()
var possible = true

val isInterrupted get() = Thread.currentThread().isInterrupted

fun test2() {
	val cancels0 = listOf<Long>(-1, 0, 200, 600)
	for (cancel0 in cancels0) for (cancel1 in 0..1) for (cancel2 in 0..1) for (par0 in 0..1) for (par1 in 0..1) for (par2 in 0..1) {
		println("")
		println("-------------------    $cancel0,  $cancel1,  $cancel2,        $par0,  $par1,  $par2   ---------------------")
		//							infoIds.set(0)
		N = 0
		val executor = Executors.newFixedThreadPool(10)
		var next = false
		thread {
			startTime = System.currentTimeMillis()
			suspended2(executor, cancel0, cancel1, cancel2, par0, par1, par2, true)
			  .onComplete {
				  next = true
			  }
		}
		while (!next) Thread.sleep(100)
		executor.awaitTermination(100, TimeUnit.MILLISECONDS)
		executor.shutdown()
	}
}

fun test3() {
	val cancels0 = listOf<Long>(-1/*, 0, 200, 600*/)
	//	for (cancel0 in cancels0) for (cancel1 in 0..1) for (cancel2 in 0..1)for (cancel3 in 0..1)for (cancel4 in 0..1) for (par0 in 0..1) for (par1 in 0..1) for (par2 in 0..1)for (par3 in 0..1)for (par4 in 0..1) {
	for (cancel0 in cancels0) for (cancel1 in 0..0) for (cancel2 in 0..1) for (cancel3 in 0..1) for (cancel4 in 0..0) for (par0 in 0..1) for (par1 in 0..1) for (par2 in 0..1) for (par3 in 0..1) for (par4 in 0..1) {
		println("")
		println("-------------------    $cancel0,  $cancel1,  $cancel2,  $cancel3,  $cancel4,        $par0,  $par1,  $par2,  $par3,  $par4   ---------------------")
		//							infoIds.set(0)
		N = 0
		var next = false
		val executor = Executors.newFixedThreadPool(10)
		thread {
			startTime = System.currentTimeMillis()
			suspended3(executor, cancel0, cancel1, cancel2, cancel3, cancel4, par0, par1, par2, par3, par4)
			  .onComplete {
				  next = true
			  }
		}
		while (!next) Thread.sleep(100)
		executor.awaitTermination(100, TimeUnit.MILLISECONDS)
		executor.shutdown()
	}
}

fun test4() {
	for (throw0 in 0..0) for (throw1 in 0..0) for (throw2 in 0..0) for (par0 in 0..0) for (par1 in 0..1) for (par2 in 0..1) {
		println("")
		println("-------------------    $throw0,  $throw1,  $throw2,         $par0,  $par1,  $par2   ---------------------")
		N = 0
		val executor = Executors.newFixedThreadPool(10)
		executor.shutdown()
		startTime = System.currentTimeMillis()
		suspended4(executor, throw0, throw1, throw2, par0, par1, par2)
		executor.awaitTermination(300, TimeUnit.MILLISECONDS)
		executor.shutdown()
		executor.awaitTermination(500, TimeUnit.MILLISECONDS)
	}
}

fun testInterceptor() {
	val breakpoint = 2  // 0 interrupts main thread; 2  interrupts executor thread; 1, 3 - completes
	val executor = Executors.newFixedThreadPool(10)
	val interceptor = object: AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
		override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = object: Continuation<T> {
			init {
				if (breakpoint == 0) throw Exception("Point $breakpoint")
			}
			
			override val context = continuation.context
			override fun resume(value: T) {
				logL(1, " EXEC ", "Before")
				if (breakpoint == 1) executor.shutdownNow()//throw Exception("Point $breapoint")
				executor.execute {
					logL(1, " EXEC ", "After")
					if (breakpoint == 2) throw Exception("Point $breakpoint") else continuation.resume(value)
				}
			}
			
			override fun resumeWithException(x: Throwable) {
				logL(1, " EXEC ", "resumeWithException $x")
				//				if (breapoint == 0) throw Exception("Point $breapoint")
				continuation.resumeWithException(x)
			}
		}
	}
	val susp = SuspendTask<Unit>()
	susp.start(interceptor) {
		logL(1, " SUSP ", "1")
		if (breakpoint == 3) throw Exception("Point $breakpoint")
		logL(1, " SUSP ", "2")
	}
	susp.onComplete {
		logL(1, " SUSP ", "Complete    $it")
		executor.shutdown()
	}
}

fun test5() {
	val executor = Executors.newFixedThreadPool(10)
	executeSuspension {
		val r1 = executeSuspended(context = TestInterceptor(executor)) {
			logL(1, " 1 ", "1")
			1
		}
		val r2 = executeSuspended(context = TestInterceptor(executor)) {
			logL(1, " 2 ", "1")
			1
		}
		r1.valueOr(0) + r2.valueOr(0)
	}.onComplete {
		logL(1, " 2 ", "Result= $it")
		executor.awaitTermination(100, TimeUnit.MILLISECONDS)
		executor.shutdown()
	}
	
}

fun suspended1(): AsyncResult<Int> = executeSuspension(-1) { logL(1, " 0", "0"); 1 }

fun suspended2(executor: Executor, cancel0: Long = -1, cancel1: Int = 0, cancel2: Int = 0, par0: Int = 0, par1: Int = 0, par2: Int = 0, iterrupt: Boolean = false): AsyncResult<*> {
	val icpr = TestInterceptor(executor);
	val icpr1 = icpr;
	val icpr2 = icpr
//	val icpr = TestInterceptor(executor); val icpr1 = TestInterceptor(executor); val icpr2 = icpr1
//	val icpr = TestInterceptor(executor); val icpr1 = TestInterceptor(executor); val icpr2 = TestInterceptor(executor)
	val op = executeSuspension(cancel0, if (par0 > 0) icpr else null, iterrupt) {
		logL(1, " 0 ", ">")
		val r1 = executeSuspended(if (cancel1 > 0) 200L else -1, if (par1 > 0) icpr1 else null, iterrupt) {
			logL(1, " 1 ", ">:  ? $isCancelled;  $isInterrupted")
			java.lang.Thread.sleep(400)
			logL(1, " 1 ", "X:  ? $isCancelled;  $isInterrupted")
			1
		}
		logL(1, " 0 ", "r1= $r1")
		val r2 = executeSuspended(if (cancel2 > 0) 200L else -1, if (par2 > 0) icpr2 else null, iterrupt) {
			logL(1, " 2 ", ">:  ? $isCancelled;  $isInterrupted")
			java.lang.Thread.sleep(400)
			logL(1, " 2 ", "X:  ? $isCancelled;  $isInterrupted")
			1
		}
		logL(1, " 0 ", "r2= $r2")
		val res = r1.valueOr(0) + r2.valueOr(0)
		res
	}
	op.onComplete { res ->
		logL(1, " 0 ", "X: Result= $res")
		if (cancel0 == 600L && cancel1 > 0 && cancel2 > 0 && par1 > 0 && par2 > 0) {
			if (res.value != 0) logL(1, "Exception -----------------------------------------------------------------------", "Result should be 0  VS   ${res.value}")
		} else if (cancel0 >= 0 && res.exception == null) logL(1, "Exception -----------------------------------------------------------------------", "Result should be Failure")
		else if (cancel0 < 0 && ((cancel1 > 0 && cancel2 == 0) || (cancel2 > 0 && cancel1 == 0)) && res.value != 1) logL(1, "Exception -----------------------------------------------------------------------", "Result should be 1  VS   ${res.value} ")
		else if (cancel0 < 0 && cancel1 > 0 && cancel2 > 0 && res.value != 0) logL(1, "Exception -----------------------------------------------------------------------", "Result should be 0  VS   ${res.value}")
		else if (cancel0 < 0 && cancel1 == 0 && cancel2 == 0 && res.value != 2) logL(1, "Exception -----------------------------------------------------------------------", "Result should be 2  VS   ${res.value}")
	}
	return op
}

fun suspended3(executor: Executor, cancel0: Long = -1, cancel1: Int = 0, cancel2: Int = 0, cancel3: Int = 0, cancel4: Int = 0, par0: Int = 0, par1: Int = 0, par2: Int = 0, par3: Int = 0, par4: Int = 0, iterrupt: Boolean = false): AsyncResult<*> {
	val icpr = TestInterceptor(executor);
	val icpr1 = icpr;
	val icpr2 = icpr;
	val icpr3 = icpr;
	val icpr4 = icpr
//		val icpr = TestInterceptor(executor); val icpr1 = TestInterceptor(executor); val icpr2 = TestInterceptor(executor); val icpr3 = TestInterceptor(executor); val icpr4 = TestInterceptor(executor)
	val op = executeSuspension(cancel0, if (par0 > 0) icpr else null, iterrupt) {
		logL(1, " 0 ", ">")
		val r1 = executeSuspended(if (cancel1 > 0) 200L else -1, if (par1 > 0) icpr1 else null, iterrupt) {
			logL(1, " 1 ", ">:  ? $isCancelled;  $isInterrupted")
			val sr1 = executeSuspended(if (cancel2 > 0) 200L else -1, if (par2 > 0) icpr2 else null, iterrupt) {
				logL(1, " 2 ", ">:  ? $isCancelled;  $isInterrupted")
				java.lang.Thread.sleep(400)
				logL(1, " 2 ", "X:  ? $isCancelled;  $isInterrupted")
				1
			}
			logL(1, " 1 ", ">>:  ? $isCancelled;  $isInterrupted")
			val sr2 = executeSuspended(if (cancel3 > 0) 200L else -1, if (par3 > 0) icpr3 else null, iterrupt) {
				logL(1, " 3 ", ">:  ? $isCancelled;  $isInterrupted")
				java.lang.Thread.sleep(400)
				logL(1, " 3 ", "X:  ? $isCancelled;  $isInterrupted")
				1
			}
			logL(1, " 1 ", "X:     ? $isCancelled;  $isInterrupted")
			sr1.valueOr(0) + sr2.valueOr(0)
		}
		logL(1, " 0 ", "r1= $r1")
		val r2 = executeSuspended(if (cancel4 > 0) 200L else -1, if (par4 > 0) icpr4 else null, iterrupt) {
			logL(1, " 4 ", ">:  ? $isCancelled;  $isInterrupted")
			java.lang.Thread.sleep(400)
			logL(1, " 4 ", "X:  ? $isCancelled;  $isInterrupted")
			1
		}
		logL(1, " 0 ", "r2= $r2")
		val res = r1.valueOr(0) + r2.valueOr(0)
		res
	}
	op.onComplete { res ->
		logL(1, " 0 ", "X: Result= $res")
		if (cancel2 > 0 && cancel3 > 0 && res.value != 1) logL(1, "Exception -----------------------------------------------------------------------", "Result should be 1  VS   ${res.value}")
		else if (cancel2 == 0 && cancel3 == 0 && res.value != 3) logL(1, "Exception -----------------------------------------------------------------------", "Result should be 3  VS   ${res.value}")
		else if (((cancel2 > 0 && cancel3 == 0) || (cancel2 == 0 && cancel3 > 0)) && res.value != 2) logL(1, "Exception -----------------------------------------------------------------------", "Result should be 2  VS   ${res.value}")
	}
	return op
}

fun suspended4(executor: Executor, throw0: Int = 0, throw1: Int = 0, throw2: Int = 0, par0: Int = 0, par1: Int = 0, par2: Int = 0, iterrupt: Boolean = false): AsyncResult<*> {
	val icpr = TestInterceptor(executor);
	val icpr1 = icpr;
	val icpr2 = icpr
	//		val icpr = TestInterceptor(executor); val icpr1 = TestInterceptor(executor); val icpr2 = TestInterceptor(executor)
	val op = executeSuspension(-1, if (par0 > 0) icpr else null, iterrupt) {
		logL(1, " 0 ", ">")
		if (throw0 > 0) throw Exception("0")
		val r1 = executeSuspended(-1, if (par1 > 0) icpr1 else null, iterrupt) {
			logL(1, " 1 ", ">:  ? $isCancelled;  $isInterrupted")
			if (throw1 > 0) throw Exception("1")
			val sr1 = executeSuspended(-1, if (par2 > 0) icpr2 else null, iterrupt) {
				logL(1, " 2 ", ">:  ? $isCancelled;  $isInterrupted")
				if (throw2 > 0) throw Exception("2")
				logL(1, " 2 ", "X:  ? $isCancelled;  $isInterrupted")
				1
			}
			logL(1, " 1 ", "X:     ? $isCancelled;  $isInterrupted")
			sr1.valueOr(0)
		}
		logL(1, " 0 ", "r1= $r1")
		r1.valueOr(0)
	}
	op.onComplete { res ->
		logL(1, " 0 ", "X: Result= $res")
	}
	return op
}

fun suspended5() {
	executeSuspension {
		val r0: Int = suspendCoroutine { cont ->
			executeSuspension {
				val r1 = executeSuspended {
					val r2: Int = kotlin.coroutines.experimental.suspendCoroutine { cont1: Continuation<Int> ->
						executeSuspension {
							1
						}.onComplete { cont1.resume(it.valueOr { 0 }) }
					}
					r2
				}
				cont.resume(r1.valueOr(0))
			}
		}
	}.onComplete { println("$it") }
}



class TestInterceptor(val executor: Executor): AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
	override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = object: Continuation<T> {
		override val context = continuation.context
		override fun resume(value: T) = executor.execute { continuation.resume(value) }
		override fun resumeWithException(exception: Throwable) = continuation.resumeWithException(exception)
	}
}



/* Module Request*/

suspend fun <T> executeSuspended(delay: Long = -1, context: CoroutineContext? = null, interrupt: Boolean = false, code: suspend TaskContext.() -> T): Result<T> {
	val rq = SuspendTask<T>()
	if (delay >= 0) thread { Thread.sleep(delay); rq.cancel(interrupt = interrupt) }
	return rq.preStartSuspended<SuspendTask<T>>(context) {
		if (possible) it.start(context, code)
		else it.cancel()
	}
}

/* Cancellable Request */

fun <T> executeSuspension(delay: Long = -1, context: CoroutineContext? = null, interrupt: Boolean = false, code: suspend TaskContext.() -> T): AsyncResult<T> {
	val rq = SuspendTask<T>()
	if (delay >= 0) thread { Thread.sleep(delay); rq.cancel(interrupt = interrupt) }
	rq.start(context, code)
	return rq
}
