package com.makemission.ora.security

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLockVisibilityTest {
    @Test
    fun lockedPrivateTabShowsTheLock() {
        assertEquals(true, lockScreenVisible(locked = true, wholeApp = false, selectedPrivate = true, unlockWanted = false))
    }

    @Test
    fun askingForAPrivateTabShowsTheLockOnARegularTab() {
        assertEquals(true, lockScreenVisible(locked = true, wholeApp = false, selectedPrivate = false, unlockWanted = true))
    }

    @Test
    fun unlockedOrPlainRegularTabHidesTheLock() {
        assertEquals(false, lockScreenVisible(locked = false, wholeApp = false, selectedPrivate = true, unlockWanted = true))
        assertEquals(false, lockScreenVisible(locked = true, wholeApp = false, selectedPrivate = false, unlockWanted = false))
    }

    @Test
    fun wholeAppLockAlwaysShows() {
        assertEquals(true, lockScreenVisible(locked = true, wholeApp = true, selectedPrivate = false, unlockWanted = false))
    }
}
