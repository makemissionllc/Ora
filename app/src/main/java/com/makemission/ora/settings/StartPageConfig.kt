package com.makemission.ora.settings

import com.makemission.ora.bookmarks.Bookmark
import com.makemission.ora.history.HistoryEntry
import com.makemission.ora.history.frecency
import java.net.URI

/** The parts of the start page, in the order the user arranged them. */
enum class StartSection(val key: String, val label: String, val hint: String) {
    GREETING("greeting", "Greeting", "The time of day and today's date"),
    SEARCH("search", "Search field", "Type a search or an address"),
    SHORTCUTS("shortcuts", "Shortcuts", "A grid of sites you open often"),
    STATUS("status", "Status cards", "Privacy and bookmark counts");

    companion object {
        fun fromKey(key: String): StartSection? = entries.firstOrNull { it.key == key }
    }
}

/** Where the shortcut tiles come from. */
enum class ShortcutSource(val key: String, val label: String) {
    BOOKMARKS("bookmarks", "Bookmarks"),
    MOST_VISITED("visited", "Most visited"),
    PINNED("pinned", "Pinned");

    companion object {
        fun fromKey(key: String?): ShortcutSource = entries.firstOrNull { it.key == key } ?: BOOKMARKS
    }
}

enum class TileShape(val key: String, val label: String) {
    ROUNDED("rounded", "Rounded"),
    CIRCLE("circle", "Circle");

    companion object {
        fun fromKey(key: String?): TileShape = entries.firstOrNull { it.key == key } ?: ROUNDED
    }
}

enum class TileSize(val key: String, val label: String) {
    SMALL("small", "Small"),
    REGULAR("regular", "Regular"),
    LARGE("large", "Large");

    companion object {
        fun fromKey(key: String?): TileSize = entries.firstOrNull { it.key == key } ?: REGULAR
    }
}

data class StartSlot(val section: StartSection, val enabled: Boolean)

/** Everything the user can change about the start page. */
data class StartPageConfig(
    val slots: List<StartSlot> = DEFAULT_SLOTS,
    val source: ShortcutSource = ShortcutSource.BOOKMARKS,
    /** 1 to 3 rows of shortcuts. */
    val rows: Int = 2,
    val shape: TileShape = TileShape.ROUNDED,
    val size: TileSize = TileSize.REGULAR,
    /** Whether each tile shows its name under it. */
    val labels: Boolean = true,
    val pinned: List<Bookmark> = emptyList(),
) {
    /** The sections to draw, in order. */
    val shown: List<StartSection> get() = slots.filter { it.enabled }.map { it.section }

    companion object {
        const val MIN_ROWS = 1
        const val MAX_ROWS = 3
        const val MAX_PINNED = 24

        val DEFAULT_SLOTS = listOf(
            StartSlot(StartSection.GREETING, false),
            StartSlot(StartSection.SEARCH, true),
            StartSlot(StartSection.SHORTCUTS, true),
            StartSlot(StartSection.STATUS, true),
        )
    }
}

/** "greeting:0,search:1,..." for saving. */
fun encodeSlots(slots: List<StartSlot>): String = slots.joinToString(",") { "${it.section.key}:${if (it.enabled) 1 else 0}" }

/**
 * Reads saved slots. Unknown or repeated names are dropped, and a section a newer version added is appended in its
 * default state, so an old save never loses a part of the page.
 */
fun parseSlots(raw: String?): List<StartSlot> {
    if (raw.isNullOrBlank()) return StartPageConfig.DEFAULT_SLOTS
    val seen = LinkedHashMap<StartSection, Boolean>()
    raw.split(',').forEach { part ->
        val bits = part.split(':')
        val section = StartSection.fromKey(bits.firstOrNull()?.trim().orEmpty()) ?: return@forEach
        if (section !in seen) seen[section] = bits.getOrNull(1)?.trim() == "1"
    }
    StartPageConfig.DEFAULT_SLOTS.forEach { if (it.section !in seen) seen[it.section] = it.enabled }
    return seen.map { StartSlot(it.key, it.value) }
}

/** [slots] with the entry at [index] moved by [delta] places (-1 up, 1 down). Out of range changes nothing. */
fun movedSlot(slots: List<StartSlot>, index: Int, delta: Int): List<StartSlot> {
    val to = index + delta
    if (index !in slots.indices || to !in slots.indices) return slots
    return slots.toMutableList().also { val item = it.removeAt(index); it.add(to, item) }
}

/** [slots] with [section] switched on or off. */
fun withSlotEnabled(slots: List<StartSlot>, section: StartSection, enabled: Boolean): List<StartSlot> =
    slots.map { if (it.section == section) it.copy(enabled = enabled) else it }

/** The greeting for an hour of the day (0 to 23). */
fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

/**
 * The sites opened most, best first: web addresses only, one per site, ranked by [frecency]. Private tabs never
 * reach history, so they cannot appear here.
 */
fun mostVisited(history: List<HistoryEntry>, limit: Int, now: Long = System.currentTimeMillis()): List<Bookmark> {
    val seen = HashSet<String>()
    val out = ArrayList<Bookmark>()
    for (entry in history.sortedByDescending { it.frecency(now) }) {
        if (out.size >= limit) break
        val host = hostOf(entry.url) ?: continue
        if (!seen.add(host)) continue
        out.add(Bookmark(entry.url, entry.title.ifBlank { host }))
    }
    return out
}

private fun hostOf(url: String): String? {
    if (!url.startsWith("http://") && !url.startsWith("https://")) return null
    return try { URI(url).host?.removePrefix("www.")?.lowercase()?.takeIf { it.isNotEmpty() } } catch (e: Exception) { null }
}

/** Turns what was typed into a saved address: adds https:// when there is no scheme, and refuses text that is not an address. */
fun normalizePinnedUrl(input: String): String? {
    val text = input.trim()
    if (text.isEmpty() || text.any { it.isWhitespace() }) return null
    val full = if (text.contains("://")) text else "https://$text"
    if (!full.startsWith("http://") && !full.startsWith("https://")) return null
    val host = hostOf(full) ?: return null
    return if (host.contains('.') || host == "localhost") full else null
}

/** One pinned site per line, "title<TAB>address". Tabs and line breaks are removed from titles. */
fun encodePinned(list: List<Bookmark>): String =
    list.joinToString("\n") { "${it.title.replace(Regex("[\\t\\r\\n]+"), " ").trim()}\t${it.url}" }

fun parsePinned(raw: String?): List<Bookmark> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.lines().mapNotNull { line ->
        val bits = line.split('\t', limit = 2)
        if (bits.size != 2 || bits[1].isBlank()) null else Bookmark(bits[1].trim(), bits[0].trim())
    }.take(StartPageConfig.MAX_PINNED)
}

/** [list] with one more pinned site. A repeated address is not added twice, and the list stops at [StartPageConfig.MAX_PINNED]. */
fun withPinned(list: List<Bookmark>, url: String, title: String): List<Bookmark> {
    if (list.any { it.url.equals(url, ignoreCase = true) } || list.size >= StartPageConfig.MAX_PINNED) return list
    return list + Bookmark(url, title.replace(Regex("[\\t\\r\\n]+"), " ").trim().ifBlank { hostOf(url).orEmpty() })
}
