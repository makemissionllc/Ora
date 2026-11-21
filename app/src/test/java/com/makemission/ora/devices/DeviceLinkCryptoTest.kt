package com.makemission.ora.devices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLinkCryptoTest {
    @Test fun sealedTextOpensWithTheSameKey() {
        val key = DeviceLinkCrypto.newSecret()
        assertEquals("hello", DeviceLinkCrypto.open(key, DeviceLinkCrypto.seal(key, "hello")))
    }

    @Test fun anotherKeyOrAChangedMessageIsRefused() {
        val key = DeviceLinkCrypto.newSecret()
        val sealed = DeviceLinkCrypto.seal(key, "hello")
        assertNull(DeviceLinkCrypto.open(DeviceLinkCrypto.newSecret(), sealed))
        val flipped = (if (sealed.last() == 'A') sealed.dropLast(1) + "B" else sealed.dropLast(1) + "A")
        assertNull(DeviceLinkCrypto.open(key, flipped))
        assertNull(DeviceLinkCrypto.open(key, "not base64 !!"))
    }

    @Test fun theSameCodeGivesTheSameKeyAndADifferentCodeDoesNot() {
        assertTrue(DeviceLinkCrypto.keyFromCode("123456").contentEquals(DeviceLinkCrypto.keyFromCode("123 456")))
        assertFalse(DeviceLinkCrypto.keyFromCode("123456").contentEquals(DeviceLinkCrypto.keyFromCode("123457")))
    }

    @Test fun pairingCodeIsSixDigits() {
        repeat(50) { assertEquals(6, DeviceLinkCrypto.newPairingCode().length) }
    }

    @Test fun replayGuardAcceptsOnceAndRejectsStale() {
        val guard = ReplayGuard(maxAgeMs = 1000)
        assertTrue(guard.accept("a", 5000, now = 5000))
        assertFalse(guard.accept("a", 5000, now = 5000))
        assertFalse(guard.accept("b", 1000, now = 5000))
        assertTrue(guard.accept("c", 5500, now = 5000))
    }

    @Test fun onlyWebAddressesAreAccepted() {
        assertTrue(TabInbox.isWebAddress("https://example.com/a"))
        assertTrue(TabInbox.isWebAddress("HTTP://example.com"))
        assertFalse(TabInbox.isWebAddress("javascript:alert(1)"))
        assertFalse(TabInbox.isWebAddress("file:///sdcard/x"))
        assertFalse(TabInbox.isWebAddress("intent://x#Intent;end"))
        assertNotNull(DeviceLinkCrypto.decode(DeviceLinkCrypto.encode(byteArrayOf(1, 2, 3))))
    }
}
