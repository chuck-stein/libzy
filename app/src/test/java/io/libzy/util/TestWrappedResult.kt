package io.libzy.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Tests for custom functional error handling utilities using [WrappedResult].
 */
class TestWrappedResult {

    @Test
    fun `test wrapResult success`() {
        val result = wrapResult { 4 }.unwrap()
        assertEquals(4, result)
    }

    @Test
    fun `test wrapResult success unused handler`() {
        val result = wrapResult { 2 }
            .handle(Exception::class) { 3 }
            .unwrap()
        assertEquals(2, result)
    }

    @Test
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun `test wrapResult handled failure`() {
        val result = wrapResult { 1 as String }
            .handle(IllegalArgumentException::class) { "2" } // unused
            .handle(ClassCastException::class) { "3" } // used
            .handle(IllegalStateException::class) { "4" } // unused
            .handle(RuntimeException::class) { "5"} // unused because another handler was already called
            .handle(ClassCastException::class) { "6" } // unused because another handler was already called
            .handle(IndexOutOfBoundsException::class) { "7" } // unused
            .unwrap()
        assertEquals("3", result)
    }

    @Test
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun `test wrapResult unhandled failure`() {
        assertThrows(ClassCastException::class.java) {
            wrapResult { 2 as String }
                .handle(IllegalArgumentException::class) { "handled?" } // unused
                .unwrap()
        }
    }

    @Test
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun `test wrapResult unhandled failure never unwrapped`() {
        wrapResult { 2 as String } // ClassCastException is wrapped and not rethrown, so test should pass
    }
}
