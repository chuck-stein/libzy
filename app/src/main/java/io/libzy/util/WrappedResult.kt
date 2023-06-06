package io.libzy.util

import kotlin.reflect.KClass

/**
 * Represents the result of running a block of code which may have thrown an exception.
 * The [RESULT] type is the type returned by the block of code.
 * The [OUTPUT] type is the type that the error handling chain should return.
 *
 * Allows for functional error handling via functions such as [wrapResult], [handle], and [unwrap].
 * Advantages over Kotlin's built-in functional error handling (via [runCatching], [onSuccess], and [onFailure])
 * include the ability to handle specific exception types individually (rather than all [Throwable]s at once),
 * and the ability to treat the entire error handling chain as an expression (similar to try-catch).
 */
sealed interface WrappedResult<RESULT, OUTPUT> {

    /**
     * Represents successful execution of a block of code which may have thrown an exception,
     * but did not, and returned [wrappedValue].
     *
     * The [wrappedValue] will be returned when this [WrappedResult] is [unwrap]ped.
     */
    data class Success<RESULT, OUTPUT>(val wrappedValue: RESULT) : WrappedResult<RESULT, OUTPUT>

    /**
     * Represents failed execution of a block of code, which threw [wrappedException].
     *
     * The [wrappedException] will be rethrown when this [WrappedResult] is [unwrap]ped,
     * unless the [Failure] is first converted to a [HandledFailure] via [handle].
     */
    data class Failure<RESULT, OUTPUT>(val wrappedException: Exception) : WrappedResult<RESULT, OUTPUT> {

        /**
         * Create a [HandledFailure] which represents successful error handling of this [Failure].
         *
         * @param handlerOutput The output of the error handler, which will be returned
         *                      when the [HandledFailure] is [unwrap]ped.
         */
        fun toHandledFailure(handlerOutput: OUTPUT) = HandledFailure<RESULT, OUTPUT>(wrappedException, handlerOutput)
    }

    /**
     * Represents failed execution of a block of code, which threw [wrappedException],
     * but was handled by an error handler which returned [handlerOutput].
     *
     * The [handlerOutput] will be returned when this [WrappedResult] is [unwrap]ped.
     */
    data class HandledFailure<RESULT, OUTPUT>(val wrappedException: Exception, val handlerOutput: OUTPUT) :
        WrappedResult<RESULT, OUTPUT>
}

/**
 * Kicks off a chain of functional error handling, by running the [getResult] block and returning a [WrappedResult]
 * which wraps either the return value of the block, or an exception that it threw.
 *
 * Any possible exceptions can be handled by one or more subsequent calls to [handle] on the returned [WrappedResult],
 * before finally ending the call chain with [unwrap], which will also rethrow any unhandled exceptions.
 *
 * This particular [wrapResult] variant allows for specifying an [OUTPUT] type which may be different from
 * the [RESULT] type. This [OUTPUT] type will be the type returned from the error handling chain once [unwrap]ped.
 * The [OUTPUT] value will either come from an error handler passed into [handle] which was executed,
 * or a block passed into [unwrap] to transform a successful [RESULT] into the [OUTPUT] type.
 *
 * @param getResult Analogous to a try block, except it can be limited to contain only the code which may throw an
 *                  exception, to more clearly indicate where such code is. Any code which should not throw an
 *                  exception, but depends on the code that might, can then be put in the block passed into [unwrap],
 *                  whereas with a traditional try-catch it might need to go at the end of the same try block.
 */
inline fun <RESULT, OUTPUT> wrapIntermediateResult(getResult: () -> RESULT): WrappedResult<RESULT, OUTPUT> {
    return try {
        WrappedResult.Success(getResult())
    } catch (e: Exception) {
        WrappedResult.Failure(e)
    }
}

/**
 * Kicks off a chain of functional error handling, by running the [getResult] block and returning a [WrappedResult]
 * which wraps either the return value of the block, or an exception that it threw.
 *
 * Any possible exceptions can be handled by one or more subsequent calls to [handle] on the returned [WrappedResult],
 * before finally ending the call chain with [unwrap], which will also rethrow any unhandled exceptions.
 *
 * This particular [wrapResult] variant specifies that the type returned from [getResult] is the same type that should
 * be returned from the error handling chain. Additionally, the chain can be ended with the [unwrap] overload which
 * takes no parameters, in order to directly return the [RESULT] from [getResult] (or the result of any triggered
 * error handlers) as the return value of the error handling chain.
 *
 * @param getResult Analogous to a try block, except it can be limited to contain only the code which may throw an
 *                  exception, to more clearly indicate where such code is. Any code which should not throw an
 *                  exception, but depends on the code that might, can then be put in the block passed into [unwrap],
 *                  whereas with a traditional try-catch it might need to go at the end of the same try block.
 */
inline fun <RESULT> wrapResultForOutput(getResult: () -> RESULT) = wrapIntermediateResult<RESULT, RESULT>(getResult)

/**
 * Kicks off a chain of functional error handling, by running the [getResult] block and returning a [WrappedResult]
 * which wraps either the return value of the block, or an exception that it threw.
 *
 * Any possible exceptions can be handled by one or more subsequent calls to [handle] on the returned [WrappedResult],
 * before finally ending the call chain with [unwrap], which will also rethrow any unhandled exceptions.
 *
 * This particular [wrapResult] variant specifies that the error handling chain is not being used as an expression,
 * so the chain will return [Unit] once [unwrap]ped, and the error handlers passed into [handle] may also return [Unit].
 *
 * @param getResult Analogous to a try block, except it can be limited to contain only the code which may throw an
 *                  exception, to more clearly indicate where such code is. Any code which should not throw an
 *                  exception, but depends on the code that might, can then be put in the block passed into [unwrap],
 *                  whereas with a traditional try-catch it might need to go at the end of the same try block.
 */
inline fun <RESULT> wrapResult(getResult: () -> RESULT) = wrapIntermediateResult<RESULT, Unit>(getResult)

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
inline fun <RESULT, OUTPUT, reified E : Exception> WrappedResult<RESULT, OUTPUT>.handle(
    exceptionType: KClass<E>,
    exceptionHandler: (exception: E) -> OUTPUT
): WrappedResult<RESULT, OUTPUT> {
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
inline fun <RESULT, OUTPUT> WrappedResult<RESULT, OUTPUT>.handleAny(
    vararg exceptionTypes: KClass<*>,
    exceptionHandler: (exception: Exception) -> OUTPUT
): WrappedResult<RESULT, OUTPUT> {
    if (this is WrappedResult.Failure && exceptionTypes.any { wrappedException::class == it }) {
        return toHandledFailure(exceptionHandler(wrappedException))
    }
    return this
}

/**
 * Completes a chain of functional error handling, by unwrapping the receiving [WrappedResult].
 *
 * The [WrappedResult] may be wrapping a value of type [RESULT] if the operation was a success, in which case
 * unwrapping it will pass that [RESULT] into the given [resolveOutput] block and return that block's [OUTPUT].
 *
 * Alternatively, the [WrappedResult] may be wrapping an exception that was thrown but handled,
 * in which case unwrapping it will return the [OUTPUT] of the exception handler from earlier in the call chain.
 *
 * Otherwise, the [WrappedResult] will be wrapping an unhandled exception,
 * in which case unwrapping it will rethrow that exception.
 */
inline fun <RESULT, OUTPUT> WrappedResult<RESULT, OUTPUT>.unwrap(resolveOutput: (value: RESULT) -> OUTPUT): OUTPUT {
    return when (this) {
        is WrappedResult.Success -> resolveOutput(wrappedValue)
        is WrappedResult.Failure -> throw wrappedException
        is WrappedResult.HandledFailure -> handlerOutput
    }
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
fun <RESULT> WrappedResult<RESULT, RESULT>.unwrap(): RESULT {
    return when (this) {
        is WrappedResult.Success -> wrappedValue
        is WrappedResult.Failure -> throw wrappedException
        is WrappedResult.HandledFailure -> handlerOutput
    }
}
