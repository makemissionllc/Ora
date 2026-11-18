package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeSpaceTest {
    // Pairs of (name, isPrivate), in tab order.
    private val tabs = listOf("a" to false, "p1" to true, "b" to false, "p2" to true, "c" to false)

    @Test
    fun normalSwipesSkipPrivateTabs() {
        assertEquals(listOf("a", "b", "c"), tabsInSpace(tabs, { it.second }, false).map { it.first })
    }

    @Test
    fun privateSwipesSkipNormalTabs() {
        assertEquals(listOf("p1", "p2"), tabsInSpace(tabs, { it.second }, true).map { it.first })
    }

    @Test
    fun aSpaceWithOneTabCannotSwitch() {
        val onlyPrivate = listOf("a" to false, "p1" to true)
        assertEquals(1, tabsInSpace(onlyPrivate, { it.second }, true).size)
    }
}
