package just4fun.kotlinkit.testResult

import just4fun.kotlinkit.*



fun main(a: Array<String>) {
	t1;t1;t1;t1;t1;t1;t1;t1;t1;t1;
//	t2;t2;t2;t2;t2;t2;t2;t2;t2;t2;
	println("avg= $measuredTimeAvg ns")
	
//	fun run(v: String?) = v
//	val res = Result.Value(run(null))
//	println("${res.value!!}")
}



val t1 get() = measureTime("Res", 1) {
	val v : Result<Int> = Result(1)
//	v.valueOrThrow
	v.valueOr { 11 }
}

