@file:Suppress("OVERRIDE_BY_INLINE")

package just4fun.kotlinkit


/**
 * Holds the result of some method invocation which possible outcomes are:
 * - Failure: [failure] is assigned with exception occurred; [value] is 'null'.
 * - Success: [value] is assigned with value of [T]; [failure] is 'null'.
 * - Success with some issue: [value] is assigned with value of [T] while [failure] is assigned with issue occurred.
 *
 * Both [value] and [failure] can not be 'null'.
 *
 * @param [value] - the value of successful the invocation.
 * @param [failure] - indicates a problem occurred during the invocation. Can be critical error (no [value] is set) as well as some issue along with the [value] set.
 */

@Suppress("UNCHECKED_CAST")
class Result<T> @PublishedApi internal constructor(val value: T?, val failure: Throwable?, val hasValue: Boolean) {
	
	/** Indicates that [failure] is set. */
	val hasFailure: Boolean get()= failure != null
	
	/** Returns [value] if set, or throws [failure].*/
	val valueOrThrow: T get() = if (hasValue) value as T else throw failure as Throwable
	
	/** Returns either [value] or [altValue] if the [value] isn't set.*/
	fun valueOr(altValue: T): T = if (hasValue) value as T else altValue
	
	/** Returns either [value] or a result of the [altValueFun] call if the [value] isn't set.*/
	inline fun valueOr(altValueFun: (Throwable) -> T): T = if (hasValue) value as T else altValueFun(failure  as Throwable)
	
	/** If [value] is set creates new [Result] assigned with new value from transformation of original value by [altValueFun]  and returns it. Otherwise returns this.*/
	inline fun <V> valueAs(altValueFun: (original: T) -> V): Result<V> =
	  if (hasValue) Result(altValueFun(value as T)) else this as Result<V>
	
	/** If [value] is set executes [code] with it as argument. Returns [failure]. */
	inline fun ifValue(code: (T) -> Unit): Throwable? {
		if (hasValue) code(value as T)
		return failure
	}
	
	/** If [value] is set executes [code] with it as argument. Returns this. */
	inline fun onValue(code: (T) -> Unit): Result<T> {
		if (hasValue) code(value as T)
		return this
	}
	
	/** If [failure] is set creates new [Result] assigned with new failure from transformation of original one by [altFailureFun] and returns it. Otherwise returns this.*/
	inline fun failureAs(altFailureFun: (original: Throwable) -> Throwable): Result<T> =
	  failure?.let { Result<T>(altFailureFun(it).apply { if (cause == null) initCause(it) }) } ?: this
	
	/** If [failure] is set executes [code] with it as argument. Returns [value]. */
	inline fun ifFailure(code: (Throwable) -> Unit): T? {
		failure?.let { code(it) }
		return value
	}
	
	/** Executes [code] if it's [Failure]. Returns itself so as to allow call chaining.*/
	/** If [failure] is set executes [code] with it as argument. Returns this. */
	inline fun onFailure(code: (Throwable) -> Unit): Result<T> {
		failure?.let { code(it) }
		return this
	}
	
	/** Returns [value]. Serves destructuring purpose. */
	operator fun component1(): T? = value
	
	/** Returns [failure]. Serves destructuring purpose. */
	operator fun component2(): Throwable? = failure
	
	override fun toString(): String = "Result(${if (hasValue) value else failure})"
	override fun hashCode(): Int = value?.hashCode() ?: failure?.hashCode() ?: 0
	override fun equals(other: Any?) = this === other ||
	  (other is Result<*> && (other.failure == failure && other.value == value))
	
	
	
	/** Companion object provides construction functions.*/
	companion object {
		/** Creates [Result] with successful [value] of [T]*/
		operator fun <T> invoke(value: T): Result<T> = Result(value, null, true)
		
		/** Creates [Result] with successful [value] of [T] and an non-critical [failure] occurred during invocation. */
		operator fun <T> invoke(value: T, failure: Throwable): Result<T> = Result(value, failure, true)
		
		/** Creates [Result] with critical [failure].*/
		operator fun <T> invoke(failure: Throwable): Result<T> = Result(null, failure, false)
		
		/** Executes [code] and returns [Result] with [value] in case of a success and [Result] with [failure] in case an exception was thrown during [code] execution. */
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
