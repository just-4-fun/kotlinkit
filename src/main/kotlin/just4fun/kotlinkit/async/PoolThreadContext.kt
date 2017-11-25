package just4fun.kotlinkit.async

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


/** Configurable [ThreadContext] backed by [ScheduledThreadPoolExecutor]  */
open class PoolThreadContext(protected  val scheduler: ScheduledThreadPoolExecutor): ThreadContext() {
	override fun execute(command: Runnable) = scheduler.execute(command)
	
	override fun schedule(task: AsyncTask<*>) {
		task.wrapper = scheduler.schedule(task, task.delayMs.toLong(), TimeUnit.MILLISECONDS) as? Runnable
	}
	
	override fun remove(task: AsyncTask<*>) {
		if (task.isCancelled && task.wrapper != null) scheduler.remove(task.wrapper!!)
	}
	
	override fun shutdown(await: Int) {
		scheduler.shutdown()
		if (await > 0 && !scheduler.isTerminated) thread {
			scheduler.awaitTermination(await.toLong(), TimeUnit.MILLISECONDS)
		}
	}
}
