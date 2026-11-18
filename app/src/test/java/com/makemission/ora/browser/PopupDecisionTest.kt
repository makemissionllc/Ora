package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupDecisionTest {

    @Test
    fun aPopupFromATapOpensAsATabEvenWhenBlockingIsOn() {
        assertEquals(PopupDecision.OPEN_TAB, popupDecision(fromTap = true, siteAllowed = false, blockPopups = true))
    }

    @Test
    fun aPopupWithNoTapIsAskedWhenBlockingIsOnAndTheSiteIsNotAllowed() {
        assertEquals(PopupDecision.ASK, popupDecision(fromTap = false, siteAllowed = false, blockPopups = true))
    }

    @Test
    fun anAllowedSiteOpensWithoutAsking() {
        assertEquals(PopupDecision.OPEN_TAB, popupDecision(fromTap = false, siteAllowed = true, blockPopups = true))
    }

    @Test
    fun withBlockingOffNothingIsAsked() {
        assertEquals(PopupDecision.OPEN_TAB, popupDecision(fromTap = false, siteAllowed = false, blockPopups = false))
    }

    @Test
    fun aTapCountsOnlyWithinItsWindow() {
        PopupTaps.recordTap(now = 10_000L)
        assertTrue(PopupTaps.tappedRecently(now = 10_000L + PopupTaps.TAP_WINDOW_MS))
        assertFalse(PopupTaps.tappedRecently(now = 10_000L + PopupTaps.TAP_WINDOW_MS + 1))
    }

    @Test
    fun siteKeyIsTheDomainOnly() {
        assertEquals("oceanofpdf.com", popupSiteOf("https://www.oceanofpdf.com/book/page?id=7#top"))
    }
}
