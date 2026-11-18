package com.makemission.ora.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LogPrivacyTest {
    @Test
    fun addressesBecomeDomains() {
        assertEquals("Loaded google.com", LogPrivacy.redact("Loaded https://www.google.com/search?q=secret+words#x"))
        assertEquals("Opened pop-up: ads.example.org", LogPrivacy.redact("Opened pop-up: http://user:pw@ads.example.org:8080/a/b"))
        assertEquals("from en.wikipedia.org: x.com", LogPrivacy.redact("from https://en.wikipedia.org/wiki/Moon: https://x.com/y"))
    }

    @Test
    fun localAndDataAddressesLoseTheirDetail() {
        assertEquals("tab at ora:", LogPrivacy.redact("tab at ora://start"))
        assertEquals("Could not load data:", LogPrivacy.redact("Could not load data:text/html;base64,AAAA"))
    }

    @Test
    fun noPathOrQuerySurvives() {
        val out = LogPrivacy.redact("Loading https://shop.example.com/cart?item=42&user=me")
        assertFalse(out.contains("cart") || out.contains("item") || out.contains("42"))
    }

    @Test
    fun searchLogNamesTheEngineAndNeverTheText() {
        val before = BrowserLog.entries.value.size
        SearchLog.record("Google", privateTab = false)
        val added = BrowserLog.entries.value.drop(before)
        assertEquals(listOf("Searched with Google"), added.map { it.message })
        assertEquals("Searched with DuckDuckGo", SearchLog.line("DuckDuckGo", privateTab = false))
    }

    @Test
    fun privateSearchesLeaveNoLogLine() {
        val before = BrowserLog.entries.value.size
        SearchLog.record("Google", privateTab = true)
        assertEquals(before, BrowserLog.entries.value.size)
        assertEquals(null, SearchLog.line("Google", privateTab = true))
    }

    @Test
    fun diskLinesRoundTripAcrossDays() {
        val e = LogEntry(1_700_000_000_000L, LogLevel.WARN, "page", "line one\nline\ttwo \\ back")
        assertEquals(e, LogCodec.decode(LogCodec.encode(e)))
        assertEquals(null, LogCodec.decode("garbage"))
    }
}
