package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStoreTest {

    private class MapStorage : SessionStore.Storage {
        val map = HashMap<String, String>()
        override fun get(key: String) = map[key]
        override fun put(key: String, value: String) { map[key] = value }
        override fun remove(key: String) { map.remove(key) }
        override fun clear() = map.clear()
    }

    private val five = List(5) { SessionStore.SavedTab("https://site$it.example/page$it", "state-$it") }

    @Test
    fun fiveDifferentTabsComeBackAsTheyWereSaved() {
        val store = SessionStore(MapStorage())
        assertTrue(store.save("p", five, 3))
        val loaded = store.load("p")
        assertNotNull(loaded)
        assertEquals(five, loaded!!.tabs)
        assertEquals(3, loaded.selected)
        assertEquals(5, loaded.tabs.map { it.url }.toSet().size)
        assertEquals(5, loaded.tabs.map { it.state }.toSet().size)
    }

    @Test
    fun emptyStateNeverOverwritesAGoodSave() {
        val store = SessionStore(MapStorage())
        store.save("p", five, 1)
        assertFalse(store.save("p", emptyList(), 0))
        assertEquals(five, store.load("p")!!.tabs)
    }

    @Test
    fun profilesAreKeptApart() {
        val store = SessionStore(MapStorage())
        store.save("a", five, 0)
        store.save("b", listOf(SessionStore.SavedTab("https://b.example", null)), 0)
        assertEquals(5, store.load("a")!!.tabs.size)
        assertEquals(1, store.load("b")!!.tabs.size)
    }

    @Test
    fun tabsSavedBeforeTheirHistoryWasReadComeBackWithNoHistory() {
        val store = SessionStore(MapStorage())
        val early = listOf(SessionStore.SavedTab("https://a.example", null), SessionStore.SavedTab("https://b.example", null))
        store.save("p", early, 0)
        assertEquals(early, store.load("p")!!.tabs)
        // A state written as the text "null" (what the old reader produced) is also read as no history.
        val raw = MapStorage()
        raw.put("tabs_p", """{"selected":0,"tabs":[{"url":"https://a.example","state":"null"}]}""")
        assertEquals(null, SessionStore(raw).load("p")!!.tabs[0].state)
    }
}
