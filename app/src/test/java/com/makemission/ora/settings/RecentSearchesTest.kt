package com.makemission.ora.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentSearchesTest {
    @Test fun newestFirstWithoutRepeats() {
        val list = RecentSearches.withSearch(listOf("a", "b"), "B ")
        assertEquals(listOf("B", "a"), list)
    }

    @Test fun blankChangesNothing() {
        assertEquals(listOf("a"), RecentSearches.withSearch(listOf("a"), "  "))
    }

    @Test fun capped() {
        val full = (1..RecentSearches.MAX).map { "q$it" }
        assertEquals(RecentSearches.MAX, RecentSearches.withSearch(full, "new").size)
    }
}
