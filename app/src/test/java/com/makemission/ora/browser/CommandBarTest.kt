package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandBarTest {
    @Test fun detectsPrefix() {
        assertTrue(CommandBar.isCommand(">"))
        assertTrue(CommandBar.isCommand("  > new"))
        assertFalse(CommandBar.isCommand("new tab"))
    }

    @Test fun emptyQueryListsAll() = assertEquals(CommandBar.commands, CommandBar.matches(">"))

    @Test fun startsWithRanksFirst() {
        assertEquals("New tab", CommandBar.matches(">new").first().name)
        assertEquals("Save as PDF", CommandBar.matches("> pdf").single().name)
    }

    @Test fun wordsAnyOrder() = assertEquals("Reopen closed tab", CommandBar.matches(">closed reopen").single().name)

    @Test fun noMatch() = assertTrue(CommandBar.matches(">zzz").isEmpty())

    @Test fun suggestionsUseCommands() {
        val list = addressSuggestions(">sett", emptyList())
        assertEquals(SuggestionKind.COMMAND, list.single().kind)
        assertEquals(">Settings", list.single().target)
    }
}
