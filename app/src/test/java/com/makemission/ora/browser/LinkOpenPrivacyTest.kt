package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkOpenPrivacyTest {
    @Test
    fun linksFromPrivateTabsLeaveNoLogLine() {
        assertNull(linkOpenLogLine("Opened link in new tab", "https://example.com/a", fromPrivate = true))
        assertNull(linkOpenLogLine("Opened image in new tab", "https://example.com/i.png", fromPrivate = true))
    }

    @Test
    fun linksFromNormalTabsAreLogged() {
        assertEquals(
            "Opened link in background tab: https://example.com/a",
            linkOpenLogLine("Opened link in background tab", "https://example.com/a", fromPrivate = false)
        )
    }
}
