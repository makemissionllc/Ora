package com.makemission.ora.extensions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderReportTest {
    @Test
    fun aFirstReportIsLogged() {
        assertTrue(readerReportChanged(previous = null, readable = false))
    }

    @Test
    fun aRepeatOfTheSameReportIsNotLogged() {
        assertFalse(readerReportChanged(previous = true, readable = true))
        assertFalse(readerReportChanged(previous = false, readable = false))
    }

    @Test
    fun aChangedReportIsLogged() {
        assertTrue(readerReportChanged(previous = true, readable = false))
    }
}
