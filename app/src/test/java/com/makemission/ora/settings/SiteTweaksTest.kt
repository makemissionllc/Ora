package com.makemission.ora.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteTweaksTest {
    @Test fun setAndRemoveWhenDefault() {
        var m = TweakLogic.with(emptyMap(), "a.com", SiteTweak(dark = true))
        assertEquals(1, m["a.com"]!!.count)
        m = TweakLogic.with(m, "a.com", SiteTweak())
        assertTrue(m.isEmpty())
    }

    @Test fun unknownFontDropped() = assertEquals("", TweakLogic.with(emptyMap(), "a.com", SiteTweak(wide = true, font = "comic"))["a.com"]!!.font)

    @Test fun emptySiteIgnored() = assertTrue(TweakLogic.with(emptyMap(), "", SiteTweak(dark = true)).isEmpty())

    @Test fun summarySorted() {
        val m = TweakLogic.with(TweakLogic.with(emptyMap(), "b.com", SiteTweak(dark = true)), "a.com", SiteTweak(font = "serif"))
        assertEquals(listOf("a.com", "b.com"), TweakLogic.summary(m).map { it.first })
    }
}
