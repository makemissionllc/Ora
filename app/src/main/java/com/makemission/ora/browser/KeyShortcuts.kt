package com.makemission.ora.browser

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Every action a physical keyboard can start. [group] and [keys] are what the shortcuts page shows. */
enum class Shortcut(val group: String, val label: String, val keys: String) {
    ADDRESS("Address bar", "Go to the address bar", "Ctrl+L, Ctrl+E, Alt+D or F6"),
    FIND("Page", "Find in page", "Ctrl+F"),
    RELOAD("Page", "Reload", "Ctrl+R or F5"),
    HARD_RELOAD("Page", "Reload without cache", "Ctrl+Shift+R"),
    BACK("Page", "Back", "Alt+Left"),
    FORWARD("Page", "Forward", "Alt+Right"),
    START_PAGE("Page", "Go to the start page", "Alt+Home"),
    SAVE_PDF("Page", "Save the page as a PDF", "Ctrl+S or Ctrl+P"),
    BOOKMARK("Page", "Bookmark or unbookmark this page", "Ctrl+D"),
    NEW_TAB("Tabs", "New tab", "Ctrl+T"),
    NEW_PRIVATE_TAB("Tabs", "New private tab", "Ctrl+Shift+N"),
    CLOSE_TAB("Tabs", "Close this tab", "Ctrl+W"),
    REOPEN_TAB("Tabs", "Reopen the closed tab", "Ctrl+Shift+T"),
    NEXT_TAB("Tabs", "Next tab", "Ctrl+Tab or Ctrl+Page Down"),
    PREVIOUS_TAB("Tabs", "Previous tab", "Ctrl+Shift+Tab or Ctrl+Page Up"),
    TAB_1("Tabs", "Go to tab 1 to 8", "Ctrl+1 to Ctrl+8"),
    LAST_TAB("Tabs", "Go to the last tab", "Ctrl+9"),
    ZOOM_IN("Page", "Larger text on pages", "Ctrl+Plus"),
    ZOOM_OUT("Page", "Smaller text on pages", "Ctrl+Minus"),
    ZOOM_RESET("Page", "Reset text size", "Ctrl+0"),
    COPY_ADDRESS("Page", "Copy the page address", "Ctrl+Shift+C"),
    DUPLICATE_TAB("Tabs", "Duplicate this tab", "Ctrl+Shift+K"),
    QUICK_SWITCHER("Ora", "Quick switcher: tabs, bookmarks, history, actions", "Ctrl+K"),
    SAVE_SESSION("Ora", "Save the open tabs as a session", "Ctrl+Shift+S"),
    SESSIONS("Ora", "Saved sessions", "Ctrl+Shift+O"),
    ZAP("Page", "Hide an element on this page", "Ctrl+Shift+X"),
    SITE_TWEAKS("Page", "Site tweaks: dark, wide text, font", "Ctrl+Shift+D"),
    LIBRARY("Ora", "Library: bookmarks, reading list and history", "Ctrl+H"),
    SETTINGS("Ora", "Settings", "Ctrl+,"),
    HELP("Ora", "Keyboard shortcuts", "Ctrl+/ or F1"),
    ESCAPE("Ora", "Close what is open (like Back)", "Esc");
}

/**
 * Maps a physical key press to a [Shortcut] and carries it to the open browser screen. [MainActivity] asks
 * [resolve] for every key press before any view sees it, so a shortcut works wherever the focus is, including on
 * the web page. [handler] is set by the browser screen while it is showing; [focusAddress] asks the address bar to
 * take focus. Plain keys and Shift-only keys are never taken, so typing in pages and fields is untouched.
 */
object KeyShortcuts {
    @Volatile
    var handler: ((Shortcut) -> Boolean)? = null

    /**
     * Set by the interactive tutorial's keyboard lesson. While it is set, a recognised shortcut goes here and the
     * browser does not act on it, so practising Ctrl+K does not open the switcher behind the tutorial.
     */
    @Volatile
    var practice: ((Shortcut) -> Unit)? = null

    /** The tab a Ctrl+digit press asked for (0 based), set just before [handler] runs for [Shortcut.TAB_1]. */
    @Volatile
    var tabIndex: Int = 0

    private val _focusAddress = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val focusAddress: SharedFlow<Unit> = _focusAddress.asSharedFlow()

    fun requestAddressFocus() {
        _focusAddress.tryEmit(Unit)
    }

    /** Which tab (0 based) a Ctrl+digit shortcut goes to, or -1 for the last tab; null when [keyCode] is not a digit 1 to 9. */
    fun tabIndexFor(keyCode: Int): Int? = when (keyCode) {
        in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_8 -> keyCode - KeyEvent.KEYCODE_1
        KeyEvent.KEYCODE_9 -> -1
        else -> null
    }

    /** The shortcut for this key press, or null when the press is not one of ours. Pure, so it can be tested. */
    fun resolve(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Shortcut? {
        when (keyCode) {
            KeyEvent.KEYCODE_F6 -> if (!ctrl && !alt) return Shortcut.ADDRESS
            KeyEvent.KEYCODE_F5 -> if (!alt) return if (ctrl && shift) Shortcut.HARD_RELOAD else Shortcut.RELOAD
            KeyEvent.KEYCODE_F1 -> if (!ctrl && !alt && !shift) return Shortcut.HELP
            KeyEvent.KEYCODE_ESCAPE -> if (!ctrl && !alt && !shift) return Shortcut.ESCAPE
        }
        if (alt && !ctrl) {
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> Shortcut.BACK
                KeyEvent.KEYCODE_DPAD_RIGHT -> Shortcut.FORWARD
                KeyEvent.KEYCODE_MOVE_HOME -> Shortcut.START_PAGE
                KeyEvent.KEYCODE_D -> if (!shift) Shortcut.ADDRESS else null
                else -> null
            }
        }
        if (!ctrl || alt) return null
        if (!shift && tabIndexFor(keyCode) != null) return if (keyCode == KeyEvent.KEYCODE_9) Shortcut.LAST_TAB else Shortcut.TAB_1
        return if (shift) when (keyCode) {
            KeyEvent.KEYCODE_R -> Shortcut.HARD_RELOAD
            KeyEvent.KEYCODE_N -> Shortcut.NEW_PRIVATE_TAB
            KeyEvent.KEYCODE_T -> Shortcut.REOPEN_TAB
            KeyEvent.KEYCODE_TAB -> Shortcut.PREVIOUS_TAB
            KeyEvent.KEYCODE_C -> Shortcut.COPY_ADDRESS
            KeyEvent.KEYCODE_K -> Shortcut.DUPLICATE_TAB
            KeyEvent.KEYCODE_S -> Shortcut.SAVE_SESSION
            KeyEvent.KEYCODE_O -> Shortcut.SESSIONS
            KeyEvent.KEYCODE_X -> Shortcut.ZAP
            KeyEvent.KEYCODE_D -> Shortcut.SITE_TWEAKS
            KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_PLUS -> Shortcut.ZOOM_IN
            KeyEvent.KEYCODE_PAGE_UP -> null
            else -> null
        } else when (keyCode) {
            KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_E -> Shortcut.ADDRESS
            KeyEvent.KEYCODE_K -> Shortcut.QUICK_SWITCHER
            KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_NUMPAD_ADD -> Shortcut.ZOOM_IN
            KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> Shortcut.ZOOM_OUT
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> Shortcut.ZOOM_RESET
            KeyEvent.KEYCODE_F -> Shortcut.FIND
            KeyEvent.KEYCODE_R -> Shortcut.RELOAD
            KeyEvent.KEYCODE_D -> Shortcut.BOOKMARK
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_P -> Shortcut.SAVE_PDF
            KeyEvent.KEYCODE_T -> Shortcut.NEW_TAB
            KeyEvent.KEYCODE_W -> Shortcut.CLOSE_TAB
            KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_PAGE_DOWN -> Shortcut.NEXT_TAB
            KeyEvent.KEYCODE_PAGE_UP -> Shortcut.PREVIOUS_TAB
            KeyEvent.KEYCODE_H -> Shortcut.LIBRARY
            KeyEvent.KEYCODE_COMMA -> Shortcut.SETTINGS
            KeyEvent.KEYCODE_SLASH -> Shortcut.HELP
            else -> null
        }
    }

    /** Called by the activity for each key event. Returns true when the press was a shortcut and the screen took it. */
    fun dispatch(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false
        val shortcut = resolve(event.keyCode, event.isCtrlPressed, event.isShiftPressed, event.isAltPressed) ?: return false
        practice?.let { it(shortcut); return true }
        if (shortcut == Shortcut.TAB_1) tabIndex = tabIndexFor(event.keyCode) ?: 0
        return handler?.invoke(shortcut) ?: false
    }
}

/**
 * Address bar keys that need the tab list, set by the browser screen so the address bar does not need the tabs.
 * [openInNewTab] loads an address in a new tab (Alt+Enter).
 */
object AddressKeys {
    @Volatile var openInNewTab: ((String) -> Unit)? = null

    /** Forgets one address from history (Delete on a highlighted suggestion, or its cross). Set by the browser screen. */
    @Volatile var removeFromHistory: ((String) -> Unit)? = null

    /** Adds ".com" to a single bare word (Ctrl+Enter), as most desktop browsers do. Anything else is returned unchanged. */
    fun completeCom(text: String): String {
        val t = text.trim()
        if (t.isEmpty() || t.any { it == ' ' || it == '.' || it == '/' || it == ':' }) return t
        return "www.$t.com"
    }
}
