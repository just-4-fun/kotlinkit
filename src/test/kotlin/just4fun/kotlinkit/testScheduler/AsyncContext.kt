//package just4fun.core.test.testScheduler.async
//
//import nl.komponents.kovenant.Context
//import nl.komponents.kovenant.DispatcherContext
//import nl.komponents.kovenant.Kovenant
//
//
//interface AsyncContextOwner
//
//
//open class AsyncContext(private val owner: AsyncContextOwner, private val contextBuilder: () -> Context): Context {
//	@Volatile private var context: Context? = null
//	private val lock = Any()
//
//	override var workerContext = context().workerContext
//		get() = context().workerContext
//	override var callbackContext = context().callbackContext
//		get() = context().callbackContext
//	override var multipleCompletion = context().multipleCompletion
//		get() = context().multipleCompletion
//
//	/**  do not force stopModule because all promises will be broken. Only soft stopModule. And cancel promises via module. */
//	fun stop(caller: AsyncContextOwner, force: Boolean = false) {
//		if (caller === owner) stop(force, 0, true)
//	}
//
//	override fun stop(force: Boolean, timeOutMs: Long, block: Boolean): List<() -> Unit> {
//		val undone = super.stop(false, 0, block)
//		context = null
//		return undone
//	}
//
//	private fun context(): Context = context ?: synchronized(lock) {
//		context ?: run { context = contextBuilder(); context!! }
//	}
//}
