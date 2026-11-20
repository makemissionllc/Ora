package com.makemission.ora.settings

import java.net.URLEncoder

/** A word that sends the rest of what you type to one site's search, such as "yt cats". [template] uses %s for the encoded query. */
data class SearchShortcut(val keyword: String, val label: String, val template: String) {
    fun urlFor(query: String): String = template.replace("%s", URLEncoder.encode(query, "UTF-8"))
}

/** What a typed line means: the shortcut it starts with and the text left over to search for. */
data class ShortcutMatch(val shortcut: SearchShortcut, val query: String)

/**
 * Search shortcuts for the address bar. The built-in set follows the engines Ora already knows plus a few well known
 * sites; people can add their own in Settings. [resolve] is pure so it can be tested. [enabled] and [custom] are
 * set by the browser screen from the settings, because the address bar has no settings object of its own.
 */
object SearchShortcuts {
    @Volatile var enabled: Boolean = true
    @Volatile var custom: List<SearchShortcut> = emptyList()

    val builtIn: List<SearchShortcut> = listOf(
        SearchShortcut("g", "Google", SearchEngine.GOOGLE.template),
        SearchShortcut("ddg", "DuckDuckGo", SearchEngine.DUCKDUCKGO.template),
        SearchShortcut("b", "Bing", SearchEngine.BING.template),
        SearchShortcut("w", "Wikipedia", SearchEngine.WIKIPEDIA.template),
        SearchShortcut("yt", "YouTube", "https://www.youtube.com/results?search_query=%s"),
        SearchShortcut("gh", "GitHub", "https://github.com/search?q=%s"),
        SearchShortcut("so", "Stack Overflow", "https://stackoverflow.com/search?q=%s"),
        SearchShortcut("mdn", "MDN Web Docs", "https://developer.mozilla.org/en-US/search?q=%s"),
        SearchShortcut("maps", "Google Maps", "https://www.google.com/maps/search/%s"),
        SearchShortcut("amz", "Amazon", "https://www.amazon.com/s?k=%s"),
    )

    /** A keyword is one lower-case word with no spaces or dots (a dot would make it look like a web address). */
    fun validKeyword(keyword: String): Boolean = keyword.isNotEmpty() && keyword.length <= 12 && keyword.all { it.isLetterOrDigit() }

    /** A template must be a web address with exactly one %s. */
    fun validTemplate(template: String): Boolean =
        (template.startsWith("https://") || template.startsWith("http://")) && template.split("%s").size == 2

    /**
     * The shortcut [input] starts with, when it is "keyword", a space, and at least one more word. Your own shortcuts come
     * first, so one can replace a built-in keyword. Null when shortcuts are off, there is no match, or no query follows.
     */
    fun resolve(input: String, extras: List<SearchShortcut> = custom, on: Boolean = enabled): ShortcutMatch? {
        if (!on) return null
        val text = input.trimStart()
        val space = text.indexOf(' ')
        if (space <= 0) return null
        val word = text.substring(0, space).lowercase()
        val query = text.substring(space + 1).trim()
        if (query.isEmpty()) return null
        val shortcut = (extras + builtIn).firstOrNull { it.keyword == word } ?: return null
        return ShortcutMatch(shortcut, query)
    }
}
