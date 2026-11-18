package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.geckoview.WebRequestError

class ErrorWordsTest {
    @Test
    fun codeWinsOverCategoryForCode37() {
        assertEquals(
            "The address could not be found",
            ErrorPage.messageFor(WebRequestError.ERROR_UNKNOWN_HOST, WebRequestError.ERROR_CATEGORY_URI).title
        )
    }

    @Test
    fun uriAndProxyCategoriesHaveTheirOwnWords() {
        assertEquals(
            "That address doesn't look right",
            ErrorPage.messageFor(-1, WebRequestError.ERROR_CATEGORY_URI).title
        )
        assertEquals(
            "A proxy stopped the page",
            ErrorPage.messageFor(-1, WebRequestError.ERROR_CATEGORY_PROXY).title
        )
    }
}
