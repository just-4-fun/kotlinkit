package just4fun.kotlinkit

import just4fun.kotlinkit.ThreadInfo.info
import java.util.*

var debug = 1
var N = 0

var durationSec = 60
val deadline get() = startTime + durationSec * 1000
fun logL(level: Int, id: Any, msg: String) = if (level >= debug) println("${time}    ${info.get()} [$id]::    $msg") else Unit

val rnd = Random()
fun rndChance(of: Int) = rnd.nextInt(of) == 0
fun rnd0(max: Int) = rnd.nextInt(max + 1)
fun rnd0(max: Int, zeroRatio: Int) = if (rndChance(max * zeroRatio)) 0 else rnd1(max)// zero happens zeroRatio times less
fun rnd1(max: Int) = rnd.nextInt(max) + 1
fun <T> rnd(values: Array<T>): T = values[rnd.nextInt(values.size)]
fun <T> rnd(values: List<T>): T? = if (values.isEmpty()) null else values[rnd.nextInt(values.size)]

inline fun loopFor(durationMs: Int, code: () -> Unit) {
	val deadline = now() + durationMs
	while (now() < deadline) {
		code()
	}
}

inline fun loopUntil(deadline: Long, code: () -> Unit) {
	while (now() < deadline) {
		code()
	}
}

inline fun loopWhile(condition: () -> Boolean, code: () -> Unit) {
	while (condition()) {
		code()
	}
}
