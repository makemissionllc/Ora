package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickSwitcherTest {
    private val items = listOf(
        QuickItem(QuickKind.TAB, "1", "Example Domain", "example.com"),
        QuickItem(QuickKind.BOOKMARK, "https://example.com", "Example Domain", "example.com"),
        QuickItem(QuickKind.BOOKMARK, "https://news.org", "Daily news", "news.org"),
        QuickItem(QuickKind.HISTORY, "https://exam.edu", "Exam results", "exam.edu"),
        QuickItem(QuickKind.ACTION, "Settings", "Settings"),
    )

    @Test fun emptyQueryListsTabsAndActions() {
        assertEquals(listOf("1", "Settings"), quickMatches("", items).map { it.key })
    }

    @Test fun titlePrefixBeatsAddressMatchAndOpenTabIsNotListedTwice() {
        assertEquals(listOf("1", "https://exam.edu"), quickMatches("exam", items).map { it.key })
    }

    @Test fun matchesAddressesAndIgnoresCase() {
        assertEquals(listOf("https://news.org"), quickMatches("NEWS.ORG", items).map { it.key })
    }

    @Test fun nothingMatchesGivesEmptyList() {
        assertEquals(emptyList<QuickItem>(), quickMatches("zzz", items))
    }
}
