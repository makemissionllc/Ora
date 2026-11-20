package com.makemission.ora.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementZapperTest {
    @Test fun addKeepsSitesApart() {
        var r = ZapLogic.add(emptyMap(), "News.Example.com", "#banner")
        r = ZapLogic.add(r, "other.org", "div > p")
        assertEquals(listOf("#banner"), r["news.example.com"])
        assertEquals(2, r.size)
    }

    @Test fun repeatsAndBlanksIgnored() {
        val r = ZapLogic.add(emptyMap(), "a.com", "#x")
        assertEquals(r, ZapLogic.add(r, "a.com", "#x"))
        assertEquals(r, ZapLogic.add(r, "a.com", "  "))
        assertEquals(r, ZapLogic.add(r, "", "#y"))
    }

    @Test fun capKeepsNewest() {
        var r = emptyMap<String, List<String>>()
        repeat(ZapLogic.MAX_PER_SITE + 5) { r = ZapLogic.add(r, "a.com", "#e$it") }
        assertEquals(ZapLogic.MAX_PER_SITE, r["a.com"]!!.size)
        assertEquals("#e${ZapLogic.MAX_PER_SITE + 4}", r["a.com"]!!.last())
    }

    @Test fun clearAndSummary() {
        var r = ZapLogic.add(ZapLogic.add(ZapLogic.add(emptyMap(), "a.com", "#1"), "b.com", "#1"), "b.com", "#2")
        assertEquals(listOf("b.com" to 2, "a.com" to 1), ZapLogic.summary(r))
        r = ZapLogic.clear(r, "B.com")
        assertTrue("b.com" !in r)
    }
}
