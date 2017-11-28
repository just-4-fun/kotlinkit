package compareResultImpl



/* PERFORMANCE MERTICS */

fun <T> measureTime(tag: String = "", times: Int = 1, warmup: Boolean = true, antiSurgeRate: Double = .0, code: () -> T): T {
	// warm-up
	var prevTime = 0L
	if (warmup) {
		val ratioMax = 10f / 9f
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
		if (antiSurgeRate > 0 && prevTime > 0 && t1 >= prevTime * antiSurgeRate) continue // against extreme surges @ by java class newInstance()
		t += t1
		prevTime = t1
		count--
		tries += t1
	} while (count > 0)
	println("$tag ::  $times times;  ${t / 1000000} ms;  $t ns;  ${t / times} ns/call")
	return result
}

private val tries = mutableListOf<Long>()

fun measuredStats() {
	var totalN = 0
	var totalNs = 0L
	var maxN = 0L
	var minN = Long.MAX_VALUE
	val midN = tries.sum()/tries.size
	for (t in tries) {
		if (t > maxN) maxN = t
		if (t < minN) minN = t
		if (t > midN*1.5) continue // cut gc surges
		totalN ++
		totalNs += t
	}
	val measuredTimeAvg = if (totalN == 0) 0 else totalNs / totalN
	println("calls= $totalN;  min= $minN;  max= $maxN;  avg= $measuredTimeAvg")
	tries.clear()
}
