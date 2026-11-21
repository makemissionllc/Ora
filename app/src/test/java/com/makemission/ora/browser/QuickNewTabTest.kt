package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickNewTabTest {
    private val groups = listOf("a" to "Work", "b" to "Play", "c" to "Read", "d" to "Extra")

    @Test fun normalSpaceListsEverythingInOrder() {
        val ids = quickEntries(false, "example.com", "Old page", groups, true).map { it.id }
        assertEquals(listOf("private", "paste", "reopen", "group:a", "group:b", "group:c"), ids)
    }

    @Test fun privateSpaceOffersANormalTabAndNoGroups() {
        val ids = quickEntries(true, "hi", "Old", groups, true).map { it.id }
        assertEquals(listOf("normal", "paste", "reopen"), ids)
    }

    @Test fun emptyClipboardAndNoClosedTabLeaveTheBasics() {
        assertEquals(listOf("private"), quickEntries(false, "  ", null, emptyList(), true).map { it.id })
        assertEquals(listOf("private"), quickEntries(false, "x", null, emptyList(), false).map { it.id })
    }

    @Test fun clipboardTextIsOnlyShownWhenItIsAnAddress() {
        assertEquals("example.com/a", quickEntries(false, "example.com/a", null, emptyList(), true)[1].subtitle)
        assertEquals("Text from the clipboard", quickEntries(false, "hunter2Secret", null, emptyList(), true)[1].subtitle)
        assertEquals("Text from the clipboard", quickEntries(false, "my pass.word x", null, emptyList(), true)[1].subtitle)
    }
}
