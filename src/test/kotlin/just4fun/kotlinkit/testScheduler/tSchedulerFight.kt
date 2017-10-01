package testScheduler

import just4fun.kotlinkit.async.AsyncTask
import just4fun.kotlinkit.now
import just4fun.kotlinkit.N
import just4fun.kotlinkit.logL
import just4fun.kotlinkit.testScheduler.DefaultScheduler
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit



fun main(args: Array<String>) {
	N = 100
	testClassic()
//		testFuture()
}


fun testClassic() {
	val scheduler = Executors.newScheduledThreadPool(1)
	val executor = Executors.newSingleThreadExecutor()
	var Total = N
	val t0 = now()
	for (n in 0..N) {
		val task = AsyncTask(1, executor, { n }).onComplete {
			if (--Total == 0) {
				logL(1, "FINISHED", "Time: ${now() - t0}")
				scheduler.shutdown()
				executor.shutdown()
			}
		}
		scheduler.schedule(task, 1, TimeUnit.MILLISECONDS)
	}
}

fun testFuture() {
	val pool = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>())
	val scheduler = DefaultScheduler(pool)
	val executor = Executors.newSingleThreadExecutor()
	var Total = N
	val t0 = now()
	for (n in 0..N) {
		val task = AsyncTask(1, executor, { n }).onComplete {
			if (--Total == 0) {
				logL(1, "FINISHED", "Time: ${now() - t0}")
				scheduler.shutdown()
				executor.shutdown()
			}
		}
		scheduler.schedule(task)
	}
}
