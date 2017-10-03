@file:Suppress("OVERRIDE_BY_INLINE")

package just4fun.kotlinkit


/**
 * Presents the result of some method call as a success or failure. A successful result has its [value] set while a failure has its [exception] set.
 *
 * @param [value] - a value of a successful call.
 * @param [exception] - an exception of a failed call.
 */

@Suppress("UNCHECKED_CAST")
class Result<T> @PublishedApi internal constructor(val value: T?, val exception: Throwable?, success: Boolean) {
	
	/** Indicates of successful result. */
	val isSuccess: Boolean = success
	
	/** Indicates of failed result. */
	val isFailure: Boolean get() = !isSuccess
	
	/** Returns [value] in case of a success, otherwise throws [exception].*/
	val valueOrThrow: T get() = if (isSuccess) value as T else throw exception as Throwable
	
	/** Returns [value] in case of a success, otherwise [altValue].*/
	fun valueOr(altValue: T): T = if (isSuccess) value as T else altValue
	
	/** Returns [value] in case of a success, otherwise a result of the [altFun] call.*/
	inline fun valueOr(altFun: (Throwable) -> T): T = if (isSuccess) value as T else altFun(exception as Throwable)
	
	/** Executes [code] with [value] as the argument in case of a success. Returns this result. */
	inline fun onSuccess(code: (T) -> Unit): Result<T> {
		if (isSuccess) code(value as T)
		return this
	}
	
	/** Executes [code] with [exception] as the argument in case of a failure. Returns this result. */
	inline fun onFailure(code: (Throwable) -> Unit): Result<T> {
		if (!isSuccess) code(exception as Throwable)
		return this
	}
	
	/** Returns a result of the execution of [code] with [value] as the argument in case of a success. Otherwise returns 'null'. */
	inline fun <R> ifSuccess(code: (T) -> R): R? = if (isSuccess) code(value as T) else null
	
	/** Returns a result of the execution of [code] with [exception] as the argument in case of a failure. Otherwise returns 'null'. */
	inline fun <R> ifFailure(code: (Throwable) -> R): R? = if (isSuccess) null else code(exception as Throwable)
	
	/** Returns [value]. Serves destructuring purpose. */
	operator fun component1(): T? = value
	
	/** Returns [exception]. Serves destructuring purpose. */
	operator fun component2(): Throwable? = exception
	
	override fun toString(): String = "Result(${if (isSuccess) value else exception})"
	override fun hashCode(): Int = value?.hashCode() ?: exception?.hashCode() ?: 0
	override fun equals(other: Any?) = this === other || (other is Result<*> && other.exception == exception && other.value == value)
	
	
	
	/** Companion object provides [Result] construction.*/
	companion object {
		/** Creates a successful [Result] with [value]*/
		operator fun <T> invoke(value: T): Result<T> = Result(value, null, true)
		
		/** Creates a failed [Result] with [exception].*/
		operator fun <T> invoke(exception: Throwable): Result<T> = Result(null, exception, false)
		
		/** Executes [code] in a try/catch block and returns a failed [Result] if an [exception] was thrown, otherwise returns successful [Result] with [value] */
		operator inline fun <T> invoke(code: () -> T): Result<T> {
			return try {
				Result(code())
			} catch (x: Throwable) {
				Result(x)
			}
		}
	}
}



/** Flattens nested [Result]. */
@Suppress("UNCHECKED_CAST")
fun <T> Result<Result<T>>.flatten(): Result<T> = value ?: this as Result<T>
