package just4fun.kotlinkit.testResult

import just4fun.kotlinkit.*



fun main(a: Array<String>) {
	t1;t1;t1;t1;t1;t1;t1;t1;t1;t1;
//	t2;t2;t2;t2;t2;t2;t2;t2;t2;t2;
	measuredStats()
	fun test1(v: String?) = v
	fun test2(v: String) = v
	val res1 = Result(test1(null))
	res1.valueOr(null)?.length
	val res2 = Result { test2("") }
	res2.valueOr("").length
	
	val res = Result{ 10 / 0}
	val tres = res.transformFailure { Exception("oops") }
	println("${tres.isSuccess}")
}



val t1
	get() = measureTime("Res", 1) {
		val v: Result<Int> = Result(1)
	v.valueOrThrow
//		v.valueOr { 11 }
	}

