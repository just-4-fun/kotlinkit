package just4fun.kotlinkit.testResult

import just4fun.kotlinkit.Result



fun main(a: Array<String>) {
	// construction
	
	//	from execution of code block
	val result1 = Result { 10 / 10 }
	val result2 = Result { 10 / 0 }
	
	// from constructor
	
	fun devide(a: Int, b: Int): Result<Int> = if (b != 0) Result(a / b) else Result(ArithmeticException())
	val result31 = devide(10, 0) // failure with exception as ArithmeticException
	val result32 = devide(10, 10) // Result(1)
	
	// from companion's functions
	val result4: Result<String> = Result.Success("ok") // value = "ok"
	val result7: Result<Int> = Result.Failure(ArithmeticException())  // value is null; exception is ArithmeticException
	val result5: Result<Exception> = Result.Success(Exception())  // value is Exception
	
	// destructuring
	val (value, exception) = Result("ok") // value = "ok"; exception = null
	Result { 10 / 0 }.let { (value, exception) -> println("v=$value;  e=$exception") } // v=null; e=ArithmeticException
	
	// usage
	
	val okay = Result { 10 / 10 }
	val oops = Result { 10 / 0 }
	
	val s0 = okay.value ?: 0 + 1 // = 2
	val f0 = oops.value ?: 0 + 1 // = 1
	val s1 = okay.valueOrThrow + 1 // = 2
//	val f1 = oops.valueOrThrow + 1 // throws ArithmeticException
	val s2 = okay.value!! + 1 // = 2
//	val f2 = okay.value!! + 1 // throws NullPointerException
	val s3 = okay.valueOr(0) + 1 // = 2
	val f3 = oops.valueOr(0) + 1 // = 1
	val s4 = okay.valueOr { if (it is ArithmeticException) 0 else 1 } + 1 // = 2
	val f4 = oops.valueOr { if (it is ArithmeticException) 0 else 1 } + 1 // = 1
	val e0 = okay.exception // is null
	val e1 = oops.exception // is ArithmeticException
	val b0 = if (okay.isSuccess) 1 else 0 // = 1
	val b1 = if (oops.isFailure) 1 else 0 // = 1
	okay.onSuccess { println("success") }.onFailure { println("failure 1") } // prints "success"
	oops.onSuccess { println("success") }.onFailure { println("failure 2") } // prints "failure 2"
	oops.onFailureOf<ArithmeticException> { println("failure 3") } // prints "failure 3"
	oops.onFailureOfNot<ArithmeticException> { println("failure 4") } // prints nothing
	oops.onFailureOf<Exception> { println("failure 5") } // prints "failure 5"
	oops.onFailureOfNot<NullPointerException> { println("failure 6") } // prints "failure 6"
	val t0 = okay.ifFailure { 0 }// Result(2)
	val t1 = oops.ifFailure { 0 } // Result(0)
	val t11 = oops.ifFailureOf<ArithmeticException> { 0 } // Result(0)
	val t12 = oops.ifFailureOfNot<NullPointerException> { 0 } // Result(0)
	val t2 = okay.ifSuccess { "ok" }.ifFailure { "oops" } // Result("ok")
	val t3 = oops.ifSuccess { "ok" }.ifFailure { "oops" } // Result("oops")
	val w0 = oops.wrapFailure { IllegalStateException() } // exception is IllegalStateException with cause: ArithmeticException
	val t4 = okay.fromSuccess {
		if (it == 0) Result("ok") else Result(Exception("oops"))
	}// failure
	val t5 = okay.fromSuccess {
		if (it == 1) Result("ok") else Result(Exception("oops"))
	}//  Result("ok")
	val t6 = oops.fromSuccess { Result("ok") }.fromFailure {
		if (it is ArithmeticException) Result("wrong") else Result(Exception("oops"))
	}// Result("wrong")
	val t7 = oops.fromSuccess { Result("ok") }.fromFailure {
		if (it is ArithmeticException) Result(Exception("oops")) else Result("wrong")
	}// failure
	
	assert(s0 == 2); assert(s1 == 2); assert(s2 == 2); assert(s3 == 2); assert(s4 == 2);
	assert(f0 == 1); assert(f3 == 1); assert(f4 == 1);
	assert(e0 == null); assert(e1 is ArithmeticException)
	assert(b0 == 1);assert(b1 == 1);
	assert(t0 == Result(2));assert(t1 == Result(0));
	assert(t2 == Result("ok"));assert(t3 == Result("oops"));
	assert(w0.exception!!.cause is ArithmeticException)
	assert(t4.exception is Exception)
	assert(t5 == Result("ok"))
	assert(t6 == Result("wrong"))
	assert(t7.exception is Exception)
}