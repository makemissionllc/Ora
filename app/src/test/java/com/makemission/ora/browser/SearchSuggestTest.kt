package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSuggestTest {

    @Test
    fun readsThePhrasesFromAnOpenSearchAnswer() {
        val answer = """["how to", ["how to tie a tie", "how to screenshot", "how to tie a tie", "HOW TO"]]"""
        assertEquals(listOf("how to tie a tie", "how to screenshot"), SearchSuggest.parse(answer, "how to"))
    }

    @Test
    fun aBadAnswerGivesNothing() {
        assertEquals(emptyList<String>(), SearchSuggest.parse("<html>nope</html>", "cats"))
        assertEquals(emptyList<String>(), SearchSuggest.parse("[]", "cats"))
    }

    @Test
    fun onlySearchesAreAskedAbout() {
        assertTrue(SearchSuggest.worthAsking("weather"))
        assertTrue(SearchSuggest.worthAsking("github login"))
        assertFalse(SearchSuggest.worthAsking("a"))
        assertFalse(SearchSuggest.worthAsking("github.com"))
        assertFalse(SearchSuggest.worthAsking("https://example.com/x"))
    }

    @Test
    fun phrasesComeFirstThenAtMostTwoLocalMatches() {
        val base = listOf(
            AddressSuggestion("Search for \"we\"", "Default search engine", "we", SuggestionKind.SEARCH),
            AddressSuggestion("Weather", "weather.com", "https://weather.com"),
            AddressSuggestion("Web", "web.dev", "https://web.dev"),
            AddressSuggestion("Wired", "wired.com", "https://wired.com"),
        )
        val merged = withQuerySuggestions(base, listOf("weather today", "weather tomorrow"))
        assertEquals(listOf("weather today", "weather tomorrow", "Weather", "Web"), merged.map { it.title })
        assertEquals(base, withQuerySuggestions(base, emptyList()))
    }
}
