package com.makemission.ora.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadableFailureTest {
    private val fallback = "Could not install this extension."

    @Test
    fun aRealMessageIsShownAsItIs() {
        assertEquals("Invalid add-on ID", readableFailure("Invalid add-on ID", fallback))
    }

    @Test
    fun anEmptyOrNullMessageGetsTheFallbackAndAHint() {
        val blank = readableFailure("", fallback)
        assertTrue(blank.startsWith(fallback))
        assertTrue(blank.contains("Check the address"))
        assertEquals(blank, readableFailure("   ", fallback))
        assertEquals(blank, readableFailure(null, fallback))
    }
}
