@file:Suppress("OVERRIDE_BY_INLINE")

package just4fun.kotlinkit


/* Utils */

@Suppress("UNCHECKED_CAST")
fun <T> Result<Result<T>>.flatten(): Result<T> = when (this) {
	is Result.Success -> value
	is Result.Failure -> this as Result.Failure<T>
}


/* Result*/

/** Subclasses [Success] and [Failure] represent result of some method.*/
sealed class Result<T> {
	
	companion object {
		/** Tries to execute the [code] and returns [Success] or [Failure].*/
		operator fun <T> invoke(code: () -> T): Result<T> {
			return try {
				Success(code())
			} catch (x: Throwable) {
				Failure(x)
			}
		}
	}
	
	val xxx: Int = 0
	
	/** Returns true if [Success].*/
	abstract val isSuccess: Boolean
	
	/** Returns true if [Failure].*/
	abstract val isFailure: Boolean
	
	/** Returns [Success.value] or throws [Failure.exception].*/
	abstract val valueOrThrow: T
	
	/** Returns either [Success.value] or null if the result is [Failure]. If the [T] is nullable type returning null is ambiguous. */
	abstract val valueOrNull: T?
	
	/** Returns either [Success.value] or [altValue] if the result is [Failure].*/
	abstract fun valueOr(altValue: T): T
	
	/** Returns either [Success.value] or a result of the [altValueFun]. call.*/
	inline fun valueOr(altValueFun: (Throwable) -> T): T = (this as? Success)?.value ?: altValueFun((this as Failure).exception)
	
	/** Returns either [Failure.exception] or null if the result is [Success].*/
	abstract val exceptionOrNull: Throwable?
	
	/** If [Failure] wraps its exception as a cause and returns new [Failure] otherwise just returns the same [Success].*/
	inline fun exceptionAs(wrapper: (original: Throwable) -> Throwable): Result<T> =
	  if (this is Failure) Failure<T>(wrapper(exception).apply { if (cause == null) initCause(exception) }) else this
	
	/** Executes [code] if it's [Success]. Returns itself so as to allow call chaining.*/
	inline fun ifSuccess(code: (T) -> Unit): Result<T> {
		if (this is Success) code(value)
		return this
	}
	
	/** Executes [code] if it's [Failure]. Returns itself so as to allow call chaining.*/
	inline fun ifFailure(code: (Throwable) -> Unit): Result<T> {
		if (this is Failure) code(exception)
		return this
	}
	
	/**@see [valueOrNull]*/
	abstract operator fun component1(): T?
	
	/**@see [exceptionOrNull]*/
	abstract operator fun component2(): Throwable?
	
	
	/*Success*/
	
	/** Represents successful result.
	
	@property[value] An actual resulting value: [T].
	 */
	class Success<T>(val value: T): Result<T>() {
		
		override val isSuccess get() = true
		override val isFailure get() = false
		override val valueOrThrow: T get() = value
		override val valueOrNull: T? get() = value
		override fun valueOr(altValue: T): T = value
		override val exceptionOrNull: Throwable? get() = null
		override fun component1(): T? = value
		override fun component2(): Throwable? = null
		override fun toString(): String = "Success($value)"
		override fun hashCode(): Int = value?.hashCode() ?: 0
		override fun equals(other: Any?) = this === other || (other is Success<*> && other.value == value)
	}
	
	
	/*Failure*/
	
	/** Represents failed result.
	
	@property[exception] An exception the result's failed with.
	 */
	class Failure<T>(val exception: Throwable): Result<T>() {
		constructor(message: String? = null): this(if (message == null) Exception() else Exception(message))
		
		override val isSuccess get() = false
		override val isFailure get() = true
		override val valueOrThrow: T get() = throw exception
		override val valueOrNull: T? get() = null
		override fun valueOr(altValue: T): T = altValue
		override val exceptionOrNull: Throwable? get() = exception
		override fun component1(): T? = null
		override fun component2(): Throwable? = exception
		override fun toString(): String = "Failure($exception)"
		override fun hashCode(): Int = exception.hashCode()
		override fun equals(other: Any?) = this === other || (other is Failure<*> && other.exception == exception)
	}
}
