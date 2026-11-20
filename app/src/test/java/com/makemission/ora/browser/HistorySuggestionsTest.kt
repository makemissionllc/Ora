package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySuggestionsTest {
    private val history = AddressSuggestion("Ora blog", "ora.example/blog", "https://ora.example/blog", SuggestionKind.HISTORY, 4)

    @Test fun historyRowsMatchTypedWords() {
        val rows = addressSuggestions("blog", listOf(history))
        assertEquals(SuggestionKind.HISTORY, rows.last().kind)
    }

    @Test fun historyStaysWhenEnginePhrasesArrive() {
        val rows = withQuerySuggestions(listOf(history), listOf("blog post"))
        assertEquals(listOf(SuggestionKind.QUERY, SuggestionKind.HISTORY), rows.map { it.kind })
    }
}
