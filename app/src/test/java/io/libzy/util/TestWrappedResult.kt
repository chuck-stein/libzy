package io.libzy.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for custom functional error handling utilities using [WrappedResult].
 */
class TestWrappedResult {

    @Test
    fun `test wrapResult success`() {
        var sum = 2
        wrapResult { 2 }.unwrap { sum += it }
        assertEquals(4, sum)
    }

    @Test
    fun `test wrapResult success unused handler`() {
        var sum = 2
        wrapResult { 2 }
            .handle(Exception::class) { sum += 5 }
            .unwrap { sum += it }
        assertEquals(4, sum)
    }

    @Test
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun `test wrapResult handled failure`() {
        var sum = 2
        wrapResult { 2 as String }
            .handle(IllegalArgumentException::class) { sum += 5 } // unused
            .handle(ClassCastException::class) { sum += 7 } // used
            .handle(IllegalStateException::class) { sum += 5 } // unused
            .handle(RuntimeException::class) { sum += 5 } // unused because another handler was already called
            .handle(ClassCastException::class) { sum += 5 } // unused because another handler was already called
            .handle(IndexOutOfBoundsException::class) { sum += 5 } // unused
            .unwrap { sum += it.toInt() }
        assertEquals(9, sum)
    }

    @Test
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun `test wrapResult unhandled failure`() {
        assertThrows(ClassCastException::class.java) {
            var sum = 2
            wrapResult { 2 as String }
                .handle(IllegalArgumentException::class) { sum += 5 } // unused
                .unwrap { sum += it.toInt() }
        }
    }

    @Test
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun `test wrapResult unhandled failure never unwrapped`() {
        wrapResult { 2 as String } // ClassCastException is wrapped and not rethrown, so test should pass
    }

    @Test
    fun `test wrapResultForOutput success`() {
        val output = wrapResultForOutput { true }.unwrap()
        assertTrue(output)
    }

    @Test
    fun `test wrapResultForOutput success unused handler`() {
        val output = wrapResultForOutput { true }
            .handle(IndexOutOfBoundsException::class) { false }
            .unwrap()
        assertTrue(output)
    }

    @Test
    @Suppress("SimplifyBooleanWithConstants")
    fun `test wrapResultForOutput handled failure`() {
        val output = wrapResultForOutput {
            val alwaysFalsePredicate = 1 > 2
            "success".takeIf { alwaysFalsePredicate } ?: throw RuntimeException("the predicate was false!")
        }.handle(RuntimeException::class) {
            it.message ?: "failure"
        }.unwrap()
        assertEquals("the predicate was false!", output)
    }

    @Test
    @Suppress("SimplifyBooleanWithConstants")
    fun `test wrapResultForOutput unhandled failure`() {
        assertThrows(IllegalStateException::class.java) {
            wrapResultForOutput {
                val alwaysFalsePredicate = 1 > 2
                "success".takeIf { alwaysFalsePredicate } ?: throw IllegalStateException()
            }.handle(IllegalArgumentException::class) { // unused
                "handled failure 1"
            }.handle(ArithmeticException::class) { // unused
                "handled failure 2"
            }.unwrap()
        }
    }

    @Test
    fun `test wrapIntermediateResult success`() {
        val output = wrapIntermediateResult<Int, String> { 2 }.unwrap { it.toString() }
        assertEquals("2", output)
    }

    @Test
    fun `test wrapIntermediateResult success unused handler`() {
        val output = wrapIntermediateResult<Int, String> {
            2
        }.handle(RuntimeException::class) {
            "operation failed"
        }.unwrap {
            it.toString()
        }
        assertEquals("2", output)
    }

    @Test
    @Suppress("DIVISION_BY_ZERO")
    fun `test wrapIntermediateResult handled failure`() {
        val output = wrapIntermediateResult<Int, String> {
            2 / 0
        }.handle(IllegalArgumentException::class) { // unused
            "looks like we got an IllegalArgumentException caused by ${it.cause}"
        }.handle(IllegalStateException::class) { // unused
            "looks like we got an IllegalStateException caused by ${it.cause}"
        }.handle(IndexOutOfBoundsException::class) { // unused
            "looks like we got an IndexOutOfBoundsException caused by ${it.cause}"
        }.handle(ArithmeticException::class) { // used
            "you can't divide by zero, silly"
        }.unwrap {
            it.toString()
        }
        assertEquals("you can't divide by zero, silly", output)
    }

    @Test
    @Suppress("DIVISION_BY_ZERO")
    fun `test wrapIntermediateResult unhandled failure`() {
        assertThrows(ArithmeticException::class.java) {
            wrapIntermediateResult<Int, String> {
                2 / 0
            }.unwrap {
                it.toString()
            }
        }
    }
}
