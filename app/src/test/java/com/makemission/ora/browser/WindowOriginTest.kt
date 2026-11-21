package com.makemission.ora.browser

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowOriginTest {
    private val fallback = TransformOrigin(0.85f, 1f)

    @Test fun pressBecomesFractionOfWindow() {
        val o = windowOrigin(Offset(500f, 250f), IntSize(1000, 1000), fallback)
        assertEquals(0.5f, o.pivotFractionX, 0.001f)
        assertEquals(0.25f, o.pivotFractionY, 0.001f)
    }

    @Test fun unknownPressUsesFallback() {
        assertEquals(fallback, windowOrigin(Offset.Unspecified, IntSize(1000, 1000), fallback))
        assertEquals(fallback, windowOrigin(Offset(1f, 1f), IntSize.Zero, fallback))
    }

    @Test fun pressOutsideWindowIsClamped() {
        val o = windowOrigin(Offset(-50f, 5000f), IntSize(1000, 1000), fallback)
        assertEquals(0f, o.pivotFractionX, 0f)
        assertEquals(1f, o.pivotFractionY, 0f)
    }
}
