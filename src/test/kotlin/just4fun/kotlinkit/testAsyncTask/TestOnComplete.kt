package just4fun.kotlinkit.testAsyncTask

import just4fun.kotlinkit.async.AsyncTask
import just4fun.kotlinkit.async.DefaultExecutionContext
import just4fun.kotlinkit.log



fun main(args: Array<String>) {
	//	test1()
	//	test1_1()
//		test1_2()
	//	test2()
	test2_0()
	//	test2_1()
	//	test2_2()
	//	test2_3()
	//	test3()
	//	test3_1()
//	test3_2()
	//	test3_3()
	Thread.sleep(1000)
	default.shutdown(1000)
	executor.shutdown(1000)
}

val executor = DefaultExecutionContext()
val default = AsyncTask.sharedContext

fun test1() {
	AsyncTask { log("task body", "none") }
	  .onComplete() { log("reaction body", "none") }
}

fun test1_1() {
	AsyncTask { log("task body", "none") }
	  .onComplete(executor) { log("reaction body", "executor") }
}

fun test1_2() {
	AsyncTask { executor.shutdown(); log("task body", "gonna boom") }
	  .onComplete(executor) { log("reaction body", "executor") }
}

fun test2_0() {
	AsyncTask { log("task body", "body") }
	  .onComplete { throw Exception("reaction 1 is boom") }
	  .onComplete { log("reaction 2", "executor") }
}

fun test2() {
	AsyncTask { log("task body", "none") }
	  .onComplete { log("reaction 1", "none") }
	  .onComplete { log("reaction 2", "none") }
}

fun test2_1() {
	AsyncTask { log("task body", "none") }
	  .onComplete { log("reaction 1", "none") }
	  .onComplete(precede = true) { log("reaction 2", "none") }
}

fun test2_2() {
	AsyncTask { log("task body", "none") }
	  .onComplete(executor) { log("reaction 1", "executor") }
	  .onComplete { log("reaction 2", "none") }
}

fun test2_3() {
	AsyncTask { log("task body", "none") }
	  .onComplete(executor) { log("reaction 1", "executor") }
	  .onComplete(default) { log("reaction 2", "default") }
}

fun test3() {
	AsyncTask { log("task body", "none") }
	  .onComplete { log("reaction 1", "none") }
	  .onComplete { log("reaction 2", "none") }
	  .onComplete { log("reaction 3", "none") }
}

fun test3_1() {
	AsyncTask { log("task body", "none") }
	  .onComplete { log("reaction 1", "none") }
	  .onComplete(precede = true) { log("reaction 2", "none") }
	  .onComplete(precede = true) { log("reaction 3", "none") }
}

fun test3_2() {
	AsyncTask { log("task body", "none") }
	  .onComplete(executor) { log("reaction 1", "executor") }
	  .onComplete(default) { log("reaction 2", "default") }
	  .onComplete(executor) { log("reaction 3", "executor") }
}
