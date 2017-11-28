package compareResultImpl.optimized


class Result<T> {
	@PublishedApi internal var any: Any? = null
	@PublishedApi internal var success = true
	
	constructor (value: T) {
		any = value
	}
	
	constructor (exception: Throwable) {
		any = exception
		success = false
	}
	
	val value: T? get() = if (success) any as T else null
	val exception: Throwable? get() = if (success) null else any as Throwable
	val isSuccess: Boolean get() = success
	val isFailure: Boolean get() = !success
	val valueOrThrow: T get() = if (success) any as T else throw any as Throwable
	
	fun valueOr(altValue: T): T = if (success) any as T else altValue
	inline fun valueOr(code: (Throwable) -> T): T = if (success) any as T else code(any as Throwable)
	
	inline fun onSuccess(code: (T) -> Unit): Result<T> {
		if (success) code(any as T)
		return this
	}
	
	inline fun onFailure(code: (Throwable) -> Unit): Result<T> {
		if (!success) code(any as Throwable)
		return this
	}
	
	inline fun <R> mapSuccess(code: (T) -> R): Result<R> {
		return if (success) Result(code(any as T)) else this as Result<R>
	}
	
	inline fun <R> flatMapSuccess(code: (T) -> Result<R>): Result<R> {
		return if (success) code(any as T) else this as Result<R>
	}
	
	inline fun mapFailure(code: (Throwable) -> T): Result<T> {
		return if (success) this else Result(code(any as Throwable))
	}
	
	inline fun flatMapFailure(code: (Throwable) -> Result<T>): Result<T> {
		if (!success) return code(any as Throwable)
		return this
	}
	
	inline fun wrapFailure(code: (original: Throwable) -> Throwable): Result<T> {
		return if (success) this else (any as Throwable).let { x ->
			Result<T>(code(x).also { if (it.cause == null) it.initCause(x) })
		}
	}
	
	operator fun component1(): T? = if (success) any as T else null
	operator fun component2(): Throwable? = if (success) null else any as Throwable
	
	override fun toString(): String = "ResultB($any)"
	override fun hashCode(): Int = any?.hashCode() ?: 0
	override fun equals(other: Any?) = this === other || (other is Result<*> && other.any == any)
	
	
	companion object {
		fun <T> Success(value: T): Result<T> = Result(value)
		fun <T> Failure(exception: Throwable): Result<T> = Result(exception)
	}
}


inline fun <T> ResultB(code: () -> T): Result<T> = try {
	Result(code())
} catch (x: Throwable) {
	Result(x)
}


@Suppress("UNCHECKED_CAST")
fun <T> Result<Result<T>>.flatten(): Result<T> = value ?: this as Result<T>
