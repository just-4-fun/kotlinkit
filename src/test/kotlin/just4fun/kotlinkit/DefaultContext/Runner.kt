package just4fun.kotlinkit.DefaultContext

import just4fun.kotlinkit.async.AsyncTask
import just4fun.kotlinkit.async.DefaultThreadContext
import just4fun.kotlinkit.log



fun main(a: Array<String>) {
	testDefault()
//	testNew()
}

fun testNew() {
	val scheduler = DefaultThreadContext(5000)
	AsyncTask(1000, scheduler) { log(0, "task 1") }
	Thread.sleep(2000)
	AsyncTask(5000, scheduler) { log(0, "task 2")}
	Thread.sleep(11000)
	AsyncTask(1000, scheduler) { log(0, "task 3")}
}

fun testDefault() {
	AsyncTask(1000) { log(0, "task 1") }
	Thread.sleep(3000)
	AsyncTask(1000) { log(0, "task 2") }
}