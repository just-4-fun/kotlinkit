//package just4fun.core.test.testSuspensions
//
//
//
//
//import just4fun.core.modules.utils.Result
//import just4fun.core.modules.utils.ResultTask
//import java.util.concurrent.CancellationException
//import kotlin.coroutines.experimental.AbstractCoroutineContextElement
//import kotlin.coroutines.experimental.Continuation
//import kotlin.coroutines.experimental.ContinuationInterceptor
//import kotlin.coroutines.experimental.CoroutineContext
//import kotlin.coroutines.experimental.intrinsics.createCoroutineUnchecked
//import just4fun.core.modules.utils.ResultTaskState.*
//import just4fun.core.modules.utils.TaskContext
//import just4fun.core.modules.utils.Safely
//
////typealias Reaction<T> = (Result<T>) -> Unit
//
//
///* SUSPENSIONS */
//
//class SuspendedTaskB<T>(context: CoroutineContext?, continuation: Continuation<Result<T>>?, private val code: suspend () -> T): SuspendedTask<T>(context, continuation) {
//	override fun start(): SuspendedTask<T> = start(code)
//}
//
//class SuspendedTaskC<T>(context: CoroutineContext?, continuation: Continuation<Result<T>>?, private val code: suspend TaskContext.() -> T): SuspendedTask<T>(context, continuation) {
//	override fun start(): SuspendedTask<T> = start(this, code)
//}
//
//class SuspendedTaskR<R, T>(context: CoroutineContext?, continuation: Continuation<Result<T>>?, private val receiver: R, private val code: suspend R.() -> T): SuspendedTask<T>(context, continuation) {
//	override fun start(): SuspendedTask<T> = start(receiver, code)
//}
//
//
//
//
///* SUSPENSION */
//
//
//// TODO leaks scheduler thread execution to child and parent. But lacks the custom scheduler to redirect resumption.
//open class SuspendedTask<T>(context: CoroutineContext?, private val continuation: Continuation<Result<T>>?): Continuation<T>, ResultTask<T>() {
//	internal var parent: SuspendedTask<*> = this// stays for root task, changes otherwise
//	internal var child: SuspendedTask<*>? = null
//	internal var interceptor: ContinuationInterceptor? = null
//	@PublishedApi internal lateinit var tracker: Tracker
//	override val context: CoroutineContext = run {
//		val orig = continuation?.context
//		tracker = if (orig == null) Tracker(this) else orig as? Tracker ?: orig[ContinuationInterceptor] as? Tracker ?: Tracker(this)
//		interceptor = if (context == null) null else context as? ContinuationInterceptor ?: context[ContinuationInterceptor]
//		val c = if (context == null || context === interceptor) tracker else context + tracker
//		if (orig == null || orig === tracker) c else orig + c
//	}
//
//	open fun start(): SuspendedTask<T> = throw NotImplementedError("Use other versions of 'start' or its implementation in subclasses.")
//	fun start(code: suspend () -> T): SuspendedTask<T> = startCoroutine { code.createCoroutineUnchecked(this) }
//	fun <R> start(receiver: R, code: suspend R.() -> T): SuspendedTask<T> = startCoroutine { code.createCoroutineUnchecked(receiver, this) }
//	private inline fun startCoroutine(code: () -> Continuation<Unit>): SuspendedTask<T> {
//		val coroutine = synchronized(tracker) {
//			if (state > CREATED) null
//			else if (tracker.starting(this)) run { state = RUNNING; code() }
//			else run { cancel(interrupt = false); null }
//			//			else run { cancel(Exception("Tracker boozing ${tracker.message(this)}")); null }//TODO
//		}
//		coroutine?.resume(Unit)// should be out of sync
//		return this
//	}
//
//
//	open protected fun onComplete(result: Result<T>): Unit = Unit
//
//	final override fun cancel(cause: Throwable, interrupt: Boolean) = complete(Result.Failure(cause), true, interrupt)
//	final override fun resume(value: T) = complete(Result.Success(value), false, false)
//	final override fun resumeWithException(exception: Throwable) = complete(Result.Failure(exception), false, false)
//
//	private fun complete(res: Result<T>, cancelled: Boolean, interrupt: Boolean) {
//		synchronized(tracker) {
//			// even if task was cancelled, if it wasn't intercepted to other thread it should be tracked here to detect actual resume
//			if (state > CREATED && !parent.isComplete) tracker.complete(this, !isComplete, cancelled)
//			if (state > RUNNING) return
//			state = if (cancelled) CANCELLED else EXECUTED
//			result = res
//		}
//		if (cancelled) child?.cancel(res.exceptionOrNull!!, interrupt)
//		if (reaction != null) Safely { reaction!!.invoke(result!!) }
//		onComplete(res)
//		// doesn't resume right away if wasn't intercepted to other thread. Should wait actual resume..
//		continuation?.resume(res)
//		child = null
//	}
//}
//
//
//
///* TRACKER */
//
//class Tracker(val root: SuspendedTask<*>): AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
//	private var active: SuspendedTask<*> = root
//	private lateinit var thread: Thread
//	val isCanceled get(): Boolean = active.isComplete || thread != Thread.currentThread()
//
//	internal fun starting(curr: SuspendedTask<*>): Boolean = synchronized(this) {
//		//		log(1, curr, "${if (isCanceled) "not " else ""}starting")
//		if (curr === root) {
//			thread = Thread.currentThread()
//			return true
//		}
//		curr.parent = active
//		if (active.isComplete) return false
//		active.child = curr
//		active = curr
//		true
//	}
//
//	//	fun message(curr: SuspendTask<*>) = if (curr === root)"root" else if (active.isComplete)"active complete" else if (thread != Thread.currentThread()) "wrong thread" else "ok"
//
//	internal fun complete(curr: SuspendedTask<*>, justComplete: Boolean, canceled: Boolean): Unit = synchronized(this) {
//		if (justComplete) {
//			//			if (canceled) thread.interrupt()
//			active = curr.parent
//			thread = Thread.currentThread()
//			//			log(1, curr, "${if (canceled)"cancelled" else "complete"}   > [$active]")
//		} else if (!canceled && curr.parent === active) thread = Thread.currentThread()
//		//		} else if (canceled) Thread.interrupted()// todo ??? clears interrupted status if any
//	}
//
//	override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = synchronized(this) {
//		if (continuation.context !== active.context) throw CancellationException("Coroutine is cancelled while starting.")
//		val media = Media(active, continuation)
//		media.interceptContinuation = active.interceptor?.interceptContinuation(media)
//		return media
//	}
//
//	inner private class Media<T>(val current: SuspendedTask<*>, val continuation: Continuation<T>): Continuation<T> {
//		override val context = continuation.context
//		internal var interceptContinuation: Continuation<T>? = null
//		private var inited = false
//		private var expect = false
//
//		override fun resume(value: T) {
//			if (!expect && interceptContinuation != null) {
//				val icpr = if (inited) current.child?.interceptor else run { inited = true; current.parent.interceptor }
//				if (icpr !== current.interceptor || current.parent === current) {
//					expect = true
//					try {
//						interceptContinuation!!.resume(value)// expecting RejectedExecutionException
//					} catch(e: Throwable) {
//						expect = false
//						resumeWithException(e)
//					}
//					return
//				}
//			}
//			expect = false
//			if (current === active && !active.isComplete) thread = Thread.currentThread()
//			//			log(1, current, "resumed    ${if (current != active) "!!!" else ""}")
//			continuation.resume(value)
//		}
//
//		override fun resumeWithException(exception: Throwable) {
//			if (current === active && !active.isComplete) thread = Thread.currentThread()
//			//			log(1, active, "!!!  this shouldn't happen: $exception    active? ${current == active}");
//			continuation.resumeWithException(exception)
//		}
//	}
//}
