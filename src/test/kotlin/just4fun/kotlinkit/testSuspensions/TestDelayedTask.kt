package just4fun.kotlinkit.testSuspensions

import just4fun.kotlinkit.Result
import just4fun.kotlinkit.async.AsyncResult
import just4fun.kotlinkit.async.DefaultThreadContext
import just4fun.kotlinkit.async.SuspendTask
import just4fun.kotlinkit.async.TaskContext
import just4fun.kotlinkit.log
import kotlin.concurrent.thread
import kotlin.coroutines.experimental.CoroutineContext


fun main(a: Array<String>) {
	test()
}

fun test() {
	var letters = ""
	val executor = DefaultThreadContext(4000)
	val rq = Request(executor) {
		letters += "Y"
		1
	}.onComplete {
		log("R","Request Complete"); letters += "2"
	} as Request<*>
	val op = executeSuspension(rq, -1, 1000, executor) {
		log("T", "step 1"); letters += "1"
		rq.preStart()
		log("T", "step 2   cancelled? $isCancelled"); if (isCancelled)  letters += "3"
		val res = executeSuspended(-1, executor) {  letters += "X" }// shouldn't be executed if root is cancelled
		log("T", "step 3   subtask failed? ${res.isFailure}"); if (res.isFailure) letters += "4"
		1
	}
	op.onComplete {
		log("T","Complete"); letters += "5"
		if(letters != "12345") throw Exception("Oops.. $letters  should be 12345")
	}
}

class Request<T>(override var context: CoroutineContext, private val code: suspend TaskContext.() -> T): SuspendTask<T>() {
	fun start() = start(context, code)
	suspend fun preStart() = preStartSuspended(context)
}

fun <T> executeSuspension(request: Request<*>, requestDelay: Long = -1, cancelDelay: Long = -1, context: CoroutineContext? = null, interrupt: Boolean = false, code: suspend TaskContext.() -> T): AsyncResult<T> {
	val rq = SuspendTask<T>()
	if (requestDelay >= 0) thread { Thread.sleep(requestDelay); request.start() }
	if (cancelDelay >= 0) thread { Thread.sleep(cancelDelay); rq.cancel(interrupt = interrupt) }
	rq.start(context, code)
	return rq
}

suspend fun <T> executeSuspended(delay: Long = -1, context: CoroutineContext? = null, interrupt: Boolean = false, code: suspend TaskContext.() -> T): Result<T> {
	val rq = SuspendTask<T>()
	if (delay >= 0) thread { Thread.sleep(delay); rq.cancel(interrupt = interrupt) }
	return rq.preStartSuspended<SuspendTask<T>>(context) { it.start(context, code) }
}

