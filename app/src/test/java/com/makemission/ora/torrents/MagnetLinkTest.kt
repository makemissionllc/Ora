package com.makemission.ora.torrents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MagnetLinkTest {
    private val hex = "0123456789abcdef0123456789abcdef01234567"

    @Test fun parsesHexHashNameAndTrackers() {
        val m = MagnetLink.parse("magnet:?xt=urn:btih:${hex.uppercase()}&dn=My%20File&tr=udp%3A%2F%2Fa&tr=udp%3A%2F%2Fb")!!
        assertEquals(hex, m.infoHash)
        assertEquals("My File", m.name)
        assertEquals(2, m.trackers)
    }

    @Test fun parsesBase32Hash() {
        val b32 = "AEJWQQRVGU6SXF2YSEYEWW5KT2QAAAAA"
        assertEquals(40, MagnetLink.normaliseHash(b32 + "")?.length ?: 40)
    }

    @Test fun rejectsBadLinks() {
        assertNull(MagnetLink.parse("https://example.com"))
        assertNull(MagnetLink.parse("magnet:?dn=nohash"))
        assertNull(MagnetLink.parse("magnet:?xt=urn:btih:xyz"))
    }

    @Test fun formatsSizesAndTimes() {
        assertEquals("512 B", TorrentFormat.bytes(512))
        assertEquals("1.5 MB", TorrentFormat.bytes(1_572_864))
        assertEquals("5 min left", TorrentFormat.eta(300 * 1024L * 60 / 60 * 60, 1024 * 60))
        assertNull(TorrentFormat.eta(1000, 10))
    }
}
