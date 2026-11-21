package com.makemission.ora.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticsTest {
    @Test fun offPlaysNothing() {
        Feel.entries.forEach { assertFalse(it.name, feelAllowed(HapticStrength.OFF, it)) }
    }

    @Test fun fullPlaysEverything() {
        Feel.entries.forEach { assertTrue(it.name, feelAllowed(HapticStrength.FULL, it)) }
    }

    @Test fun subtleSkipsOnlyTheLightTicks() {
        val skipped = Feel.entries.filterNot { feelAllowed(HapticStrength.SUBTLE, it) }
        assertEquals(listOf(Feel.TICK, Feel.SELECT), skipped)
    }

    @Test fun unknownSavedValueFallsBackToFull() {
        assertEquals(HapticStrength.FULL, HapticStrength.fromKey(null))
        assertEquals(HapticStrength.FULL, HapticStrength.fromKey("loud"))
        assertEquals(HapticStrength.SUBTLE, HapticStrength.fromKey("subtle"))
    }
}
