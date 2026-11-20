package com.makemission.ora.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageNameTest {
    @Test
    fun usesTheServerTypeBeforeTheName() {
        assertEquals("photo" to "png", imageNameParts("https://example.com/a/photo.jpg?x=1", "image/png; charset=binary"))
    }

    @Test
    fun fallsBackToTheNameThenJpg() {
        assertEquals("cat" to "webp", imageNameParts("https://example.com/cat.webp", null))
        assertEquals("image" to "jpg", imageNameParts("https://example.com/", null))
    }

    @Test
    fun removesCharactersThatCannotBeInAFileName() {
        assertEquals("a b" to "png", imageNameParts("https://example.com/a:b.png", null))
    }
}
