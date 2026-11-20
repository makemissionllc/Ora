package com.makemission.ora.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrecencyTest {
    private val now = 1_000_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    @Test fun moreVisitsRankHigher() {
        val few = HistoryEntry("https://a.com/", "a", now, 1)
        val many = HistoryEntry("https://b.com/", "b", now, 20)
        assertTrue(many.frecency(now) > few.frecency(now))
    }

    @Test fun recentBeatsOldAtEqualVisits() {
        val recent = HistoryEntry("https://a.com/", "a", now - day / 2, 5)
        val old = HistoryEntry("https://b.com/", "b", now - 200 * day, 5)
        assertTrue(recent.frecency(now) > old.frecency(now))
    }

    @Test fun topSitesKeepsOnePerSiteBestFirst() {
        val list = listOf(
            HistoryEntry("https://a.com/x", "x", now, 1),
            HistoryEntry("https://a.com/y", "y", now, 9),
            HistoryEntry("https://b.com/", "b", now, 3),
        )
        val top = list.topSites(5, now)
        assertEquals(listOf("https://a.com/y", "https://b.com/"), top.map { it.url })
    }
}
