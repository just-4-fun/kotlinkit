package testScheduler

import just4fun.kotlinkit.*
import just4fun.kotlinkit.async.AsyncTask
import just4fun.kotlinkit.testScheduler.DefaultScheduler
import just4fun.kotlinkit.testScheduler.TTask
import just4fun.kotlinkit.testScheduler.Token
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread


fun main(args: Array<String>) {
	debug = 1
	Safely.stackSizeLimit = 0
	TestScheduler.run()
}

object TestScheduler {
	var SessionID = 1
	val totalDuration = 60 * 5
	var total = AtomicInteger()
	val scheduler = Executors.newScheduledThreadPool(1)
	val okExecutor = Executors.newCachedThreadPool()
	val badExecutor = Executors.newCachedThreadPool().apply { shutdownNow() }
	val lock = Any()

	fun run() {
		loopFor(totalDuration * 1000) {
			startTime = now()
			runSession()
			if (promises.isNotEmpty()) {
				logE("EXCEPTION", "promises:  $promisesPrn")
				System.exit(4)
			}
			println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n")
		}
		//		Thread.sleep(1000)
		okExecutor.shutdownNow()?.forEach { (it  as? AsyncTask<*>)?.cancel(Exception("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Never should happen"), true) }
		scheduler.shutdown()
		scheduler.awaitTermination(1000, TimeUnit.MILLISECONDS)
		log("FINISHED", "Total= ${total.get()};   shut?  ${okExecutor.isTerminated}")
	}

	private fun runSession() {
		SessionID++
		val executor = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>()).apply { rejectedExecutionHandler = ThreadPoolExecutor.CallerRunsPolicy() }
		val sher = DefaultScheduler(executor)
		val maxSessionTime = 2000
		val minSessionTime = 100
		val sessionTime = rnd0(maxSessionTime - minSessionTime) + minSessionTime
		val sessionEnd = now() + sessionTime
		val sessionDelay = rnd0(sessionTime)
		val sessionTimeout = sessionTime - sessionDelay
		val loadEnd = now() + sessionDelay
		val taskNum = sessionTime / 10 + 1
		val taskStep = sessionTime / taskNum
		val actionMaxDelay = sessionDelay + Math.min(taskStep, sessionTimeout)
		log("ACTION SESSION", "${SessionID};   duration= $sessionTime; ")
		shutdownScheduler(sher, sessionDelay, sessionTimeout)
		//
		val actions = rnd0(sessionTime / 100 + 1)
		for (n in 0 until actions) {
			if (rndChance(2)) pauseScheduler(sher, rnd0(actionMaxDelay)) else resumeScheduler(sher, rnd0(actionMaxDelay))
		}
		//
		val loaders = rnd0(4, 4)
		for (n in 0 until loaders) {
			loadTasks(sher, sessionDelay+(sessionTime-sessionDelay)/4, taskNum)
		}
		while (!sher.isTerminated || now() < sessionEnd) Thread.sleep(100)
	}

	private fun loadTasks(sh: DefaultScheduler, delay: Int, num: Int) {
		logL(2, "LOADER", "num= $num")
		thread {
			var taskCount = num
			while (taskCount > 0) {
				val n = if (rndChance(10)) rnd1(taskCount) else 1
				taskCount -= n
				val tDealy = rnd0(delay)
				val sDelay = rnd0(delay - tDealy)
				for (i in 0 until n) scheduler.schedule({sh.schedule(newTask(sh, tDealy))}, sDelay.toLong(), TimeUnit.MILLISECONDS)
			}
		}
	}

	private fun newTask(sh: DefaultScheduler, delay: Int): TTask<*> {
		val executor = if (rndChance(2)) okExecutor else null
		val id = total.getAndIncrement()
		logL(2, "ADD $SessionID", "$id;  delay= $delay;")
		val task = TTask(id, delay, executor) { logL(2, "EXECUTE $SessionID", "$id;  over-wait= ${now() - (this as TTask<*>).runTime};") }
		val token = Token(id.toLong())
		addPromise(token)
		task.onComplete {
			logL(1, "COMPLETE $SessionID", "$id;    ${it.exception?.let { it::class.simpleName } ?: "Ok"}")
			removePromise(token, it)
		}
		return task
	}

	private fun pauseScheduler(sh: DefaultScheduler, delay: Int) {
		thread {
			Thread.sleep(delay.toLong())
			logL(2, "ACTION PAUSE $SessionID", "delay= $delay;   size= ${sh.queueSize}")
			sh.pause()
		}
	}

	private fun resumeScheduler(sh: DefaultScheduler, delay: Int) {
		thread {
			Thread.sleep(delay.toLong())
			logL(2, "ACTION RESUME $SessionID", "delay= $delay;   size= ${sh.queueSize}")
			sh.resume()
		}
	}

	private fun shutdownScheduler(sh: DefaultScheduler, delay: Int, timeout: Int) {
		thread {
			Thread.sleep(delay.toLong())
			logL(2, "ACTION SHUTDOWN $SessionID", "delay= $delay;   timeout= $timeout;   size= ${sh.queueSize}")
			sh.shutdown(timeout)
		}
	}


	val promises = mutableListOf<Token>()
	val promisesSize get() = synchronized(lock) { promises.size }
	val promisesPrn get() = synchronized(lock) { promises.joinToString() }
	fun addPromise(token: Token) = synchronized(lock) { promises.add(token) }
	fun removePromise(token: Token, res: Result<*>) = synchronized(lock) { promises.remove(token) }
}
