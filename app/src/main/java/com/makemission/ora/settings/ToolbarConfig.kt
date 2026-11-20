package com.makemission.ora.settings

/** A button the phone Top layout's bottom toolbar can hold. Menu is not here: it is always last and cannot be removed. */
enum class ToolbarItem(val id: String, val label: String) {
    BACK("back", "Back"),
    FORWARD("forward", "Forward"),
    TABS("tabs", "Tabs"),
    SHARE("share", "Share"),
    BOOKMARK("bookmark", "Bookmark"),
    HOME("home", "Home"),
    RELOAD("reload", "Reload"),
    NEW_TAB("new_tab", "New tab"),
    PRIVATE_TAB("private_tab", "Private tab"),
    FIND("find", "Find in page"),
    READER("reader", "Reader view"),
    DOWNLOADS("downloads", "Downloads"),
}

/** The bar's saved order, the presets, and reading and writing the saved text. */
object ToolbarConfig {
    /** At most this many buttons, before Menu. */
    const val MAX_BUTTONS = 5

    val CLASSIC = listOf(ToolbarItem.BACK, ToolbarItem.FORWARD, ToolbarItem.SHARE, ToolbarItem.BOOKMARK, ToolbarItem.TABS)
    val READING = listOf(ToolbarItem.BACK, ToolbarItem.READER, ToolbarItem.FIND, ToolbarItem.BOOKMARK, ToolbarItem.TABS)
    val MINIMAL = listOf(ToolbarItem.BACK, ToolbarItem.TABS)

    /** Preset names in the order the chips show them. */
    val PRESETS: List<Pair<String, List<ToolbarItem>>> = listOf("Classic" to CLASSIC, "Reading" to READING, "Minimal" to MINIMAL)

    /** Reads saved text such as "back,forward,tabs". Unknown ids (from an older or newer version) and repeats are dropped. */
    fun parse(saved: String?): List<ToolbarItem> {
        if (saved == null) return CLASSIC
        val byId = ToolbarItem.entries.associateBy { it.id }
        return saved.split(',').mapNotNull { byId[it.trim()] }.distinct().take(MAX_BUTTONS)
    }

    fun encode(items: List<ToolbarItem>): String = items.joinToString(",") { it.id }

    /** The preset this list equals, or "Custom". */
    fun presetName(items: List<ToolbarItem>): String = PRESETS.firstOrNull { it.second == items }?.first ?: "Custom"
}
