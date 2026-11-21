package com.makemission.ora.devices

import com.makemission.ora.readinglist.ReadingItem
import com.makemission.ora.readinglist.ReadingMerge
import com.makemission.ora.readinglist.ReadingState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingMergeTest {
    private fun item(url: String, added: Long, read: Boolean = false, readAt: Long = 0) = ReadingItem(url, url, added, read, readAt)
    private fun state(vararg items: ReadingItem, removed: Map<String, Long> = emptyMap()) = ReadingState(items.toList(), removed)
    private val now = 10_000L

    @Test fun pagesOnOneSideAreAddedToBoth() {
        val m = ReadingMerge.merge(state(item("https://a", 1)), state(item("https://b", 2)), now)
        assertEquals(listOf("https://b", "https://a"), m.items.map { it.url })
    }

    @Test fun theNewestReadChangeWins() {
        val a = state(item("https://a", 1, read = true, readAt = 5))
        val b = state(item("https://a", 1, read = false, readAt = 8))
        assertFalse(ReadingMerge.merge(a, b, now).items.single().read)
        assertFalse(ReadingMerge.merge(b, a, now).items.single().read)
    }

    @Test fun aRemovalAfterTheLastChangeRemovesEverywhere() {
        val a = state(item("https://a", 1))
        val b = ReadingState(emptyList(), mapOf("https://a" to 5L))
        assertTrue(ReadingMerge.merge(a, b, now).items.isEmpty())
        assertTrue(ReadingMerge.merge(b, a, now).items.isEmpty())
    }

    @Test fun aPageSavedAgainAfterRemovalStays() {
        val a = state(item("https://a", 7))
        val b = ReadingState(emptyList(), mapOf("https://a" to 5L))
        assertEquals(1, ReadingMerge.merge(a, b, now).items.size)
    }

    @Test fun mergeIsTheSameInEitherOrder() {
        val a = state(item("https://a", 1, true, 4), item("https://c", 3), removed = mapOf("https://b" to 6L))
        val b = state(item("https://a", 1, false, 2), item("https://b", 2))
        assertEquals(ReadingMerge.merge(a, b, now), ReadingMerge.merge(b, a, now))
    }

    @Test fun messagesKeepOnlyWebAddresses() {
        val body = JSONObject().put("items", JSONArray()
            .put(JSONObject().put("url", "https://ok").put("addedAt", 1))
            .put(JSONObject().put("url", "javascript:alert(1)").put("addedAt", 2)))
        assertEquals(listOf("https://ok"), ReadingSync.fromJson(body).items.map { it.url })
    }
}
