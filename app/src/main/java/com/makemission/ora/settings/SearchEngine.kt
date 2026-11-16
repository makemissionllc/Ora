package com.makemission.ora.settings

import java.net.URLEncoder

/** A search engine the address bar can search with. [template] uses %s for the encoded query. */
enum class SearchEngine(val key: String, val label: String, val template: String) {
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    GOOGLE("google", "Google", "https://www.google.com/search?q=%s"),
    BING("bing", "Bing", "https://www.bing.com/search?q=%s"),
    BRAVE("brave", "Brave Search", "https://search.brave.com/search?q=%s"),
    ECOSIA("ecosia", "Ecosia", "https://www.ecosia.org/search?q=%s"),
    WIKIPEDIA("wikipedia", "Wikipedia", "https://en.wikipedia.org/w/index.php?search=%s");

    /** The address for [query] in this engine. */
    fun urlFor(query: String): String = template.replace("%s", URLEncoder.encode(query, "UTF-8"))

    companion object {
        fun fromKey(key: String?): SearchEngine? = entries.firstOrNull { it.key == key }
    }
}

/**
 * True when address-bar input would be searched rather than opened as a URL.
 * Mirrors the rules in toLoadableUrl so both agree on what counts as a search.
 */
fun isSearchInput(input: String): Boolean {
    val text = input.trim()
    if (text.isEmpty()) return false
    if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*").matches(text)) return false
    return text.contains(' ') || !text.contains('.')
}
