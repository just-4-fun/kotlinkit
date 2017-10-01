package just4fun.kotlinkit.async

import just4fun.kotlinkit.async.ExecutionContextState.*
import just4fun.kotlinkit.Safely
import java.util.*
import java.util.concurrent.RejectedExecutionException

internal enum class ExecutionContextState {RESUMED, PAUSED, SHUTDOWN, TERMINATED; }


open class DefaultExecutionContext: ExecutionContext() {
	private var state: ExecutionContextState = RESUMED
	private val tasks = FutureQueue()
	private val lock = tasks
	private var thread = kotlin.concurrent.thread { loop() }
	
	override fun execute(command: Runnable) {
		if (state >= SHUTDOWN) throw RejectedExecutionException("Executor has been shutdown")
		AsyncTask(0, this) { command.run() }
	}
	
	override fun onSchedule(task: AsyncTask<*>) = synchronized(lock) {
		if (state != RESUMED) {
			task.cancel(RejectedExecutionException("Executor has been shutdown"))
			return
		}
		tasks.add(task)
		if (tasks.head() === task) (lock as java.lang.Object).notify()
	}
	
	override fun onRemove(task: AsyncTask<*>) = synchronized(lock) {
		if (state >= SHUTDOWN || !task.isCancelled) return
		val head = tasks.head()
		tasks.remove(task)
		if (head !== tasks.head()) (lock as java.lang.Object).notify()
	}
	
	override fun resume() = synchronized(lock) {
		if (state != PAUSED) return
		state = RESUMED
		thread = Thread(Runnable { loop() })
		(lock as java.lang.Object).notify()
		thread.start()
	}
	
	override fun pause() = synchronized(lock) {
		if (state == RESUMED) state = PAUSED
	}
	
	/**Cancels any task which [AsyncTask.runTime] >= [await] and shuts down. [await] = 0 removes all tasks. Can be called more than once to cancell more tasks.*/
	override fun shutdown(await: Int) {
		val list = synchronized(lock) {
			if (state == TERMINATED) return
			else {
				state = SHUTDOWN
				val deadline = System.currentTimeMillis() + await
				val list = mutableListOf<AsyncTask<*>>()
				tasks.removeAfter(deadline) { list += it }
				(lock as java.lang.Object).notify()
				list
			}
		}
		val cause = RejectedExecutionException("Scheduler has been shutdown.")
		list.forEach { it.cancel(cause, true) }
		synchronized(lock) { if (tasks.isEmpty()) onShutdown() }
	}
	
	private fun loop() {
		fun suspend(delay: Long) {
			//			Thread.interrupted()// make no much sence as returns back soon
			Safely { (lock as Object).wait(delay) }
		}
		//
		while (Thread.currentThread() == thread && (state == RESUMED || tasks.nonEmpty())) {
			var task: AsyncTask<*>? = null
			synchronized(lock) {
				val now = System.currentTimeMillis()
				val head = tasks.head()
				when {
					head == null -> if (state == RESUMED) suspend(0)
					head.runTime > now -> suspend(head.runTime - now)
					else -> task = tasks.remove()
				}
			}
			task?.run() // !!! should be outside sync block;  safe enough  to call w.o. try
		}
		synchronized(lock) { if (state == SHUTDOWN && Thread.currentThread() == thread) onShutdown() }
		//		println("Exited loop by ${Thread.currentThread().name}:  ${if (Thread.currentThread() != thread) "Dumped" else if (isTerminated) "Terminated" else "Stopped"}")// TODO just for test
	}
	
	private fun onShutdown(): Unit = synchronized(lock) {
		if (state == TERMINATED) return
		state = TERMINATED
		//		if (tasks.nonEmpty()) System.err.println("Exception TASKS NON EMPTY:  ${tasks.size}")// TODO just for test
		if (tasks.nonEmpty()) tasks.removeAfter(0) { it.cancel(interrupt = true) }// shouldn't happen
	}
	
}







/* QUEUE */

open class FutureQueue {
	private var queue: Array<AsyncTask<*>?> = arrayOfNulls(16)
	private var size = 0
	
	fun size() = size
	fun isEmpty() = size == 0
	fun nonEmpty() = size > 0
	fun head() = queue[0]
	
	fun <T> add(task: AsyncTask<T>): AsyncTask<T> {
		if (++size > queue.size) grow()
		if (size > 1) siftUp(task, size - 1)
		else {
			queue[0] = task
			task.index = 0
		}
		return task
	}
	
	fun remove(): AsyncTask<*>? {
		val head = queue[0]
		if (head != null) {
			head.index = -1
			val lastIx = --size
			val lastElt = queue[lastIx]!!
			queue[lastIx] = null
			if (lastIx != 0) siftDown(lastElt, 0)
		}
		return head
	}
	
	fun remove(task: AsyncTask<*>): Boolean {
		val index = task.index
		if (index < 0) return false
		task.index = -1
		val lastIx = --size
		val lastElt = queue[lastIx]!!
		queue[lastIx] = null
		if (lastIx != index) {
			siftDown(lastElt, index)
			if (queue[index] === lastElt) siftUp(lastElt, index)
		}
		return true
	}
	
	fun removeAfter(deadline: Long, apply: (AsyncTask<*>) -> Unit) {
		val q = queue
		val s = size
		queue = arrayOfNulls(16)
		size = 0
		for (n in 0 until s) q[n]!!.let {
			if (it.runTime < deadline) add(it)
			else {
				it.index = -1
				apply(it)
			}
		}
	}
	
	fun toArray(): Array<AsyncTask<*>> {
		return Arrays.copyOf(queue, size) as Array<AsyncTask<*>>
	}
	
	private fun siftUp(elt: AsyncTask<*>, lastIndex: Int) {
		var index = lastIndex
		while (index > 0) {
			val parentIx = (index - 1).ushr(1)
			val parentElt = queue[parentIx]!!
			if (elt >= parentElt) break
			queue[index] = parentElt
			parentElt.index = index
			//			println("˄ [$elt]::    [$index]= $parentElt")
			index = parentIx
		}
		queue[index] = elt
		elt.index = index
		//		println("˄ [$elt]::    [$index]= $elt")
	}
	
	private fun siftDown(elt: AsyncTask<*>, startIx: Int) {
		var index = startIx
		val halfIx = size.ushr(1)
		while (index < halfIx) {
			var childIx = index.shl(1) + 1
			var childElt = queue[childIx]!!
			val rightIx = childIx + 1
			if (rightIx < size && childElt > queue[rightIx]!!) run { childIx = rightIx; childElt = queue[childIx]!! }
			if (elt <= childElt) break
			//			println("˅ [$elt]::    [$index]= $childElt")
			queue[index] = childElt
			childElt.index = index
			index = childIx
		}
		queue[index] = elt
		elt.index = index
		//		println("˅ [$elt]::    [$index]= $elt")
	}
	
	// TODO shrink
	private fun grow() {
		val oldSize = queue.size
		var newSize = oldSize + oldSize.shr(1) // grow 50%
		if (newSize < 0) newSize = Int.MAX_VALUE // overflow
		queue = Arrays.copyOf(queue, newSize)
	}
	
}