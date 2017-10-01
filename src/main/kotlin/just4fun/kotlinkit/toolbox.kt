package just4fun.kotlinkit

import kotlin.reflect.KProperty

/*Lazy var delegate */

fun <T:Any> lazyVar(default: () -> T): LazyVarDelegate<T> = LazyVarDelegate(default)

class LazyVarDelegate<T:Any>(default: () -> T) /*: ReadWriteProperty<Any, T>*/ {
	private var default: (() -> T)? = default
	private var value: T? = null
	
	operator fun getValue(thisRef: Any, property: KProperty<*>): T {
		return value ?: synchronized(this) {
			if (value == null) {
				value = default!!()
				default = null
			}
			value!!
		}
	}
	
	operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
		this.value = value
	}
}
