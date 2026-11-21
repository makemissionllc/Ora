package com.makemission.ora.devices

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * The small amount of cryptography behind device linking. Two devices pair once with a short code; from then on
 * every message between them is sealed with AES-256-GCM under a random secret only those two devices hold, so
 * other people on the same Wi-Fi can neither read nor forge it. Pure functions, no Android types, so they are
 * tested on the JVM.
 */
object DeviceLinkCrypto {
    private val random = SecureRandom()
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val KDF_ROUNDS = 30_000
    private val PAIR_SALT = "ora-device-link-pairing-v1".toByteArray()

    /** A fresh 256-bit secret, shared by two devices when they pair. */
    fun newSecret(): ByteArray = ByteArray(32).also { random.nextBytes(it) }

    /** Six digits shown on the device that is waiting to be paired. */
    fun newPairingCode(): String = (random.nextInt(900_000) + 100_000).toString()

    /** Turns the typed pairing code into the key that protects the one pairing exchange. */
    fun keyFromCode(code: String): ByteArray {
        val spec = PBEKeySpec(code.filter { it.isDigit() }.toCharArray(), PAIR_SALT, KDF_ROUNDS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    /** Encrypts and authenticates [plain]. The result is one line of text: a random nonce followed by the ciphertext. */
    fun seal(key: ByteArray, plain: String): String {
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return Base64.getEncoder().withoutPadding().encodeToString(iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    /** The text inside [sealed], or null when it was not sealed with [key] or was changed on the way. */
    fun open(key: ByteArray, sealed: String): String? = try {
        val bytes = Base64.getDecoder().decode(sealed)
        if (bytes.size <= IV_BYTES) null else {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, IV_BYTES)))
            String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
        }
    } catch (e: GeneralSecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    fun encode(bytes: ByteArray): String = Base64.getEncoder().withoutPadding().encodeToString(bytes)
    fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)
}
