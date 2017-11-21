package just4fun.kotlinkit.testResult

import just4fun.kotlinkit.Result
import just4fun.kotlinkit.flatten
import org.jetbrains.spek.api.*



class TestProduction: Spek() { init {
	given("Result test") {
		on("Construction") {
			val res0 = Result("ok")
			val res2 = Result<String>(Exception("oops"))
			it("") {
				shouldEqual(res0.value, "ok")
				shouldBeNull(res0.exception)
				shouldBeNull(res2.value)
				shouldNotBeNull(res2.exception)
			}
		}
		on("Companion invoke") {
			val res0 = Result { val v = 10; v }
			val res1 = Result { val x = 10 / 0 }
			it("") {
				shouldEqual(res0.value, 10)
				shouldBeNull(res0.exception)
				shouldBeNull(res1.value)
				shouldEqual(res1.exception!!::class, ArithmeticException::class)
			}
		}
		on("Flatten") {
			val res0 = Result("ok")
			val res1 = Result { res0 }
			val res2 = res1.flatten()
			val res3 = Result<String>(Exception("oops"))
			val res4 = Result { res3 }
			val res5 = res4.flatten()
			it("") {
				shouldEqual(res2.value, "ok")
				shouldBeNull(res2.exception)
				shouldBeNull(res5.value)
				shouldNotBeNull(res5.exception)
			}
		}
		on("Destructuring") {
			var v0 = 0
			var v1 = 0
			var v2 = 0
			var v3 = 0
			Result { 22 }.let { (v, x) -> v0 = if (v == null) 1 else 2; v1 = if (x == null) 1 else 2 }
			Result { 22 / 0 }.let { (v, x) -> v2 = if (v == null) 1 else 2; v3 = if (x == null) 1 else 2 }
			it("") {
				shouldEqual(v0, 2)
				shouldEqual(v1, 1)
				shouldEqual(v2, 1)
				shouldEqual(v3, 2)
			}
		}
		on("Nullable type") {
			val v: String? = null
			val r = Result(v)
			it("") {
				shouldBeTrue(r.isSuccess)
				shouldBeNull(r.valueOrThrow)
				shouldBeNull(r.value)
			}
		}
		on("Throwable value") {
			val v = Exception()
			val r = Result.Success(v)
			it("") {
				shouldBeTrue(r.isSuccess)
				shouldEqual(r.valueOrThrow, v)
			}
		}
		on("Accessors") {
			val res0 = Result("ok")
			val res2 = Result<String>(Exception("oops"))
			val v00 = res0.value
			val v20 = res2.value
			val v01 = res0.valueOr("yep")
			val v21 = res2.valueOr("yep")
			val v02 = res0.valueOrThrow
			val v12 = res0.valueOrThrow
			val v03 = res0.valueOr { "yep" }
			val v23 = res2.valueOr { "yep" }
			val v05 = res0.exception
			val v25 = res2.exception
			var v09 = 0
			var v29 = 0
			res0.onSuccess { v09 = 1 }.onFailure { v09 = -1 }
			res2.onSuccess { v29 = 1 }.onFailure { v29 = -1 }
			it("") {
				shouldEqual(v00, "ok")
				shouldBeNull(v20)
				shouldEqual(v01, "ok")
				shouldEqual(v21, "yep")
				shouldEqual(v02, "ok")
				shouldEqual(v12, "ok")
				shouldThrow(Exception::class.java) { res2.valueOrThrow }
				shouldEqual(v03, "ok")
				shouldEqual(v23, "yep")
				shouldBeNull(v05)
				shouldNotBeNull(v25)
				shouldEqual(v09, 1)
				shouldEqual(v29, -1)
			}
		}
		on("Transformers") {
			var r01 = 0
			Result { 10 / 0 }
			  .onFailureOf<ArithmeticException> { r01 += 1 }
			  .onFailureOf<Exception> { r01 += 1 }
			  .onFailureOfNot<NullPointerException> { r01 += 1 }
			  .onFailureOf<NullPointerException> { r01 = -10 }
			var r02 = 0
			Result { 10 / 0 }
			  .wrapFailure { NullPointerException() }
			  .onFailureOf<NullPointerException> { r02 += 1 }
			  .onFailureOfNot<ArithmeticException> { r02 += 1 }
			  .onFailureOf<ArithmeticException> { r02 = -10 }
			var r03 = Result { 10 / 0 }.ifFailure { 10 }.valueOrThrow
			var r04 = Result(0).ifSuccess { "ok" }.ifFailure { "oops" }.valueOrThrow
			var r05 = Result { 10 / 0 }.ifSuccess { "ok" }.ifFailure { "oops" }.valueOrThrow
			
			fun Result<Int>.tfm() = fromSuccess { if (it == 0) Result(Oops()) else Result("ok") }.ifFailureOf<Oops> { "oops" }.ifFailureOfNot<Oops> { "wtf" }.valueOrThrow
			val r06 = Result(0).tfm()
			val r07 = Result(10).tfm()
			val r08 = Result { 10 / 0 }.tfm()
			it("") {
				shouldEqual(r01, 3)
				shouldEqual(r02, 2)
				shouldEqual(r03, 10)
				shouldEqual(r04, "ok")
				shouldEqual(r05, "oops")
				shouldEqual(r06, "oops")
				shouldEqual(r07, "ok")
				shouldEqual(r08, "wtf")
			}
		}
		
	}
}
}



class Oops: Exception("Oops.")
















