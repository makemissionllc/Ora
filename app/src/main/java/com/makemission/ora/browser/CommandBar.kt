package com.makemission.ora.browser

/** One thing the address bar can do when the text starts with ">". [name] is also the Quick switcher action key. */
data class Command(val name: String, val hint: String)

/**
 * The command bar: type ">" in the address bar, then a few letters, and pick an action instead of a page.
 * The same names run from the Quick switcher, so one list serves both.
 */
object CommandBar {
    const val PREFIX = ">"

    val commands = listOf(
        Command("New tab", "Open a blank tab"),
        Command("New private tab", "Nothing is saved"),
        Command("Close tab", "Close the tab you are on"),
        Command("Reopen closed tab", "Bring back the last one"),
        Command("Library", "Bookmarks, history and reading list"),
        Command("Settings", "Open Ora's settings"),
        Command("Find in page", "Search the page you are on"),
        Command("Hide an element", "Pick something on this page to hide"),
        Command("Site tweaks", "Dark, wide text or a font for this site"),
        Command("Toggle dark colours", "Turn dark colours on or off for this site"),
        Command("Save as PDF", "Save this page as a PDF"),
        Command("Save session", "Save the open tabs under a name"),
        Command("Saved sessions", "Open or delete saved sessions"),
        Command("Keyboard shortcuts", "See every shortcut"),
    )

    fun isCommand(typed: String) = typed.trimStart().startsWith(PREFIX)

    /** Commands whose name contains every typed word, best (name starts with the text) first. Empty text lists all. */
    fun matches(typed: String): List<Command> {
        val q = typed.trimStart().removePrefix(PREFIX).trim().lowercase()
        if (q.isEmpty()) return commands
        val words = q.split(' ').filter { it.isNotEmpty() }
        return commands
            .filter { c -> words.all { c.name.lowercase().contains(it) } }
            .sortedBy { if (it.name.lowercase().startsWith(q)) 0 else 1 }
    }
}
