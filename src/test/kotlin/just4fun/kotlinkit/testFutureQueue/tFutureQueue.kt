package just4fun.kotlinkit.testFutureQueue

import just4fun.kotlinkit.async.FutureQueue
import just4fun.kotlinkit.async.AsyncTask
import just4fun.kotlinkit.loopFor
import just4fun.kotlinkit.rnd0
import just4fun.kotlinkit.rnd1
import just4fun.kotlinkit.rndChance
import org.junit.Assert


fun main(args: Array<String>) {
	TestFutureQ.testOrder(300)
	TestFutureQ.testRemove(300)
	TestFutureQ.testRemoveAfter(300)
}


object TestFutureQ {
	
	fun testOrder(duration:Int) {
		var count = 0
		loopFor(duration * 1000) {
			count++
			var IDs = 0
			val size = rnd0(1000)
			val inArray = Array(size) { Task(IDs++, rnd0(100)) }
			val que = FutureQueue()
			for (n in 0 until size) que.add(inArray[n])
			val outArray = arrayOfNulls<Task>(size)
			for (n in 0 until size) outArray[n] = que.remove() as Task
			val chkArray = inArray.sortedArray()
			//			println(outArray.map { "${it!!.runTime}:${it.seqId}" }.joinToString("  "))
			//			println(chkArray.map { "${it.runTime}:${it.seqId}" }.joinToString("  "))
			Assert.assertArrayEquals(outArray, chkArray)
		}
		println("${count}")
	}
	
	fun testRemove(duration:Int) {
		var count = 0
		var IDs = 0
		loopFor(duration * 1000) {
			count++
			val opsSize = rnd1(100)
			val list = mutableListOf<Task>()
			val que = FutureQueue()
			for (n in 0..opsSize) {
				if (rndChance(2)) {
					val task = Task(IDs++, rnd0(100))
					list += task
					que.add(task)
				} else if (list.size > 0) {
					val task = list.removeAt(rnd0(list.size - 1))
					que.remove(task)
				}
			}
			val a1 = list.toTypedArray().sortedArray()
			list.clear()
			for (n in 0 until que.size()) list += que.remove() as Task
			val a2 = list.toTypedArray()
			Assert.assertArrayEquals(a1, a2)
//			println(a1.map { "${it!!.runTime}:${it.seqId}" }.joinToString("  "))
//			println(a2.map { "${it.runTime}:${it.seqId}" }.joinToString("  "))
		}
		println("${count}")
	}
	
	fun testRemoveAfter(duration:Int) {
		var count = 0
		var IDs = 0
		loopFor(duration * 1000) {
			count++
			val size = rnd0(100)
			val inArray = Array(size) { Task(IDs++, rnd0(100)) }
			val que = FutureQueue()
			for (n in 0 until size) que.add(inArray[n])
			val deadline = rnd0(100)
			val a1 = inArray.filter { it.runTime < deadline }.toTypedArray().sortedArray()
			que.removeAfter(deadline.toLong()) {}
			val list = mutableListOf<Task>()
			for (n in 0 until que.size()) list += que.remove() as Task
			val a2 = list.toTypedArray()
			Assert.assertArrayEquals(a1, a2)
//			println(a1.map { "${it!!.runTime}:${it.seqId}" }.joinToString("  "))
//			println(a2.map { "${it.runTime}:${it.seqId}" }.joinToString("  "))
		}
		println("${count}")
	}
}


// TODO changed time to delay FutureTask
class Task(val id: Int, time: Int): AsyncTask<Int>(time, null, { 0 }) {
	override fun hashCode(): Int = id
	override fun toString() = id.toString()
}
