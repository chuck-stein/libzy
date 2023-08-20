package io.libzy.util

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Represents the result of running a block of code which may have thrown an exception.
 *
 * Allows for functional error handling via functions such as [wrapResult], [handle], and [unwrap].
 * Advantages over Kotlin's built-in functional error handling (via [runCatching], [onSuccess], and [onFailure])
 * include the ability to handle specific exception types individually (rather than all [Throwable]s at once),
 * and the ability to treat the entire error handling chain as an expression (similar to try-catch).
 */
sealed interface WrappedResult<RESULT> {

    /**
     * Represents successful execution of a block of code which may have thrown an exception,
     * but did not, and returned [wrappedValue].
     *
     * The [wrappedValue] will be returned when this [WrappedResult] is [unwrap]ped.
     */
    data class Success<RESULT>(val wrappedValue: RESULT) : WrappedResult<RESULT>

    /**
     * Represents failed execution of a block of code, which threw [wrappedException].
     *
     * The [wrappedException] will be rethrown when this [WrappedResult] is [unwrap]ped,
     * unless the [Failure] is first converted to a [HandledFailure] via [handle].
     */
    data class Failure<RESULT>(val wrappedException: Exception) : WrappedResult<RESULT> {

        /**
         * Create a [HandledFailure] which represents successful error handling of this [Failure].
         *
         * @param handlerResult The output of the error handler, which will be returned
         * when the [HandledFailure] is [unwrap]ped.
         */
        fun toHandledFailure(handlerResult: RESULT) = HandledFailure(wrappedException, handlerResult)
    }

    /**
     * Represents failed execution of a block of code, which threw [wrappedException],
     * but was handled by an error handler which returned [handlerResult].
     *
     * The [handlerResult] will be returned when this [WrappedResult] is [unwrap]ped.
     */
    data class HandledFailure<RESULT>(val wrappedException: Exception, val handlerResult: RESULT) : WrappedResult<RESULT>
}

/**
 * Kicks off a chain of functional error handling, by running the [getResult] block and returning a [WrappedResult]
 * which wraps either the return value of the block, or an exception that it threw.
 *
 * Any possible exceptions can be handled by one or more subsequent calls to [handle] on the returned [WrappedResult],
 * before finally ending the call chain with [unwrap], which will also rethrow any unhandled exceptions.
 *
 * @param getResult Analogous to a try block.
 */
inline fun <RESULT> wrapResult(getResult: () -> RESULT): WrappedResult<RESULT> {
    return try {
        WrappedResult.Success(getResult())
    } catch (e: Exception) {
        WrappedResult.Failure(e)
    }
}

/**
 * Attaches an [exceptionHandler] to a chain of functional error handling.
 *
 * If the receiving [WrappedResult] represents a failure which wraps an exception of type [exceptionType],
 * then the [exceptionHandler] will be called with the wrapped exception, and this function will return a new
 * [WrappedResult] which will no longer rethrow the wrapped exception once [unwrap]ped.
 * Instead, once unwrapped, it will return the output of the [exceptionHandler].
 *
 * If the receiving [WrappedResult] does not wrap an exception of type [exceptionType],
 * then this function will return that [WrappedResult] unchanged.
 *
 * @param exceptionType The type of exception to handle, as a [KClass] instead of a type parameter, because otherwise
 *                      the other type parameters would be have to be specified explicitly as well, hurting readability.
 * @param exceptionHandler Analogous to a catch block.
 */
inline fun <RESULT, reified E : Exception> WrappedResult<RESULT>.handle(
    exceptionType: KClass<E>,
    exceptionHandler: (exception: E) -> RESULT
): WrappedResult<RESULT> {
    if (this is WrappedResult.Failure && wrappedException is E) {
        return toHandledFailure(exceptionHandler(wrappedException))
    }
    return this
}

/**
 * Attaches an [exceptionHandler] to a chain of functional error handling.
 *
 * If the receiving [WrappedResult] represents a failure which wraps an exception of any of the given [exceptionTypes],
 * then the [exceptionHandler] will be called with the wrapped exception, and this function will return a new
 * [WrappedResult] which will no longer rethrow the wrapped exception once [unwrap]ped.
 * Instead, once unwrapped, it will return the output of the [exceptionHandler].
 *
 * If the receiving [WrappedResult] does not wrap an exception of one of the given [exceptionTypes],
 * then this function will return that [WrappedResult] unchanged.
 *
 * @param exceptionTypes The types of exception to handle.
 * @param exceptionHandler Analogous to a catch block.
 */
inline fun <RESULT> WrappedResult<RESULT>.handleAny(
    vararg exceptionTypes: KClass<*>,
    exceptionHandler: (exception: Exception) -> RESULT
): WrappedResult<RESULT> {
    if (this is WrappedResult.Failure && exceptionTypes.any { wrappedException::class.isSubclassOf(it) }) {
        return toHandledFailure(exceptionHandler(wrappedException))
    }
    return this
}

/**
 * Completes a chain of functional error handling, by unwrapping the receiving [WrappedResult].
 *
 * The [WrappedResult] may be wrapping a value of type [RESULT] if the operation was a success,
 * in which case unwrapping it will return that [RESULT].
 *
 * Alternatively, the [WrappedResult] may be wrapping an exception that was thrown but handled,
 * in which case unwrapping it will return the output of the exception handler which ran earlier in the call chain.
 *
 * Otherwise, the [WrappedResult] will be wrapping an unhandled exception,
 * in which case unwrapping it will rethrow that exception.
 */
fun <RESULT> WrappedResult<RESULT>.unwrap(): RESULT {
    return when (this) {
        is WrappedResult.Success -> wrappedValue
        is WrappedResult.Failure -> throw wrappedException
        is WrappedResult.HandledFailure -> handlerResult
    }
}
