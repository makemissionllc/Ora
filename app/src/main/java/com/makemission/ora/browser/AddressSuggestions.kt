package com.makemission.ora.browser

import com.makemission.ora.settings.isSearchInput

/**
 * One row in the address bar dropdown. [target] is what gets loaded when the row is tapped,
 * for example a full URL, or the typed text when the row offers a search.
 */
data class AddressSuggestion(val title: String, val subtitle: String, val target: String)

/** Suggestions for what is typed: a search row first, then matching bookmarks and open tabs. */
fun addressSuggestions(
    typed: String,
    candidates: List<AddressSuggestion>,
    limit: Int = 6,
): List<AddressSuggestion> {
    val query = typed.trim()
    if (query.isEmpty()) return emptyList()
    val search = if (isSearchInput(query)) {
        listOf(AddressSuggestion("Search for \"$query\"", "Default search engine", query))
    } else {
        emptyList()
    }
    val matches = candidates
        .filter { it.title.contains(query, ignoreCase = true) || it.target.contains(query, ignoreCase = true) }
        .take(limit)
    return search + matches
}

/**
 * The host of the first candidate that starts with [typed], used to finish a domain as the user types.
 * Returns null when nothing matches, the text has a space, or it already looks like a full URL.
 */
fun autocompleteFor(typed: String, candidates: List<AddressSuggestion>): String? {
    val query = typed.trim()
    if (query.isEmpty() || query.contains(' ') || query.contains("://")) return null
    return candidates
        .map { it.subtitle }
        .firstOrNull { it.length > query.length && it.startsWith(query, ignoreCase = true) }
}
