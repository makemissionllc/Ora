package com.makemission.ora.downloads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class DownloadSafetyTest {

    private val abc = DownloadSafety.hashes(ByteArrayInputStream("abc".toByteArray()))

    @Test
    fun executableTypesAreRiskyWhateverTheCase() {
        assertTrue(DownloadSafety.isRisky("tool-installer.apk"))
        assertTrue(DownloadSafety.isRisky("SETUP.EXE"))
        assertTrue(DownloadSafety.isRisky("run-me.sh"))
        assertFalse(DownloadSafety.isRisky("notes.pdf"))
        assertFalse(DownloadSafety.isRisky("archive.tar.gz"))
        assertFalse(DownloadSafety.isRisky("readme"))
    }

    @Test
    fun plainHttpIsFlaggedAndHttpsIsNot() {
        assertTrue(DownloadSafety.isPlainHttp("http://example.org/a.zip"))
        assertTrue(DownloadSafety.isPlainHttp("HTTP://example.org/a.zip"))
        assertFalse(DownloadSafety.isPlainHttp("https://example.org/a.zip"))
    }

    @Test
    fun pastedHashLengthPicksTheAlgorithm() {
        assertEquals("SHA-256", DownloadSafety.algorithmFor("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))
        assertEquals("MD5", DownloadSafety.algorithmFor("900150983CD24FB0D6963F7D28E17F72"))
        assertNull(DownloadSafety.algorithmFor("not a hash"))
        assertNull(DownloadSafety.algorithmFor("abc"))
        assertNull(DownloadSafety.algorithmFor(""))
    }

    @Test
    fun spacesAndColonsInAPastedHashAreIgnored() {
        val spaced = "BA:78 16BF 8F01 CFEA 4141 40DE 5DAE 2223 B003 61A3 9617 7A9C B410 FF61 F200 15AD"
        assertEquals("SHA-256", DownloadSafety.algorithmFor(spaced))
        assertEquals(true, DownloadSafety.compare(spaced, abc))
    }

    @Test
    fun bothHashesOfAKnownInputMatchTheStandardValues() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", abc.sha256)
        assertEquals("900150983cd24fb0d6963f7d28e17f72", abc.md5)
    }

    @Test
    fun aMatchingHashOfEitherKindIsTrueAndADifferentOneIsFalse() {
        assertEquals(true, DownloadSafety.compare("900150983cd24fb0d6963f7d28e17f72", abc))
        assertEquals(true, DownloadSafety.compare(abc.sha256, abc))
        assertEquals(false, DownloadSafety.compare("900150983cd24fb0d6963f7d28e17f73", abc))
    }

    @Test
    fun anIncompleteHashHasNothingToCompareYet() {
        assertNull(DownloadSafety.compare("ba7816bf8f01", abc))
        assertNull(DownloadSafety.compare("", abc))
    }
}
