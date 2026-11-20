package com.makemission.ora.browser

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyShortcutsTest {
    private fun r(key: Int, ctrl: Boolean = false, shift: Boolean = false, alt: Boolean = false) = KeyShortcuts.resolve(key, ctrl, shift, alt)

    @Test fun ctrlCombosMapToActions() {
        assertEquals(Shortcut.ADDRESS, r(KeyEvent.KEYCODE_L, ctrl = true))
        assertEquals(Shortcut.NEW_TAB, r(KeyEvent.KEYCODE_T, ctrl = true))
        assertEquals(Shortcut.REOPEN_TAB, r(KeyEvent.KEYCODE_T, ctrl = true, shift = true))
        assertEquals(Shortcut.NEXT_TAB, r(KeyEvent.KEYCODE_TAB, ctrl = true))
        assertEquals(Shortcut.PREVIOUS_TAB, r(KeyEvent.KEYCODE_TAB, ctrl = true, shift = true))
        assertEquals(Shortcut.HELP, r(KeyEvent.KEYCODE_SLASH, ctrl = true))
    }

    @Test fun newKeyboardShortcuts() {
        assertEquals(Shortcut.QUICK_SWITCHER, r(KeyEvent.KEYCODE_K, ctrl = true))
        assertEquals(Shortcut.DUPLICATE_TAB, r(KeyEvent.KEYCODE_K, ctrl = true, shift = true))
        assertEquals(Shortcut.COPY_ADDRESS, r(KeyEvent.KEYCODE_C, ctrl = true, shift = true))
        assertEquals(Shortcut.ZOOM_IN, r(KeyEvent.KEYCODE_EQUALS, ctrl = true))
        assertEquals(Shortcut.ZOOM_IN, r(KeyEvent.KEYCODE_PLUS, ctrl = true, shift = true))
        assertEquals(Shortcut.ZOOM_OUT, r(KeyEvent.KEYCODE_MINUS, ctrl = true))
        assertEquals(Shortcut.ZOOM_RESET, r(KeyEvent.KEYCODE_0, ctrl = true))
        assertNull(r(KeyEvent.KEYCODE_C, ctrl = true))
    }

    @Test fun altHomeIsTheStartPage() {
        assertEquals(Shortcut.START_PAGE, r(KeyEvent.KEYCODE_MOVE_HOME, alt = true))
        assertNull(r(KeyEvent.KEYCODE_MOVE_HOME))
    }

    @Test fun plainTypingIsNeverTaken() {
        assertNull(r(KeyEvent.KEYCODE_L))
        assertNull(r(KeyEvent.KEYCODE_T, shift = true))
        assertNull(r(KeyEvent.KEYCODE_TAB))
        assertNull(r(KeyEvent.KEYCODE_DPAD_LEFT))
    }

    @Test fun altArrowsAreBackAndForward() {
        assertEquals(Shortcut.BACK, r(KeyEvent.KEYCODE_DPAD_LEFT, alt = true))
        assertEquals(Shortcut.FORWARD, r(KeyEvent.KEYCODE_DPAD_RIGHT, alt = true))
    }

    @Test fun functionKeysAndEscape() {
        assertEquals(Shortcut.RELOAD, r(KeyEvent.KEYCODE_F5))
        assertEquals(Shortcut.HARD_RELOAD, r(KeyEvent.KEYCODE_F5, ctrl = true, shift = true))
        assertEquals(Shortcut.ADDRESS, r(KeyEvent.KEYCODE_F6))
        assertEquals(Shortcut.HELP, r(KeyEvent.KEYCODE_F1))
        assertEquals(Shortcut.ESCAPE, r(KeyEvent.KEYCODE_ESCAPE))
    }

    @Test fun digitsPickTabs() {
        assertEquals(Shortcut.TAB_1, r(KeyEvent.KEYCODE_3, ctrl = true))
        assertEquals(2, KeyShortcuts.tabIndexFor(KeyEvent.KEYCODE_3))
        assertEquals(Shortcut.LAST_TAB, r(KeyEvent.KEYCODE_9, ctrl = true))
        assertEquals(-1, KeyShortcuts.tabIndexFor(KeyEvent.KEYCODE_9))
        assertNull(KeyShortcuts.tabIndexFor(KeyEvent.KEYCODE_A))
    }

    @Test fun otherWaysToReachTheAddressBar() {
        assertEquals(Shortcut.ADDRESS, r(KeyEvent.KEYCODE_E, ctrl = true))
        assertEquals(Shortcut.ADDRESS, r(KeyEvent.KEYCODE_D, alt = true))
        assertEquals(Shortcut.BOOKMARK, r(KeyEvent.KEYCODE_D, ctrl = true))
    }

    @Test fun ctrlEnterCompletesABareWord() {
        assertEquals("www.ora.com", AddressKeys.completeCom("ora"))
        assertEquals("ora.org", AddressKeys.completeCom("ora.org"))
        assertEquals("two words", AddressKeys.completeCom("two words"))
    }

    @Test fun sessionShortcuts() {
        assertEquals(Shortcut.SAVE_SESSION, r(KeyEvent.KEYCODE_S, ctrl = true, shift = true))
        assertEquals(Shortcut.SESSIONS, r(KeyEvent.KEYCODE_O, ctrl = true, shift = true))
        assertEquals(Shortcut.SAVE_PDF, r(KeyEvent.KEYCODE_S, ctrl = true))
    }
}
