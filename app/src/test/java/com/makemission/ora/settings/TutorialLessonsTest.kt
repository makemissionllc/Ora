package com.makemission.ora.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialLessonsTest {
    @Test fun keyboardLessonIsTabletOnly() {
        assertTrue(tutorialLessons(true).any { it.id == "keys" })
        assertFalse(tutorialLessons(false).any { it.id == "keys" })
    }

    @Test fun lessonIdsAreUnique() {
        val ids = tutorialLessons(true).map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun nextLessonSkipsFinishedOnes() {
        val l = tutorialLessons(false)
        assertEquals(l[1].id, nextLesson(l, setOf(l[0].id))!!.id)
        assertNull(nextLesson(l, l.map { it.id }.toSet()))
    }
}
