package just4fun.kotlinkit.testResult

import just4fun.kotlinkit.Result
import just4fun.kotlinkit.flatten
import just4fun.kotlinkit.measureTime


fun main(args: Array<String>) {
//	f1;f1;f1;f1;f1;f1;f1;f1;f1;f1;f1;f1;f1;f1
//	println("measureMiddleTime: $measureMiddleTime ns")
	
	val resIn = Result {
		if (false) 42 else throw Exception("oops")
	}
	val resOut = Result { resIn }
	println("${resOut.flatten()}")
}

val N = 1
val res = Result { 1 + 1 / 1 }
val f1 get() = measureTime("Instance", N) {
	//	val res = Result.Success(42) // + 400 ns
	val res = 42
	res
}

//import java.util.concurrent.CancellationException
//import just4fun.core.modules.utils.Result.Failure
//import just4fun.core.modules.utils.Result.Success
//
//fun main(args: Array<String>) {
//	val fal = testSuccess(4)
//	println("${fal is Failure}")
//	println("${fal is Success}")
//	println("${fal.valueOr(5)}")
//	println("${fal.valueOrNull()}")
//	val fal2 = testSuccess<Any?>(12)
//	println("${fal2 is Failure}")
//	println("${fal2 is Success}")
//	println("${fal2.valueOr(5)}")
//	println("${fal2.valueOrNull()}")
//	val fal3 = testSuccess<Any?>(null)
//	println("${fal3 is Failure}")
//	println("${fal3 is Success}")
//	println("${fal3.valueOr(5)}")
//	println("${fal3.valueOrNull()}")
//	val fal4 = testFailure(4)
//	println("${fal4 is Failure}")
//	println("${fal4 is Success}")
//	println("${fal4.valueOr(5)}")
//	println("${fal4.valueOrNull()}")
//
//	val vf1 = testSuccess("ok")
//	val s10: String? = vf1.valueOrNull()
//	val vf2 = testSuccess("ok")
//	val s2: String = vf2.valueOrThrow()
//	val s20: String = vf2.valueOr("oops")
//	val v21: String = when (vf2) {
//		is Result.Success -> vf2.value
//		is Result.Failure -> vf2.exception.toString()
//	}
//	val v22 = vf2.valueOr { x ->
//		when (x) {
//			is CancellationException -> "Oops"
//			else -> x.toString()
//		}
//	}
//	testSuccess(Unit).valueOr { x ->
//		// auto return Unit
//	}
//	val res = Result {
//		val vf0 = testFailure(4)
//		val s1: Int = vf0.valueOrThrow()
//	}
//	println("Result= $res")
//
//	testSuccess(11)
//	  .ifSuccess { println("Success: $it") }
//	  .ifFailure { println("Failure: $it") }
//	testFailure(11)
//	  .ifSuccess { println("Success: $it") }
//	  .ifFailure { println("Failure: $it") }
//
//	val (v, x) = testSuccess("ok")
//	println("Value= $v;  Exception= $x")
//	val (v1, x1) = testFailure("ok")
//	println("Value= $v1;  Exception= $x1")
//
//	val s5: String = testSuccess(21).let { (v, x) ->
//		x?.toString() ?: v!!.toString()
//	}
//
//	val r1: Result<Result<Int>> = Result{
//		Success(4)
//	}
//	println("flatten: ${r1.flatten()}")
//}
//
//fun <T> testSuccess(v: T): Result<T> {
//	return Result.Success(v)
//}
//
//fun <T> testFailure(v: T): Result<T> {
//	return Result.Failure(Exception("oops"))
//}
//	try { Result{throw Exception("oops")}.exceptionAs { Exception("wrapper") }} catch (x: Throwable) {x.printStackTrace()}
