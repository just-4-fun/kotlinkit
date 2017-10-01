package just4fun.kotlinkit

import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicInteger


var DEBUG = run { var v = false; assert({ v = true; true }()); v }

fun now() = System.currentTimeMillis()

fun nowNs() = System.nanoTime()

/* logging */
internal var startTime = System.currentTimeMillis()
private val fmt by lazy { DecimalFormat("00.00") }
val time: String get() = fmt.format((System.currentTimeMillis() - startTime).toInt() / 1000f)

var logFun: (priority: Int, id: Any, msg: String) -> Unit = { _, id, msg -> println("${time}    ${ThreadInfo.info.get()} [$id]::    $msg") }

var logPriorityShift = 2
fun log(id: Any, msg: String) = run { if (DEBUG) logFun(0 + logPriorityShift, id, msg) }//VERBOSE
fun log1(id: Any, msg: String) = run { if (DEBUG) logFun(1 + logPriorityShift, id, msg) }//DEBUG
fun log2(id: Any, msg: String) = run { if (DEBUG) logFun(2 + logPriorityShift, id, msg) }//INFO
fun log3(id: Any, msg: String) = run { if (DEBUG) logFun(3 + logPriorityShift, id, msg) }//WARNING
fun log4(id: Any, msg: String) = run { if (DEBUG) logFun(4 + logPriorityShift, id, msg) }//ERROR
fun log5(id: Any, msg: String) = run { if (DEBUG) logFun(5 + logPriorityShift, id, msg) }//ASSERT
fun logE(id: Any, msg: String) = System.err.println("${time}    ${ThreadInfo.info.get()} [$id]::    $msg")

internal object ThreadInfo {
	private val nextId = AtomicInteger(0)
	var info: ThreadLocal<Int> = ThreadLocal.withInitial<Int> { nextId.getAndIncrement() }
//	val info = object: ThreadLocal<Int>() {
//		override fun initialValue(): Int {
//			return nextId.getAndIncrement()
//		}
//	}
}



/* PERFORMANCE MERTICS */

fun <T> measureTime(tag: String = "", times: Int = 1, warmup: Boolean = true, antiSurgeRate:Double = .0, code: () -> T): T {
	// warm-up
	var prevTime = 0L
	if (warmup) {
		val ratioMax = 5f / 4f
		var count = 0
		do {
			val t0 = System.nanoTime()
			code()
			val time = System.nanoTime() - t0
			val ratio = prevTime / time.toDouble()
//			if (ratio < 1) println("Warmup $count;  recent= $prevTime;  curr= $time;   ratio= $ratio")
			prevTime = time
		} while (count++ < 2 || ratio > ratioMax)
	}
	//
	var result: T
	var count = times
	var t = 0L
	var t1 = 0L
	do {
		val t0 = System.nanoTime()
		result = code()
		t1 = System.nanoTime() - t0
		if (antiSurgeRate > 0 && prevTime > 0 && t1 >= prevTime*antiSurgeRate) continue // against extreme surges @ by java class newInstance()
		t += t1
		prevTime = t1
		count--
	} while (count > 0)
	println("$tag ::  $times times;  ${t / 1000000} ms;  $t ns;  ${t / times} ns/call")
	totalNs += t
	totalN++
	return result
}

private var totalNs = 0L
private var totalN = 0

val measuredTimeAvg get() = if (totalN == 0) 0 else totalNs / totalN
