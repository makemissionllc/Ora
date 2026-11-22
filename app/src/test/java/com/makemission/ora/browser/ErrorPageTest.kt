package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mozilla.geckoview.WebRequestError

class ErrorPageTest {
    @Test
    fun noNetworkShowsTheOfflineCardWhateverGeckoViewSaid() {
        val message = ErrorPage.messageFor(
            WebRequestError.ERROR_UNKNOWN_HOST, WebRequestError.ERROR_CATEGORY_URI,
            host = "theverg.com", online = false
        )
        assertEquals("You're offline", message.title)
        assertEquals(ErrorPage.Icon.OFFLINE, message.icon)
        assertEquals("no network", message.support)
    }

    @Test
    fun unknownHostNamesTheSiteInTheTitle() {
        val message = ErrorPage.messageFor(
            WebRequestError.ERROR_UNKNOWN_HOST, WebRequestError.ERROR_CATEGORY_URI,
            host = "theverg.com", online = true
        )
        assertEquals("Can't find theverg.com", message.title)
        assertEquals(ErrorPage.Icon.NOT_FOUND, message.icon)
    }

    @Test
    fun certificateProblemHasOneButtonAndOnlyAOneTimeEngineOverride() {
        val message = ErrorPage.messageFor(WebRequestError.ERROR_SECURITY_BAD_CERT, WebRequestError.ERROR_CATEGORY_SECURITY)
        assertEquals(ErrorPage.Icon.SECURITY, message.icon)
        assertEquals(ErrorPage.Action.GO_BACK, message.primary)
        assertNull(message.secondary)
        assertEquals(ErrorPage.Allow.CERT, message.allow)
    }

    @Test
    fun httpsOnlyAndSafeBrowsingGetAOneTimeOverrideButHstsDoesNot() {
        assertEquals(ErrorPage.Allow.HTTP_ONCE, ErrorPage.messageFor(WebRequestError.ERROR_HTTPS_ONLY, WebRequestError.ERROR_CATEGORY_SECURITY).allow)
        assertEquals(ErrorPage.Allow.RISK_ONCE, ErrorPage.messageFor(WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI, WebRequestError.ERROR_CATEGORY_SAFEBROWSING).allow)
        assertNull(ErrorPage.messageFor(WebRequestError.ERROR_BAD_HSTS_CERT, WebRequestError.ERROR_CATEGORY_SECURITY).allow)
    }

    @Test
    fun blockedPortGetsItsOwnCardNotTheGenericOne() {
        val message = ErrorPage.messageFor(WebRequestError.ERROR_PORT_BLOCKED, WebRequestError.ERROR_CATEGORY_NETWORK)
        assertEquals("This address uses a blocked port", message.title)
        assertEquals("port blocked", message.support)
    }

    @Test
    fun timeoutCardOffersTryAgainAndGoBackWithItsCategoryForSupport() {
        val message = ErrorPage.messageFor(
            WebRequestError.ERROR_NET_TIMEOUT, WebRequestError.ERROR_CATEGORY_NETWORK,
            host = "oceanofpdf.com", online = true
        )
        assertEquals("oceanofpdf.com isn't responding", message.title)
        assertEquals("network", message.category)
        assertEquals("connection timed out", message.support)
        assertEquals(ErrorPage.Action.TRY_AGAIN, message.primary)
        assertEquals(ErrorPage.Action.GO_BACK, message.secondary)
    }
}
