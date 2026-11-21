package com.makemission.ora.settings

import com.makemission.ora.bookmarks.Bookmark
import com.makemission.ora.history.HistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartPageConfigTest {
    @Test fun slotsRoundTrip() {
        val moved = movedSlot(StartPageConfig.DEFAULT_SLOTS, 3, -2)
        assertEquals(moved, parseSlots(encodeSlots(moved)))
    }

    @Test fun oldOrBrokenSavesKeepEverySection() {
        val slots = parseSlots("search:0,bogus:1,search:1")
        assertEquals(StartSection.entries.size, slots.size)
        assertEquals(StartSection.SEARCH, slots.first().section)
        assertEquals(false, slots.first().enabled)
        assertEquals(StartPageConfig.DEFAULT_SLOTS, parseSlots(null))
    }

    @Test fun movingPastTheEndsChangesNothing() {
        val s = StartPageConfig.DEFAULT_SLOTS
        assertEquals(s, movedSlot(s, 0, -1))
        assertEquals(s, movedSlot(s, s.lastIndex, 1))
    }

    @Test fun shownFollowsOrderAndSwitches() {
        val s = withSlotEnabled(movedSlot(StartPageConfig.DEFAULT_SLOTS, 2, -1), StartSection.SEARCH, false)
        assertEquals(listOf(StartSection.SHORTCUTS, StartSection.STATUS), StartPageConfig(slots = s).shown)
    }

    @Test fun greetingByHour() {
        assertEquals("Good morning", greetingFor(8))
        assertEquals("Good afternoon", greetingFor(13))
        assertEquals("Good evening", greetingFor(19))
        assertEquals("Good night", greetingFor(2))
    }

    @Test fun mostVisitedOnePerSiteWebOnly() {
        val now = 1_000_000_000_000L
        val h = listOf(
            HistoryEntry("https://a.com/x", "A one", now, 9),
            HistoryEntry("https://www.a.com/y", "A two", now, 3),
            HistoryEntry("about:home", "Home", now, 50),
            HistoryEntry("https://b.org", "", now, 5),
        )
        val out = mostVisited(h, 5, now)
        assertEquals(listOf("https://a.com/x", "https://b.org"), out.map { it.url })
        assertEquals("b.org", out[1].title)
    }

    @Test fun pinnedAddressChecks() {
        assertEquals("https://example.com", normalizePinnedUrl(" example.com "))
        assertNull(normalizePinnedUrl("two words"))
        assertNull(normalizePinnedUrl("nodots"))
        assertNull(normalizePinnedUrl("javascript:alert(1)"))
    }

    @Test fun pinnedRoundTripAndNoRepeats() {
        val list = withPinned(withPinned(emptyList(), "https://a.com", "A\tb"), "https://A.com", "again")
        assertEquals(1, list.size)
        assertEquals(list, parsePinned(encodePinned(list)))
    }
}
