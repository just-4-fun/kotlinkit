package compareResultImpl.monad


sealed class Result<T> {
	abstract val isSuccess: Boolean
	abstract val isFailure: Boolean
	abstract val valueOrThrow: T
	abstract val valueOrNull: T?
	abstract fun valueOr(altValue: T): T
	inline fun valueOr(code: (Throwable) -> T): T = (this as? Success)?.value ?: code((this as Failure).exception)
	abstract val exceptionOrNull: Throwable?
	
	inline fun onSuccess(code: (T) -> Unit): Result<T> {
		if (this is Success) code(value)
		return this
	}
	
	inline fun onFailure(code: (Throwable) -> Unit): Result<T> {
		if (this is Failure) code(exception)
		return this
	}
	
	inline fun <R> mapSuccess(code: (T) -> R): Result<R> {
		return if (this is Success) Success(code(value)) else this as Result<R>
	}
	
	inline fun <R> flatMapSuccess(code: (T) -> Result<R>): Result<R> {
		return if (this is Success) code(value) else this as Result<R>
	}
	
	inline fun mapFailure(code: (Throwable) -> T): Result<T> {
		return if (this is Failure) Success(code(exception)) else this
	}
	
	inline fun flatMapFailure(code: (Throwable) -> Result<T>): Result<T> {
		if (this is Failure) return code(exception)
		return this
	}
	
	inline fun wrapFailure(wrapper: (original: Throwable) -> Throwable): Result<T> =
	  if (this is Failure) Failure<T>(wrapper(exception).apply { if (cause == null) initCause(exception) }) else this
	
	
	abstract operator fun component1(): T?
	abstract operator fun component2(): Throwable?
	
	
	/*Success*/
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
	class Failure<T>(val exception: Throwable): Result<T>() {
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
	
	
	companion object {
		operator fun <T> invoke(code: () -> T): Result<T> {
			return try {
				Success(code())
			} catch (x: Throwable) {
				Failure(x)
			}
		}
	}
	
}


@Suppress("UNCHECKED_CAST")
fun <T> Result<Result<T>>.flatten(): Result<T> = when (this) {
	is Result.Success -> value
	is Result.Failure -> this as Result.Failure<T>
}
