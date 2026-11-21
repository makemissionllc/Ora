package com.makemission.ora.devices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContinueLogicTest {
    private fun reply(url: String, age: Long, name: String = "Tablet") = WhereReply(name, name, url, "t", age)

    @Test fun picksTheNewestFreshPage() {
        val offer = ContinueLogic.pick(listOf(reply("https://a.com", 5 * 60_000, "A"), reply("https://b.com", 60_000, "B")), emptyList())
        assertEquals("https://b.com", offer?.url)
    }

    @Test fun ignoresOldPagesAndPagesAlreadyOpen() {
        assertNull(ContinueLogic.pick(listOf(reply("https://a.com", ContinueLogic.FRESH_MS + 1)), emptyList()))
        assertNull(ContinueLogic.pick(listOf(reply("https://a.com/x#top", 1000)), listOf("https://A.com/x/")))
    }

    @Test fun ignoresNonWebAddresses() {
        assertNull(ContinueLogic.pick(listOf(reply("javascript:alert(1)", 1000)), emptyList()))
    }
}
