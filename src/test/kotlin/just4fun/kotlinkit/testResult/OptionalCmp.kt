package just4fun.kotlinkit.testResult

import just4fun.kotlinkit.Result
import just4fun.kotlinkit.measureTime
import just4fun.kotlinkit.measuredStats
import java.util.*
import kotlin.reflect.KClass

fun main(a: Array<String>) {
	// same speed
	r();r();r();r();r();r();r();r();r();r();
//	o();o();o();o();o();o();o();o();o();o();
	measuredStats()
}

val N1 = 1
// 650 - 850
fun r() = measureTime("Result", N1) {
	val res = Result(10)
	res.valueOrThrow
}

// 650 - 850
fun o() = measureTime("Opt", N1) {
	val res = Optional.of(10)
	res.get()
}

class Something<T>(val value: T?) {
	companion object {
		operator inline fun <T> invoke(v: T): Something<T> = Something(v)
		operator inline fun <T> invoke(code: () -> T): Something<T> = Something(code())
	}
}
//inline fun <T> Something(code: () -> T):Something<T> = Something(code())
//fun test():Something<Int> = Something { 0 }

