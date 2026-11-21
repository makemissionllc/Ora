package com.makemission.ora.readinglist

/** The reading list as exchanged between devices: the pages, and the pages removed (address to the time it was removed). */
data class ReadingState(val items: List<ReadingItem>, val removed: Map<String, Long>)

/**
 * Merges two devices' reading lists so both end with the same list, whichever synced first (last writer wins):
 * - A page on one side only is added.
 * - A page on both sides keeps the read or unread state that was changed most recently.
 * - A page removed on one side after it was last changed on the other is removed on both. A page saved again
 *   after it was removed stays.
 * Pure, so it is tested on the JVM.
 */
object ReadingMerge {
    private const val TOMBSTONE_KEEP_MS = 30L * 24 * 60 * 60_000L
    private const val MAX_ITEMS = 500

    private fun changedAt(item: ReadingItem) = maxOf(item.addedAt, item.readAt)

    fun merge(a: ReadingState, b: ReadingState, now: Long = System.currentTimeMillis()): ReadingState {
        val removed = HashMap<String, Long>()
        (a.removed.entries + b.removed.entries).forEach { (url, time) -> removed[url] = maxOf(removed[url] ?: 0L, time) }
        val byUrl = HashMap<String, ReadingItem>()
        (a.items + b.items).forEach { item ->
            val have = byUrl[item.url]
            byUrl[item.url] = if (have == null) item else {
                val newer = if (changedAt(item) > changedAt(have)) item else have
                newer.copy(addedAt = minOf(item.addedAt, have.addedAt))
            }
        }
        val items = byUrl.values
            .filter { (removed[it.url] ?: 0L) <= changedAt(it) }
            .sortedByDescending { it.addedAt }
            .take(MAX_ITEMS)
        val keptRemoved = removed.filter { (url, time) -> now - time < TOMBSTONE_KEEP_MS && items.none { it.url == url } }
        return ReadingState(items, keptRemoved)
    }
}
