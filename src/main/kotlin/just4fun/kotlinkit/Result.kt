@file:Suppress("OVERRIDE_BY_INLINE")

package just4fun.kotlinkit

import just4fun.kotlinkit.Result.Companion.errorHandler



/** Executes [code] in a try/catch block and returns a failed [Result] if an [Result.exception] was thrown, otherwise returns successful [Result] with [Result.value] assigned. An error can be handled with [errorHandler]. */
inline fun <T> Result(code: () -> T): Result<T> = try {
	Result(code())
} catch (x: Throwable) {
	errorHandler?.invoke(x)
	Result(x)
}


/** Flattens nested [Result]. */
@Suppress("UNCHECKED_CAST")
fun <T> Result<Result<T>>.flatten(): Result<T> = value ?: this as Result<T>







/**
 * The result of some method call provides the caller with a requested [value] if the call is successful or an [exception] if failed.
 * Replaces the usual approach to throw an exception or return the null in case of an execution failure.
 * Note: [Result.exception] of the successful [Result] is always `null`.
 */

@Suppress("UNCHECKED_CAST")
class Result<T> {
	@PublishedApi internal var any: Any? = null
	@PublishedApi internal var success = true
	
	/** Creates a successful [Result] with [value]. Note: to create a [Result] with the [value] of [Throwable] the [Result.Success] fun can be used. */
	constructor (value: T) {
		any = value
	}
	
	/** Creates a failed [Result] with [exception]. */
	constructor (exception: Throwable) {
		any = exception
		success = false
	}
	
	/** A value of a successful call. */
	val value: T? get() = if (success) any as T else null
	/** exception of a failed call. */
	val exception: Throwable? get() = if (success) null else any as Throwable
	/** Indicates of successful result. */
	val isSuccess: Boolean get() = success
	/** Indicates of failed result. */
	val isFailure: Boolean get() = !success
	/** Returns [value] in case of a success, otherwise throws [exception].*/
	val valueOrThrow: T get() = if (success) any as T else throw any as Throwable
	
	/** Returns [value] in case of a success, otherwise [altValue].*/
	fun valueOr(altValue: T): T = if (success) any as T else altValue
	
	/** Returns [value] in case of a success, otherwise a result of the [code] call.*/
	inline fun valueOr(code: (Throwable) -> T): T = if (success) any as T else code(any as Throwable)
	
	/** Executes [code] in case of a success with [value] as the argument. Returns this. */
	inline fun onSuccess(code: (T) -> Unit): Result<T> {
		if (success) code(any as T)
		return this
	}
	
	/** If this is a success, returns the successful [Result] of the [code] execution. Returns this otherwise. */
	inline fun <R> mapSuccess(code: (T) -> R): Result<R> {
		return if (success)  try {
			Result(code(any as T))
		} catch (x: Throwable) {
			Result<R>(x)
		}
		else this as Result<R>
	}
	
	inline fun <R> safeMapSuccess(code: (T) -> R): Result<R> {
		return if (success) Result{code(any as T)} else this as Result<R>
	}
	
	/** In case of success, returns result of [code] execution. Returns this otherwise. */
	inline fun <R> flatMapSuccess(code: (T) -> Result<R>): Result<R> {
		return if (success) code(any as T) else this as Result<R>
	}
	
	/** Executes [code] in case of a failure with [exception] as the argument. Returns this. */
	inline fun onFailure(code: (Throwable) -> Unit): Result<T> {
		if (!success) code(any as Throwable)
		return this
	}
	
	/** Executes [code] in case of a failure if [Exception] is [F]. Returns this. */
	inline fun <reified F: Throwable> onFailureOf(code: (F) -> Unit): Result<T> {
		if (!success) any?.let { if (it is F) code(it) }
		return this
	}
	
	/** Executes [code] in case of a failure if [Exception] is not [F]. Returns this. */
	inline fun <reified F: Throwable> onFailureOfNot(code: (Throwable) -> Unit): Result<T> {
		if (!success) any?.let { if (it !is F) code(it as Throwable) }
		return this
	}
	
	/** Wraps [exception] into the [Throwable] returned by [code] as its cause. */
	inline fun wrapFailure(code: (Throwable) -> Throwable): Result<T> {
		return if (success) this else (any as Throwable).let { x ->
			Result<T>(code(x).also { if (it.cause == null) it.initCause(x) })
		}
	}
	
	/** If this is a failure, returns the successful [Result] of the [code] execution. Returns this otherwise. */
	inline fun mapFailure(code: (Throwable) -> T): Result<T> {
		return if (success) this else   try {
			Result(code(any as Throwable))
		} catch (x: Throwable) {
			Result<T>(x)
		}
	}
	
	/** If this is a failure, returns the successful [Result] of the [code] execution. Returns this otherwise. */
	inline fun <reified F: Throwable> mapFailureOf(code: (F) -> T): Result<T> {
		return if (success) this else any.let {
			if (it is F) Result(code(it)) else this
		}
	}
	
	/** If this is a failure, returns the successful [Result] of the [code] execution. Returns this otherwise. */
	inline fun <reified F: Throwable> mapFailureOfNot(code: (Throwable) -> T): Result<T> {
		return if (success) this else any.let {
			if (it !is F) Result(code(it as Throwable)) else this
		}
	}
	
	/** In case of failure, returns result of [code] execution. Returns this otherwise. */
	inline fun flatMapFailure(code: (Throwable) -> Result<T>): Result<T> {
		if (!success) return code(any as Throwable)
		return this
	}
	
	/** Returns [value]. Serves destructuring purpose. */
	operator fun component1(): T? = if (success) any as T else null
	
	/** Returns [exception]. Serves destructuring purpose. */
	operator fun component2(): Throwable? = if (success) null else any as Throwable
	
	override fun toString(): String = "Result($any)"
	override fun hashCode(): Int = any?.hashCode() ?: 0
	override fun equals(other: Any?) = this === other || (other is Result<*> && other.any == any)
	
	
	
	
	
	/** Companion object provides [Result] utilities.*/
	companion object {
		private var unit: Result<Unit>? = null
		
		/** Constant shorthand for Result(Unit) object. */
		val ofUnit: Result<Unit> get() = unit ?: Result(Unit).apply { unit = this }
		
		/** Handles an error happened during a [Result] fun 'code' execution. */
		var errorHandler: ((Throwable) -> Unit)? = null
		
		/** Creates a successful [Result] with [value]. Same as calling the constructor with non-[Throwable]. */
		fun <T> Success(value: T): Result<T> = Result(value)
		
		/** Creates a failed [Result] with [exception]. Same as calling the constructor with [Throwable]. */
		fun <T> Failure(exception: Throwable): Result<T> = Result(exception)
	}
}



//
///** Executes [code] in a try/catch block and returns a failed [Result] if an [Result.exception] was thrown, otherwise returns successful [Result] with [Result.value] assigned. An error can be handled with [errorHandler]. */
//inline fun <T> Result(code: () -> T): Result<T> = try {
//	Result(code(), null)
//} catch (x: Throwable) {
//	errorHandler?.invoke(x)
//	Result(x)
//}
//
///** Flattens nested [Result]. */
//@Suppress("UNCHECKED_CAST")
//fun <T> Result<Result<T>>.flatten(): Result<T> = value ?: this as Result<T>
//
//
//
///**
// * The result of some method call provides the caller with a requested [value] if the call is successful or an [exception] if failed.
// * Replaces the usual approach to throw an exception or return the null in case of execution failure.
// */
//
//@Suppress("UNCHECKED_CAST")
//class Result<T> @PublishedApi internal constructor(value: T?, exception: Throwable?) {
//	/** Creates a successful [Result] with [value]*/
//	constructor (value: T): this(value, null)
//
//	/** Creates a failed [Result] with [exception]. */
//	constructor (exception: Throwable): this(null, exception) {
//		success = false
//	}
//
//	@PublishedApi internal var success = true
//	/** A value of a successful call. */
//	val value: T? = value
//	/** exception of a failed call. */
//	val exception: Throwable? = exception
//	/** Indicates of successful result. */
//	val isSuccess: Boolean get() = success
//	/** Indicates of failed result. */
//	val isFailure: Boolean get() = !success
//	/** Returns [value] in case of a success, otherwise throws [exception].*/
//	val valueOrThrow: T get() = if (success) value as T else throw exception!!
//
//	/** Returns [value] in case of a success, otherwise [altValue].*/
//	fun valueOr(altValue: T): T = if (success) value as T else altValue
//
//	/** Returns [value] in case of a success, otherwise a result of the [altFun] call.*/
//	inline fun valueOr(altFun: (Throwable) -> T): T = if (success) value as T else altFun(exception!!)
//
//	/** Executes [code] in case of a success with [value] as the argument. Returns this result. */
//	inline fun onSuccess(code: (T) -> Unit): Result<T> {
//		if (success) code(value as T)
//		return this
//	}
//
//	/** Executes [code] in case of a failure with [exception] as the argument. Returns this result. */
//	inline fun onFailure(code: (Throwable) -> Unit): Result<T> {
//		if (!success) code(exception!!)
//		return this
//	}
//
//	/** Executes [code] in case of a failure if [Exception] is [F]. Returns this result. */
//	inline fun <reified F: Throwable> onFailureOf(code: (F) -> Unit): Result<T> {
//		if (exception is F) code(exception)
//		return this
//	}
//
//	/** Executes [code] in case of a failure if [Exception] is not [F]. Returns this result. */
//	inline fun <reified F: Throwable> onFailureOfNot(code: (Throwable) -> Unit): Result<T> {
//		if (!success && exception !is F) code(exception!!)
//		return this
//	}
//
//	/** If this is failure returns the [Result] of the [code]. Returns this otherwise. */
//	inline fun mapFailure(code: (Throwable) -> Result<T>): Result<T> {
//		return if (success) this else code(exception!!)
//	}
//
//	/** Wraps [exception] into the [Throwable] returned by [code] as its cause. */
//	inline fun wrapFailure(code: (Throwable) -> Throwable): Result<T> {
//		return if (success) this else Result<T>(code(exception!!)).also { if (it.exception!!.cause == null) it.exception.initCause(exception) }
//	}
//
//	/** Returns a result of the execution of [code] with [value] as the argument in case of a success. Otherwise returns 'null'. */
//	inline fun <R> mapSuccess(code: (T) -> R): R? = if (success) code(value as T) else null
//
//	/** Returns a result of the execution of [code] with [exception] as the argument in case of a failure. Otherwise returns 'null'. */
//	inline fun <R> mapFailure(code: (Throwable) -> R): R? = if (success) null else code(exception!!)
//
//	/** Returns [value]. Serves destructuring purpose. */
//	operator fun component1(): T? = value
//
//	/** Returns [exception]. Serves destructuring purpose. */
//	operator fun component2(): Throwable? = exception
//
//	override fun toString(): String = "Result(${if (success) value else exception})"
//	override fun hashCode(): Int = value?.hashCode() ?: exception?.hashCode() ?: 0
//	override fun equals(other: Any?) = this === other || (other is Result<*> && other.exception == exception && other.value == value)
//
//
//
//
//
//	/** Companion object provides [Result] construction.*/
//	companion object {
//		private var unit: Result<Unit>? = null
//
//		/** Constant shorthand for Result(Unit) object. */
//		val ofUnit: Result<Unit> get() = unit ?: Result(Unit).apply { unit = this }
//
//		/** Handles an error happened during a [Result] fun 'code' execution. */
//		var errorHandler: ((Throwable) -> Unit)? = null
//
//		/** Creates a successful [Result] with [value]. Same as calling the constructor with non-[Throwable]. */
//		inline fun <T> Success(value: T): Result<T> = Result(value, null)
//
//		/** Creates a failed [Result] with [exception]. Same as calling the constructor with [Throwable]. */
//		inline fun <T> Failure(exception: Throwable): Result<T> = Result(exception)
//	}
//}
//

