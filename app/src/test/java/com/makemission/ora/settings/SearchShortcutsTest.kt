package com.makemission.ora.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchShortcutsTest {
    @Test fun keywordThenQueryResolves() {
        val m = SearchShortcuts.resolve("yt funny cats", emptyList(), true)!!
        assertEquals("YouTube", m.shortcut.label)
        assertEquals("funny cats", m.query)
        assertEquals("https://www.youtube.com/results?search_query=funny+cats", m.shortcut.urlFor(m.query))
    }

    @Test fun needsAQueryAndAKnownKeyword() {
        assertNull(SearchShortcuts.resolve("yt", emptyList(), true))
        assertNull(SearchShortcuts.resolve("yt   ", emptyList(), true))
        assertNull(SearchShortcuts.resolve("zzz cats", emptyList(), true))
    }

    @Test fun offMeansNothingResolves() = assertNull(SearchShortcuts.resolve("yt cats", emptyList(), false))

    @Test fun ownShortcutBeatsBuiltIn() {
        val mine = SearchShortcut("yt", "Mine", "https://example.com/?q=%s")
        assertEquals("Mine", SearchShortcuts.resolve("YT cats", listOf(mine), true)!!.shortcut.label)
    }

    @Test fun validation() {
        assertTrue(SearchShortcuts.validKeyword("docs"))
        assertFalse(SearchShortcuts.validKeyword("a b"))
        assertFalse(SearchShortcuts.validKeyword("a.b"))
        assertTrue(SearchShortcuts.validTemplate("https://x.org/s?q=%s"))
        assertFalse(SearchShortcuts.validTemplate("https://x.org/s"))
        assertFalse(SearchShortcuts.validTemplate("https://x.org/%s/%s"))
        assertFalse(SearchShortcuts.validTemplate("ftp://x.org/%s"))
    }
}
