package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreloaderTest {
    @Test fun queryStringsAreNeverPrefetched() {
        assertNull(Preloader.prefetchUrl("https://a.com/search?q=x", "a.com"))
    }

    @Test fun actionPathsAreNeverPrefetched() {
        assertNull(Preloader.prefetchUrl("https://a.com/account/logout", "a.com"))
    }

    @Test fun plainSiteBecomesHttpsRoot() {
        assertEquals("https://a.com/", Preloader.prefetchUrl("a.com", "a.com"))
        assertEquals("https://a.com/news", Preloader.prefetchUrl("http://a.com/news", "a.com"))
    }

    @Test fun slowAdviceFollowsTheCause() {
        assertEquals(true, slowLoadAdvice(online = false, progress = 0, saverOn = false).retry)
        assertEquals(true, slowLoadAdvice(online = true, progress = 5, saverOn = false).retry)
        assertEquals(false, slowLoadAdvice(online = true, progress = 70, saverOn = false).retry)
    }
}
