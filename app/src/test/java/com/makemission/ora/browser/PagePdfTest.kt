package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class PagePdfTest {
    @Test fun usesTheTitle() = assertEquals("Hello world.pdf", pdfFileName("Hello world", "https://a.com/x"))
    @Test fun removesForbiddenCharacters() = assertEquals("a b c.pdf", pdfFileName("a/b:c", "https://a.com"))
    @Test fun blankTitleFallsBackToTheSite() = assertEquals("example.com.pdf", pdfFileName("  ", "https://www.example.com/p"))
    @Test fun nothingAtAllIsPage() = assertEquals("page.pdf", pdfFileName("", ""))
    @Test fun longTitlesAreCut() = assertEquals(84, pdfFileName("x".repeat(200), "").length)
}
