package testSuspensions

import just4fun.kotlinkit.Result
import just4fun.kotlinkit.Safely
import just4fun.kotlinkit.ThreadInfo.info
import just4fun.kotlinkit.async.AsyncResult
import just4fun.kotlinkit.async.ResultTaskState
import just4fun.kotlinkit.async.SuspendTask
import just4fun.kotlinkit.now
import just4fun.kotlinkit.startTime
import just4fun.kotlinkit.logL
import just4fun.kotlinkit.rnd0
import just4fun.kotlinkit.rndChance
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor

fun main(args: Array<String>) {
	SInterceptor.sharedExecutor = Executors.newSingleThreadExecutor({ Thread(it, "shared") })
	test()
}

var NextID = 0
val maxlevel = 3
val Sleep = 100L
val durationSec = 60 * 60 * 8
val deadline = now() + durationSec * 1000
var root: Config? = null

fun test() {
	startTime = System.currentTimeMillis()
	root = Config()
//		root = gen()
	val cfg = root!!
	val buff = StringBuilder()
	cfg.prn(buff)
	println("$buff")
	val task = STask(cfg)
	task.run().onComplete {
		val total = task.config.total
		logL(2, task, "RESULT= ${it.failure?.prn() ?: it.value}    from $total")
		Thread.sleep(600)
		val expect = task.config.result()
		if (expect != it.valueOr(0)) {
			cfg.failed = true
			logL(2, task, "Expect = $expect VS  Actual= ${it.valueOr(0)}   !!!  ---------------------------------------")
		}
//		if (task.config.isDirtyInterrupted) cfg.failed = true
		println(if (cfg.isFailed) "Oops..  !!!  FAILED" else "\n\n\n\n\n\n\n")
		if (now() < deadline && !cfg.isFailed) {
//			Thread.sleep(100)
			test()
		} else {
			SInterceptor.sharedExecutor!!.shutdown()
			SInterceptor.sharedExecutor!!.awaitTermination(2000, TimeUnit.MILLISECONDS)
		}
	}
}

fun Throwable.prn() = "${javaClass.simpleName}${if (message == null) "" else message}"



class STask(val config: Config): SuspendTask<Int>() {
	init {
		config.task = this
		if (config.name == "x") config.init()
		logL(2, this, "${config.interceptorId}  ${config.childs.size} x ${config.cancel}")
	}
	
	fun run(): AsyncResult<Int> = start(config.interceptor) { suspend() }
	suspend fun runSuspended(): Result<Int> {
		var executed = false
		try {
			return startSuspended(config.interceptor) { executed = true; suspend() }
		} finally {
			if (!executed) config.cancelled = isCancelled
		}
	}
	
	suspend fun suspend(): Int {
		logL(2, this, "○  ${if (isCancelled) "  Cancelled" else ""}")
		config.checkIntercept(config.parent)
		config.sum = 0
		if (config.childs.isEmpty()) {
			if (config.cancel == 0) doCancel(null)
			Safely({ Thread.sleep(Sleep) }, { logL(2, this, "interrupted ") })
		}
		//
		config.childs.forEachIndexed { n, child ->
			if (config.cancel == n) doCancel(child)
			Safely({ Thread.sleep(Sleep) }, { logL(2, this, "interrupted ") })
			//
			val task = STask(child)
			val res = Safely { task.runSuspended() }
			logL(2, task, "Res= ${res?.failure?.prn() ?: res?.value ?: "see red message !!!"}")
			//			checkInterrupted(task)
			config.checkIntercept(child)
			//
			val v = res?.valueOr(0) ?: 0
			config.sum += v
		}
		//
		checkCancel()
		config.sum++
		if (config.interceptorOpt == 2) config.interceptor?.shutdown()
		return config.sum
	}
	
	fun doCancel(child: Config?) = thread {
		Thread.sleep(Sleep / 3)
		val canceller = chooseCanceller(config, 10)
		synchronize {
			if (canceller.task!!.isComplete) return@thread
			logL(2, canceller, "cancel")
			child?.cancelSelfAndChilds(null)
			var chCfg: Config? = child
			var parCfg: Config? = config
			while (parCfg != null) {
				parCfg!!.cancelSelfAndChilds(chCfg)
				if (parCfg === canceller) break
				chCfg = parCfg
				parCfg = parCfg!!.parent
			}
		}
		canceller.task!!.cancel(Oops(), true)
		var parCfg: Config? = config
		while (parCfg != null) {
			parCfg!!.task!!.checkCancel()
			if (parCfg === canceller) break
			parCfg = parCfg!!.parent
		}
	}
	
	fun chooseCanceller(curr: Config, chance: Int): Config =
	  if (chance >= 2 && curr.parent != null && rndChance(chance)) chooseCanceller(curr.parent!!, chance / 2)
	  else curr
	
	fun checkCancel() {
		if (config.cancelled != isCancelled) {
			config.failed = true
			logL(2, this, "Should ${if (isCancelled) "" else " not"} be cancelled !!! ----------------------------------------------")
		}
	}
	
	fun checkInterrupted() {
		if (Thread.currentThread().isInterrupted != isCancelled) {
			config.failed = true
			logL(2, this, "Should ${if (isCancelled) "" else " not"} be interrupted !!! ----------------------------------------------")
		}
	}
	
	override fun toString() = config.toString()
}




class Config(var interceptorOpt: Int = -1, var cancel: Int = -1, vararg childs: Config) {
	var parent: Config? = null
	var name: String = "x"// = id.padEnd((maxlevel - 1) * 2, '\t')
	var interceptor: SInterceptor? = if (interceptorOpt < 0) null else genInterceptor(interceptorOpt)
	var childs: Array<out Config> = childs
	var task: STask? = null
	var cancelled = false
	var thread: Thread = Thread.currentThread()
	var threadId = ""
	var failed = false
	var sum = -1
	
	fun init(name: String = "0", parent: Config? = null): Config {
		this.name = name
		this.parent = parent
		if (interceptorOpt < 0) run { interceptorOpt = rnd0(2);interceptor = genInterceptor(interceptorOpt) }
		if (cancel < 0 && childs.isEmpty()) {
			if (name.length <= maxlevel) childs = Array(rnd0(3)) { Config() }
			cancel = if (childs.isEmpty()) rnd0(maxlevel) else rnd0(childs.size * 2 + 1)
		}
		childs.forEachIndexed { ix, ch -> ch.init(name + ix, this) }
		return this
	}
	
	fun cancelSelfAndChilds(after: Config?) {
		if (task != null && task!!.state == ResultTaskState.EXECUTED) return
		cancelled = true
		if (childs.isEmpty()) return
		var found = after == null
		for (ch in childs) {
			if (ch === after) {
				found = true; continue
			}
			if (found) ch.cancelSelfAndChilds(null)
		}
	}
	
	val isCancelled get(): Boolean = cancelled || parent?.isCancelled ?: false
	val isFailed get(): Boolean = failed || childs.any { it.isFailed }
//	val isDirtyInterrupted: Boolean get() {
//		val dirty = task != null && task!!.interrupted
//		if (dirty) log(2, this, "Dirty interruption   !!!  ---------------------------------------")
//		return dirty || childs.any { it.isDirtyInterrupted }
//	}
	
	val total get(): Int = 1 + childs.sumBy { it.total }
	
	fun result(): Int {
		if (task != null && cancelled != task!!.isCancelled) {
			failed = true
			logL(2, this, "Must ${if (task!!.isCancelled) "" else " not"} be cancelled !!! ----------------------------------------------")
		}
		return (if (isCancelled) 0 else 1) + childs.sumBy { it.result() }
	}
	
	fun checkIntercept(successor: Config?) {
		thread = Thread.currentThread()
		threadId = info.get().toString()
		if (successor == null || successor.isCancelled) return
		val equal1 = successor != parent && successor.noInterceptor()
		  || (interceptorOpt != 2 && (interceptorOpt == 0 || interceptorOpt == successor.interceptorOpt || (successor.interceptorOpt == 0 && successor.thread.name == "shared")))
		val equal2 = successor.thread == thread
		if (equal1 != equal2) {
			failed = true
			logL(2, this, "$threadId [$this]:$interceptorId   VS   ${successor.threadId} [$successor]:${successor.interceptorId}:  same icps? $equal1;  same ths? $equal2  !!! ----------------------------------------------")
		}
	}
	
	fun noInterceptor(): Boolean = sum == -1 || (interceptorOpt == 0 && (childs.isEmpty() || childs.all { it.noInterceptor() }))
	
	val interceptorId get() = if (interceptor == null) "'X'" else if (interceptor == SInterceptor.shared) "'S'" else "'N'"
	fun genInterceptor(opt: Int): SInterceptor? = when (opt) {
		0 -> SInterceptor.none
		1 -> SInterceptor.shared
		else -> SInterceptor.new
	}
	
	fun prn(buff: StringBuilder) {
		if (name == "x") init()
		buff.append("".padStart(name.length * 2, '\t')).append("[$this]:   ")
		buff.append("$interceptorId  ${childs.size} x $cancel")
		buff.append("\n")
		childs.forEach { it.prn(buff) }
	}
	
	override fun toString() = name.padEnd((maxlevel - 1) * 2, '\t')
}




class SInterceptor(val executor: ExecutorService): AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
	companion object {
		var sharedExecutor: ExecutorService? = null//Executors.newCachedThreadPool()
		var none: SInterceptor? = null
		val shared by lazy { SInterceptor(sharedExecutor!!) }
		val new get() = SInterceptor(Executors.newSingleThreadExecutor())
	}
	
	fun shutdown() = executor.shutdown()
	override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = object: Continuation<T> {
		override val context = continuation.context
		override fun resume(value: T) = executor.execute { continuation.resume(value) }
		override fun resumeWithException(exception: Throwable) = continuation.resumeWithException(exception)
	}
}


class Oops: Exception()

fun gen() =
  Config(0, 10,
	Config(0, 10,
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  )
	),
	Config(0, 10,
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  )
	),
	Config(0, 10,
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  )
	),
	Config(0, 10,
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  ),
	  Config(0, 0,
		Config(0, 10), Config(0, 10), Config(0, 10), Config(0, 10)
	  )
	)
  )

/**
 
 */
