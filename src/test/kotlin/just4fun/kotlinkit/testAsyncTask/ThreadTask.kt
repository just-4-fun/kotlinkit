package just4fun.kotlinkit.testAsyncTask

import just4fun.kotlinkit.async.ThreadTask
import just4fun.kotlinkit.testResult.Something



fun main(a: Array<String>) {
	testInstantReaction()
	Thread.sleep(500)
	println()
	testDelayedReaction()
}

fun testInstantReaction() {
	val res = ThreadTask {
		Thread.sleep(100)
		println("Calculating...")
		10
	}
	res.onComplete { println("$it"); if (it.value != 10) throw Exception("Oops.. must be 10") }
	println("Registered reaction")
}

fun testDelayedReaction() {
	val res = ThreadTask {
		println("Calculating...")
		10
	}
	Thread.sleep(100)
	res.onComplete { println("$it"); if (it.value != 10) throw Exception("Oops.. must be 10") }
	println("Registered reaction")
}