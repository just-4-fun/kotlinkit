package compareResultImpl

import compareResultImpl.monad.Result as MResult
import compareResultImpl.optimized.Result as OResult


fun main(a: Array<String>) {
	/* Uncomment one line at a time */
	
//	monadHandle() // 1400 ns/call // high surges of gc !!!
//	optimHandle() // 1100 ns/call
//	directHandle() // 450 ns/call
//	monadCreate() // 850  ns/call
//	optimCreate() // 700 ns/call
//	optimCreateConstr() // 550 ns/call
//	monadTransform() // 1150 ns/call
//	optimTransform() // 1050 ns/call
	
	measuredStats()
}

val NumOfRuns = 20

/* Creation tests */

fun monadCreate(): MResult<Int> = measureTime("Monad", NumOfRuns){
	MResult.Success(1)
}

fun optimCreate(): OResult<Int> = measureTime("Optim", NumOfRuns){
	OResult.Success(1)
}

fun optimCreateConstr(): OResult<Int> = measureTime("Optim", NumOfRuns){
	OResult(1)
}


/* Handle tests */

fun monadHandle():Int = measureTime("Monad", NumOfRuns){
	monadDivide(10, 1)
	  .valueOr(-1)
}
fun monadDivide(num: Int, denom: Int): MResult<Int> {
	return if (denom == 0) MResult.Failure(Exception())
	else MResult.Success(num / denom)
}

fun optimHandle():Int  = measureTime("Optim", NumOfRuns){
	optimDivide(10, 1)
	  .valueOr(-1)
}
fun optimDivide(num: Int, denom: Int): OResult<Int> {
	return if (denom == 0) OResult.Failure(Exception())
	else OResult.Success(num / denom)
}

fun directHandle():Int = measureTime("Direct", NumOfRuns){
	try {
		directDivide(10, 1)
	} catch (x: Exception) {
		-1
	}
}
fun directDivide(num: Int, denom: Int): Int {
	return if (denom == 0) throw Exception()
	else num / denom
}


/* transformation tests */

fun monadTransform():Int = measureTime("Monad", NumOfRuns){
	val okayM: MResult<Int> = MResult.Success(1)
	when (okayM) {
		is MResult.Success ->  okayM.value
		else ->  -1
	}
}

fun optimTransform():Int  = measureTime("Optim", NumOfRuns){
	val okayB = OResult.Success(1)
	when {
		okayB.isSuccess ->  okayB.valueOrThrow
		else ->  -1
	}
}






