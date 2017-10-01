package just4fun.kotlinkit.async

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread



open class PoolExecutionContext(protected  val scheduler: ScheduledThreadPoolExecutor): ExecutionContext() {
	private var info: ExecutorInfo? = null
	override fun execute(command: Runnable) = scheduler.execute(command)
	
	override fun onSchedule(task: AsyncTask<*>) {
		task.wrapper = scheduler.schedule(task, task.delayMs.toLong(), TimeUnit.MILLISECONDS) as? Runnable
	}
	
	override fun onRemove(task: AsyncTask<*>) {
		if (task.isCancelled && task.wrapper != null) scheduler.remove(task.wrapper!!)
	}
	
	override fun resume() {
		info = info?.resumeExecutor(scheduler)
	}
	
	override fun pause() {
		info = ExecutorInfo().pauseExecutor(scheduler)
	}
	
	override fun shutdown(await: Int) {
		scheduler.shutdown()
		if (await > 0 && !scheduler.isTerminated) thread {
			scheduler.awaitTermination(await.toLong(), TimeUnit.MILLISECONDS)
		}
	}
}




/** Non-synchronized */
class ExecutorInfo {
	var poolSize: Int = 0
	var allowsTimeOut: Boolean = true
	var timeOut: Long = 1
	private var paused = false
	
	internal fun resumeExecutor(e: ThreadPoolExecutor): ExecutorInfo? {
		if (!paused) return null
		paused = false
		e.corePoolSize = poolSize
		e.setKeepAliveTime(1, TimeUnit.MILLISECONDS)
		e.allowCoreThreadTimeOut(allowsTimeOut)
		e.setKeepAliveTime(timeOut, TimeUnit.MILLISECONDS)
		e.rejectedExecutionHandler = ThreadPoolExecutor.CallerRunsPolicy()
		return null
	}
	
	internal fun pauseExecutor(e: ThreadPoolExecutor): ExecutorInfo {
		if (paused) return this
		paused = true
		poolSize = e.corePoolSize
		allowsTimeOut = e.allowsCoreThreadTimeOut()
		timeOut = e.getKeepAliveTime(TimeUnit.MILLISECONDS)
		e.corePoolSize = 0
		e.setKeepAliveTime(1, TimeUnit.MILLISECONDS)
		e.allowCoreThreadTimeOut(true)
		return this
	}
}
