package just4fun.kotlinkit.testResult

import just4fun.kotlinkit.*



fun main(a: Array<String>) {
	t1;t1;t1;t1;t1;t1;t1;t1;t1;t1;
//	t2;t2;t2;t2;t2;t2;t2;t2;t2;t2;
	measuredStats()
//	fun test1(v: String?) = v
//	fun test2(v: String) = v
//	val res1 = Result(test1(null))
//	res1.valueOr(null)?.length
//	val res2 = Result { test2("") }
//	res2.valueOr("").length
//	val (v, x) = Result{1/0}
}



val N = 1
val vals = arrayOfNulls<Any>(10)

val t1
	get() = measureTime("Res", N) {
		val v: Result<Int> = Result(1)
		vals[0] = v.value
		vals[1] = v.valueOrThrow
		vals[2] = v.valueOr (10)
		vals[3] = v.valueOr { 11 }
		vals[4] = v.isSuccess
		vals[5] = v.isFailure
		vals[6] = v.exception
		vals[7] = v.mapSuccess { 2 }
		vals[8] = v.mapFailure { 3 }
	}

