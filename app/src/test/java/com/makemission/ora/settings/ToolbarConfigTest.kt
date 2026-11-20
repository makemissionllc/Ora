package com.makemission.ora.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolbarConfigTest {
    @Test
    fun noSavedValueMeansClassic() {
        assertEquals(ToolbarConfig.CLASSIC, ToolbarConfig.parse(null))
    }

    @Test
    fun unknownIdsAreDroppedNotFatal() {
        assertEquals(listOf(ToolbarItem.BACK, ToolbarItem.TABS), ToolbarConfig.parse("back,time_travel,,tabs"))
    }

    @Test
    fun repeatsAndExtraButtonsAreDropped() {
        assertEquals(listOf(ToolbarItem.BACK), ToolbarConfig.parse("back,back"))
        assertEquals(5, ToolbarConfig.parse("back,forward,tabs,share,bookmark,home,reload").size)
    }

    @Test
    fun editingAPresetBecomesCustom() {
        assertEquals("Reading", ToolbarConfig.presetName(ToolbarConfig.READING))
        assertEquals("Custom", ToolbarConfig.presetName(ToolbarConfig.READING.reversed()))
    }

    @Test
    fun encodeAndParseRoundTrip() {
        assertEquals(ToolbarConfig.MINIMAL, ToolbarConfig.parse(ToolbarConfig.encode(ToolbarConfig.MINIMAL)))
    }
}
