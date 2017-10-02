package just4fun.kotlinkit.testResult

import just4fun.kotlinkit.Result
import just4fun.kotlinkit.flatten
import org.jetbrains.spek.api.*



class TestProduction: Spek() { init {
	given("Result test") {
		on("") {
			val res0 = Result("ok")
			val res1 = Result("ok", Exception("oops"))
			val res2 = Result<String>(Exception("oops"))
			it("Construction") {
				shouldEqual(res0.value, "ok")
				shouldBeNull(res0.failure)
				shouldEqual(res1.value, "ok")
				shouldNotBeNull(res1.failure)
				shouldBeNull(res2.value)
				shouldNotBeNull(res2.failure)
			}
		}
		on("Companion invoke") {
			val res0 = Result { val v = 10; v }
			val res1 = Result { val x = 10 / 0 }
			it("") {
				shouldEqual(res0.value, 10)
				shouldBeNull(res0.failure)
				shouldBeNull(res1.value)
				shouldEqual(res1.failure!!::class, ArithmeticException::class)
			}
		}
		on("Flatten") {
			val res0 = Result("ok")
			val res1 = Result { res0 }
			val res2 = res1.flatten()
			it("") {
				shouldEqual(res2.value, "ok")
				shouldBeNull(res2.failure)
			}
		}
		on("Destructuring") {
			var v0 = 0
			var v1 = 0
			var v2 = 0
			var v3 = 0
			 Result { 22 }.let { (v, x) -> v0 = if (v == null) 1 else 2; v1 = if (x == null) 1 else 2 }
			 Result { 22/0 }.let { (v, x) -> v2 = if (v == null) 1 else 2; v3 = if (x == null) 1 else 2 }
			it("") {
				shouldEqual(v0, 2)
				shouldEqual(v1, 1)
				shouldEqual(v2, 1)
				shouldEqual(v3, 2)
			}
		}
		on("Accessors") {
			val res0 = Result("ok")
			val res1 = Result("ok", Exception("oops"))
			val res2 = Result<String>(Exception("oops"))
			val v00 = res0.value
			val v10 = res1.value
			val v20 = res2.value
			val v01 = res0.valueOr("yep")
			val v11 = res1.valueOr("yep")
			val v21 = res2.valueOr("yep")
			val v02 = res0.valueOrThrow
			val v12 = res0.valueOrThrow
			val v03 = res0.valueOr { "yep" }
			val v13 = res1.valueOr { "yep" }
			val v23 = res2.valueOr { "yep" }
			val v04 = res0.valueAs { 21 }.value
			val v14 = res1.valueAs { 21 }.value
			val v24 = res2.valueAs { 21 }.value
			val v05 = res0.failure
			val v15 = res1.failure
			val v25 = res2.failure
			val v06 = res0.failureAs { NullPointerException() }.failure
			val v16 = res1.failureAs { NullPointerException() }.failure
			val v26 = res2.failureAs { NullPointerException() }.failure
			val v07 = res0.ifValue { 11 }
			val v17 = res1.ifValue { 11 }
			val v27 = res2.ifValue { 11 }
			val v08 = res0.ifFailure { 11 }
			val v18 = res1.ifFailure { 11 }
			val v28 = res2.ifFailure { 11 }
			var v09 = 0
			var v19 = 0
			var v29 = 0
			res0.onValue { v09 = 1 }.onFailure { v09 = -1 }
			res1.onValue { v19 = 1 }.onFailure { v19 = -1 }
			res2.onValue { v29 = 1 }.onFailure { v29 = -1 }
			
			it("") {
				shouldEqual(v00, "ok")
				shouldEqual(v10, "ok")
				shouldBeNull(v20)
				shouldEqual(v01, "ok")
				shouldEqual(v11, "ok")
				shouldEqual(v21, "yep")
				shouldEqual(v02, "ok")
				shouldEqual(v12, "ok")
				shouldThrow(Exception::class.java) { res2.valueOrThrow }
				shouldEqual(v03, "ok")
				shouldEqual(v13, "ok")
				shouldEqual(v23, "yep")
				shouldEqual(v04, 21)
				shouldEqual(v14, 21)
				shouldBeNull(v24)
				shouldBeNull(v05)
				shouldNotBeNull(v15)
				shouldNotBeNull(v25)
				shouldBeNull(v06)
				shouldNotBeNull(v16)
				shouldNotBeNull(v26)
				shouldEqual(v26!!::class.java, NullPointerException::class.java)
				shouldBeNull(v07)
				shouldNotBeNull(v17)
				shouldNotBeNull(v27)
				shouldEqual(v08, "ok")
				shouldEqual(v18, "ok")
				shouldBeNull(v28)
				shouldEqual(v09, 1)
				shouldEqual(v19, -1)
				shouldEqual(v29, -1)
				
			}
		}
		
		
	}
}
}