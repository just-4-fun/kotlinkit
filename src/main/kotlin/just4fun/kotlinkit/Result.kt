@file:Suppress("OVERRIDE_BY_INLINE")

package just4fun.kotlinkit

@Suppress("UNCHECKED_CAST")
class Result<T> @PublishedApi internal constructor(val value: T?, val failure: Throwable?, val hasValue: Boolean) {
	
	val hasFailure: Boolean get()= failure != null
	
	/** Returns [Value.value] or throws [Failure.failure].*/
	val valueOrThrow: T get() = if (hasValue) value as T else throw failure ?: URE
	
	/** Returns either [Value.value] or [altValue] if the result is [Failure].*/
	fun valueOr(altValue: T): T = if (hasValue) value as T else altValue
	
	/** Returns either [Value.value] or a result of the [altValueFun]. call.*/
	inline fun valueOr(altValueFun: (Throwable) -> T): T = if (hasValue) value as T else altValueFun(failure ?: URE)
	
	/** If [Value] returns new [Value] of value calculated by [altValueFun], otherwise just returns the same [Failure].*/
	inline fun <V> valueAs(altValueFun: (original: T) -> V): Result<V> =
	  if (hasValue) Result(altValueFun(value as T)) else this as Result<V>
	
	/** Executes [code] if it's [Value] and returns 'null'. Returns failure otherwise.*/
	inline fun ifValue(code: (T) -> Unit): Throwable? {
		if (hasValue) code(value as T)
		return failure
	}
	
	/** Executes [code] if it's [Value]. Returns itself so as to allow call chaining.*/
	inline fun onValue(code: (T) -> Unit): Result<T> {
		if (hasValue) code(value as T)
		return this
	}
	
	/** If [Failure] wraps its failure as a cause and returns new [Failure], otherwise just returns the same [Value].*/
	inline fun failureAs(wrapper: (original: Throwable) -> Throwable): Result<T> =
	  failure?.let { Result<T>(wrapper(it).apply { if (cause == null) initCause(it) }) } ?: this
	
	/** Executes [code] if it's [Failure] and returns 'null'. Returns value otherwise.*/
	inline fun ifFailure(code: (Throwable) -> Unit): T? {
		failure?.let { code(it) }
		return value
	}
	
	/** Executes [code] if it's [Failure]. Returns itself so as to allow call chaining.*/
	inline fun onFailure(code: (Throwable) -> Unit): Result<T> {
		failure?.let { code(it) }
		return this
	}
	
	/**@see [valueOrNull]*/
	operator fun component1(): T? = value
	
	/**@see [failureOrNull]*/
	operator fun component2(): Throwable? = failure
	
	override fun toString(): String = "Result(${if (hasValue) value else failure})"
	override fun hashCode(): Int = value?.hashCode() ?: failure?.hashCode() ?: 0
	override fun equals(other: Any?) = this === other ||
	  (other is Result<*> && (other.failure == failure && other.value == value))
	
	
	
	/** */
	companion object {
		@PublishedApi internal val URE get() = UninitializedResultException()
		
		/** */
		operator fun <T> invoke(value: T): Result<T> = Result(value, null, true)
		
		/** */
		operator fun <T> invoke(value: T, failure: Throwable): Result<T> = Result(value, failure, true)
		
		/** */
		operator fun <T> invoke(failure: Throwable): Result<T> = Result(null, failure, false)
		
		/** */
		operator inline fun <T> invoke(code: () -> T): Result<T> {
			return try {
				Result(code())
			} catch (x: Throwable) {
				Result(x)
			}
		}
	}
}



/** */
@Suppress("UNCHECKED_CAST")
fun <T> Result<Result<T>>.flatten(): Result<T> = value ?: this as Result<T>


/** */
class UninitializedResultException: Exception()
