package just4fun.kotlinkit


object Safely {
	var stackSizeLimit = 6

	operator inline fun <T> invoke(code: () -> T): T? = try {
		code()
	} catch (e: Throwable) {
		val size = if (e.stackTrace.size > stackSizeLimit) stackSizeLimit else e.stackTrace.size
		val stack = if (size > 0) "\n" + e.stackTrace.take(size).joinToString("\n") else ""
		System.err.println("$e$stack")
		null
	}
	
	operator inline fun <T> invoke(code: () -> T, catch: (Throwable) -> T): T = try {
		code()
	} catch (e: Throwable) {
		catch(e)
	}
	
	operator inline fun <T> invoke(code: () -> T, catch: (Throwable) -> T, finally: () -> Unit): T = try {
		code()
	} catch (e: Throwable) {
		catch(e)
	} finally {
		finally()
	}
}
