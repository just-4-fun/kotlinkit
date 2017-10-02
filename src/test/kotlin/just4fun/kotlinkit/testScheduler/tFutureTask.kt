package just4fun.kotlinkit.testScheduler

import just4fun.kotlinkit.*
import just4fun.kotlinkit.Result
import just4fun.kotlinkit.Safely
import just4fun.kotlinkit.async.AsyncTask
import just4fun.kotlinkit.async.TaskContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import java.util.concurrent.TimeUnit.MILLISECONDS as ms

fun main(args: Array<String>) {
	debug = 1
	Safely.stackSizeLimit = 0
	TestTask
//println("${TTask(1, 1, null){1} < TTask(0, 0, null){0}}")
//println("${TTask(1, 1, null){1} < TTask(2, 2, null){0}}")
//println("${TTask(3, 3, null){1} <= TTask(3, 3, null){1}}")
//println("${TTask(3, 3, null){1} < TTask(3, 3, null){1}}")
//println("${TTask(3, 3, null){1} >= TTask(3, 3, null){1}}")
//println("${TTask(3, 3, null){1} > TTask(3, 3, null){1}}")
}


object TestTask {
	val duration = 30
	val lock = Any()
	@Volatile var task: TTask<*>? = null
	var total = 0L
	val scheduler = Executors.newScheduledThreadPool(1)!!
	val okExecutor = Executors.newCachedThreadPool()!!
	val badExecutor = Executors.newCachedThreadPool().apply { shutdownNow() }!!
	var finish = false
	val caughtCancels = AtomicInteger()
	
	/**/
	
	val main = thread {
		loopFor(duration * 1000) {
			val executor = when (rnd0(2)) {
				0 -> okExecutor
				1 -> badExecutor
				else -> null
			}
			val id = total++
			logL(2, "ADD", "$id;")
			task = TTask(id.toInt(), id.toInt(), executor) {
				logL(2, "EXECUTE", "$id;    ${if (isCancelled) "cancelled  ${caughtCancels.incrementAndGet()}" else ""}")
				if (rndChance(5)) throw ExecutionException() else if (rndChance(2)) Safely { Thread.sleep(100) }
				1
			}
			Thread.sleep(5)
			if (!task!!.isComplete) task!!.run()
		}
		task = null
		okExecutor.shutdown()
		scheduler.shutdown()
		finish = true
		logL(2, "MAIN", "Total= ${total};  Promises= ${promises.joinToString()}")
	}
	
	/**/
	fun runTask(task: TTask<*>) {
		logL(1, "RUN....", "${task.runTime}")
		task.run()
	}
	
	val runner = thread {
		loopWhile({ !finish }) {
			task?.run { runTask(this) }
			Thread.sleep(1)
		}
	}
	
	/**/
	fun cancelTask(task: TTask<*>) {
		logL(1, "CANCEL", "${task.runTime}")
		task.cancel(interrupt = true)
	}
	
	val canceller = thread {
		loopWhile({ !finish }) {
			task?.run { cancelTask(this) }
			Thread.sleep(1)
		}
	}
	
	/**/
	val promises = mutableListOf<Token>()
	val promisesSize get() = synchronized(lock) { promises.size }
	val promisesPrn get() = synchronized(lock) { promises.joinToString() }
	fun addPromise(token: Token) = synchronized(lock) { promises.add(token);logL(1, "CALL +", "$token;   proms= ${promisesPrn}") }
	fun removePromise(token: Token, res: Result<*>) = synchronized(lock) {
		promises.remove(token)
		val msg = res.failure?.let { it::class.simpleName } ?: "Ok"
		logL(1, "CALL --", "$token;   $msg;   proms= ${promisesPrn}")
	}
	
	fun callbackTask(task: TTask<*>) {
		val token = Token(task.runTime)
		addPromise(token)
		Safely {
			task.onComplete {
				removePromise(token, it)
				if (rndChance(5)) throw CompleteException() else Unit
			}
		}
	}
	
	val caller1 = thread {
		loopWhile({ !finish }) {
			task?.run { callbackTask(this) }
			Thread.sleep(1)
		}
	}
	
	val caller2 = thread {
		loopWhile({ !finish }) {
			task?.run { callbackTask(this) }
			Thread.sleep(1)
		}
	}
}


//class tFutureTask {
//	@Test
//	fun test() {
//
//	}
//}

class Token(val v: Long) {
	override fun toString() = "$v"
	override fun equals(other: Any?): Boolean = this === other
}

// TODO changed time to delay FutureTask
class TTask<T>(val id: Int, delay: Int = 0, executor: Executor?, code: TaskContext.() -> T): AsyncTask<T>(delay, executor, code) {
	override fun hashCode(): Int = id
}

class CompleteException: Exception()
class ExecutionException: Exception()