# kotlinkit
Kotlin utilities: `Result` wrapper, `AsyncResult`, `AsyncTask`, `SuspendTask`, etc

## Result

The `Result<T>` class represents a result of some method call and provides the caller with a requested value of type `T` or an exception, in case if something's gone awry.     
 Returning a `Result` from a method that can fail, replaces the usual approach to throw an exception or return the null in case of an execution failure.  
 
 - Construction:  
 ```kotlin
	// from execution of a code block
	val result1 = Result { 10 / 10 }
	val result2 = Result { 10 / 0 }
	
	// from constructor
	fun devide(a: Int, b: Int): Result<Int> = if (b != 0) Result(a / b) else Result(ArithmeticException())
	val result31: Result<Int> = devide(10, 0) // failure with exception: ArithmeticException
	val result32: Result<Int> = devide(10, 10) // Result(1)
	
	// from companion's functions
	val result4: Result<String> = Result.Success("ok") // value = "ok"
	val result7: Result<Int> = Result.Failure(ArithmeticException())  // value is null; exception is ArithmeticException
	val result5: Result<Exception> = Result.Success(Exception())  // value is Exception
```  
 - Destructuring:  
 ```kotlin
	val (value, exception) = Result("ok") // value = "ok"; exception = null
	
	Result { 10 / 0 }.let { (value, exception) -> println("v=$value;  e=$exception") } // v=null; e=ArithmeticException
```  
- Simple usage  
 ```kotlin
	val okay = Result { 10 / 10 } // Result(1)
	val oops = Result { 10 / 0 }  // failed Result with exception: ArithmeticException
	
	val s0 = okay.value ?: 0 + 1 // = 2
	val f0 = oops.value ?: 0 + 1 // = 1
	
	val s1 = okay.valueOrThrow + 1 // = 2
// val f1 = oops.valueOrThrow + 1 // throws ArithmeticException

	val s2 = okay.value!! + 1 // = 2
// val f2 = oops.value!! + 1 // throws NullPointerException

	val s3 = okay.valueOr(0) + 1 // = 2
	val f3 = oops.valueOr(0) + 1 // = 1
	
	val s4 = okay.valueOr { if (it is ArithmeticException) 0 else 1 } + 1 // = 2
	val f4 = oops.valueOr { if (it is ArithmeticException) 0 else 1 } + 1 // = 1
	
	val e0 = okay.exception // is null
	val e1 = oops.exception // is ArithmeticException
	
	val b0 = if (okay.isSuccess) 1 else 0 // = 1
	val b1 = if (oops.isFailure) 1 else 0 // = 1
```  
- Hooks
 ```kotlin
	okay.onSuccess { println("success") }.onFailure { println("failure 1") } // prints "success"
	oops.onSuccess { println("success") }.onFailure { println("failure 2") } // prints "failure 2"
	
	oops.onFailureOf<ArithmeticException> { println("failure 3") } // prints "failure 3"
	oops.onFailureOfNot<ArithmeticException> { println("failure 4") } // prints nothing
	oops.onFailureOf<Exception> { println("failure 5") } // prints "failure 5"
	oops.onFailureOfNot<NullPointerException> { println("failure 6") } // prints "failure 6"
```  
- Transformations  
 ```kotlin
	val w0 = oops.wrapFailure { IllegalStateException() } // exception is IllegalStateException with cause: ArithmeticException
	
	/* Value transformation */
	
	val t0 = okay.ifFailure { 0 } // Result(2)
	val t1 = oops.ifFailure { 0 } // Result(0)
	val t11 = oops.ifFailureOf<ArithmeticException> { 0 } // Result(0)
	val t12 = oops.ifFailureOfNot<NullPointerException> { 0 } // Result(0)
	
	// Chaining
	val t2 = okay.ifSuccess { "ok" }.ifFailure { "oops" } // Result("ok")
	val t3 = oops.ifSuccess { "ok" }.ifFailure { "oops" } // Result("oops")
	
	/* Result transformation */
	
	val t4 = okay.fromSuccess {
		if (it == 0) Result("ok") else Result(Exception("oops"))
	}
	// t4 is failed Result<String>
	
	val t5 = okay.fromSuccess {
		if (it == 1) Result("ok") else Result(Exception("oops"))
	}
	// t5 is Result("ok")
	
	// Chaining
	val t6 = oops.fromSuccess { Result("ok") }.fromFailure {
		if (it is ArithmeticException) Result("wrong") else Result(Exception("oops"))
	}
	// t6 is Result("wrong")
	
	val t7 = oops.fromSuccess { Result("ok") }.fromFailure {
		if (it is ArithmeticException) Result(Exception("oops")) else Result("wrong")
	}
	// t6 is failed Result<String>
```  




## AsyncResult
The `AsyncResult` is an interface designed to represent a `Result` that may not be currently available but can be obtained on completion of some async execution via the`onComplete` callback.   

The following are implementations:   
- `AsyncTask`:  asynchronously executes code block ([see description below](#asynctask));
- `SuspendTask`:  executes suspension code block ([see description below](#suspendtask));
- `ThreadTask`: asynchronously executes code block in separate thread;
- `ReadyAsyncResult`: already complete result.  

Usage example:  
```kotlin
ThreadTask { Thread.sleep(1000); "ok" }.onComplete { println("v=${it.value};  e=${it.exception}") }
```


#### AsyncTask

An `AsyncResult` of the code block execution.
- Can be scheduled by some delay.
- Can run in an executor thread if one is specified. Otherwise runs in the `AsyncTask.sharedContext`.  
 
Usage example:  
```kotlin
AsyncTask(1000) {"ok"}.onComplete { println("v=${it.value};  e=${it.exception}") }
```

#### SuspendTask

An `AsyncResult` of suspension code block execution which is performed in a context of the coroutine.  
- Can specify `CoroutineContext`
- Can specify code block receiver  

Usage example:  
```kotlin
suspend fun longRun(): String { Thread.sleep(1000); return "ok" }

// async execution
val result: AsyncResult<String> = SuspendTask.async { longRun() }
result.onComplete { println("v=${it.value};  e=${it.exception}") }

// suspended execution
val result: Result<String> = SuspendTask { longRun() }
val value = result.valueOr("oops") // = "ok"
```




## Installation

Gradle dependency:   
```
compile 'com.github.just-4-fun:kotlinkit:0.4'
```

Maven dependency:  
```xml
<dependency>
  <groupId>com.github.just-4-fun</groupId>
  <artifactId>kotlinkit</artifactId>
  <version>0.4</version>
  <type>pom</type>
</dependency>
```
 