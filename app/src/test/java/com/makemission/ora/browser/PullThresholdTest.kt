package com.makemission.ora.browser

import com.makemission.ora.ui.theme.BrowserTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PullThresholdTest {
    @Test
    fun pullThresholdIsAboutFifteenPercentLessThanTheOldNinetySixDp() {
        assertEquals(82f, BrowserTokens.PullRefreshThreshold.value, 0.01f)
        assertTrue(BrowserTokens.PullRefreshThreshold.value <= 96f * 0.86f)
    }
}
