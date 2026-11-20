package com.makemission.ora.browser

import com.makemission.ora.settings.isSearchInput

/**
 * One row in the address bar dropdown. [target] is what gets loaded when the row is tapped,
 * for example a full URL, or the typed text when the row offers a search.
 */
data class AddressSuggestion(val title: String, val subtitle: String, val target: String, val kind: SuggestionKind = SuggestionKind.LOCAL, val visits: Int = 0)

/** Where a suggestion came from: a bookmark or open tab, a phrase the search engine offered, or the typed text itself. */
enum class SuggestionKind { LOCAL, QUERY, SEARCH, COMMAND, HISTORY }

/**
 * Puts the engine's phrases into the list the way Google does: the phrases first, as plain searches, then the
 * bookmarks and open tabs that match. With no phrases (off, offline, private tab) the usual "Search for" row stays.
 */
fun withQuerySuggestions(base: List<AddressSuggestion>, phrases: List<String>): List<AddressSuggestion> {
    if (phrases.isEmpty()) return base
    val local = base.filter { it.kind == SuggestionKind.LOCAL || it.kind == SuggestionKind.HISTORY }
    val queries = phrases.map { AddressSuggestion(it, "", it, SuggestionKind.QUERY) }
    return queries + local.take(2)
}

/** Suggestions for what is typed: a search row first, then matching bookmarks and open tabs. */
fun addressSuggestions(
    typed: String,
    candidates: List<AddressSuggestion>,
    limit: Int = 6,
): List<AddressSuggestion> {
    val query = typed.trim()
    if (query.isEmpty()) return emptyList()
    if (CommandBar.isCommand(query)) {
        return CommandBar.matches(query).map { AddressSuggestion(it.name, it.hint, CommandBar.PREFIX + it.name, SuggestionKind.COMMAND) }
    }
    val search = if (isSearchInput(query)) {
        listOf(AddressSuggestion("Search for \"$query\"", "Default search engine", query, SuggestionKind.SEARCH))
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
