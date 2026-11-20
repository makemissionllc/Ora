package com.makemission.ora.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedSessionsTest {
    @Test fun savesNoPrivateStartOrDuplicatePages() {
        val pages = SessionLogic.pagesToSave(listOf(
            Triple("https://a.org/", "A", false), Triple("https://a.org/", "A again", false),
            Triple("https://secret.org/", "S", true), Triple("ora://start", "", false), Triple("https://b.org/", "B", false),
        ))
        assertEquals(listOf("https://a.org/", "https://b.org/"), pages.map { it.url })
    }

    @Test fun openingSkipsPagesAlreadyOpen() {
        val s = SavedSession("x", 0, listOf(SessionPage("https://a.org/", "A"), SessionPage("https://b.org/", "B")))
        val plan = SessionLogic.planOpen(s, setOf("https://a.org/"))
        assertEquals(listOf("https://b.org/"), plan.toOpen.map { it.url })
        assertEquals(1, plan.alreadyOpen)
    }

    @Test fun sameNameReplacesAndNewestIsFirst() {
        val a = SavedSession("Trip", 1, listOf(SessionPage("https://a.org/", "")))
        val b = SavedSession("Work", 2, listOf(SessionPage("https://b.org/", "")))
        val again = SavedSession("trip", 3, listOf(SessionPage("https://c.org/", "")))
        val list = SessionLogic.withSession(SessionLogic.withSession(listOf(a), b), again)
        assertEquals(listOf("trip", "Work"), list.map { it.name })
    }

    @Test fun capsTheList() {
        var list = emptyList<SavedSession>()
        for (i in 1..25) list = SessionLogic.withSession(list, SavedSession("s$i", i.toLong(), emptyList()))
        assertEquals(SessionLogic.MAX_SESSIONS, list.size)
        assertEquals("s25", list.first().name)
    }

    @Test fun defaultNameUsesTheFirstSite() {
        assertTrue(SessionLogic.defaultName(listOf(SessionPage("https://www.example.com/x", "")), 0L).startsWith("example.com, "))
        assertTrue(SessionLogic.defaultName(emptyList(), 0L).startsWith("Session, "))
    }
}
