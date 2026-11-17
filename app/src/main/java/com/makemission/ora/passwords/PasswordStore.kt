package com.makemission.ora.passwords

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Saved logins for the browser. GeckoView asks for them through [PasswordStorageDelegate];
 * the Passwords page lists and deletes them.
 *
 * The file holds the logins encrypted with an AES-GCM key kept in the Android Keystore,
 * so the file alone cannot be read off the device.
 */
object PasswordStore {

    data class Login(
        val guid: String,
        /** Page origin the login belongs to, such as https://example.com. */
        val origin: String,
        val username: String,
        val password: String,
        val updatedAt: Long,
    ) {
        val host: String get() = normalizeHost(origin)
    }

    private const val FILE_NAME = "passwords.bin"
    private const val KEY_ALIAS = "ora_passwords"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val CIPHER = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128

    private val lock = Any()
    private var file: File? = null
    private val _logins = MutableStateFlow<List<Login>>(emptyList())

    /** All saved logins, most recently saved first. */
    val logins: StateFlow<List<Login>> = _logins.asStateFlow()

    /** Loads the saved logins once. Safe to call again. */
    fun init(context: Context) {
        synchronized(lock) {
            if (file != null) return
            val target = File(context.applicationContext.filesDir, FILE_NAME)
            file = target
            try {
                _logins.value = read(target)
                BrowserLog.info("passwords", "Loaded ${_logins.value.size} saved logins")
            } catch (e: Exception) {
                // Keep the unreadable file aside so it is not overwritten, and start empty.
                BrowserLog.error("passwords", "Saved passwords could not be read; starting empty", e)
                target.renameTo(File(target.path + ".unreadable"))
                _logins.value = emptyList()
            }
        }
    }

    /** Logins saved for the same host as [origin] or [host], with or without a leading www. */
    fun forHost(origin: String): List<Login> {
        val host = normalizeHost(origin)
        return _logins.value.filter { it.host == host }
    }

    /** Saves a login. An existing login for the same origin and username is updated in place. */
    fun save(origin: String, username: String, password: String): Login? {
        if (password.isEmpty()) return null
        synchronized(lock) {
            val current = _logins.value
            val existing = current.firstOrNull { it.origin == origin && it.username == username }
            val saved = Login(
                guid = existing?.guid ?: UUID.randomUUID().toString(),
                origin = origin,
                username = username,
                password = password,
                updatedAt = System.currentTimeMillis(),
            )
            val next = listOf(saved) + current.filterNot { it.guid == saved.guid }
            if (!write(next)) return null
            _logins.value = next
            BrowserLog.info("passwords", if (existing == null) "Saved a login for ${saved.host}" else "Updated the login for ${saved.host}")
            return saved
        }
    }

    /** Deletes one login. Returns false if the file could not be written; the list is then unchanged. */
    fun delete(guid: String): Boolean {
        synchronized(lock) {
            val current = _logins.value
            val next = current.filterNot { it.guid == guid }
            if (next.size == current.size) return true
            if (!write(next)) return false
            _logins.value = next
            BrowserLog.info("passwords", "Deleted a saved login")
            return true
        }
    }

    private fun write(logins: List<Login>): Boolean {
        val target = file ?: return false
        return try {
            val json = JSONArray()
            logins.forEach { login ->
                json.put(
                    JSONObject()
                        .put("guid", login.guid)
                        .put("origin", login.origin)
                        .put("username", login.username)
                        .put("password", login.password)
                        .put("updatedAt", login.updatedAt)
                )
            }
            val tmp = File(target.path + ".tmp")
            tmp.writeText(encrypt(json.toString().toByteArray(Charsets.UTF_8)))
            if (!tmp.renameTo(target)) error("Could not replace the saved passwords file")
            true
        } catch (e: Exception) {
            BrowserLog.error("passwords", "Could not save passwords", e)
            false
        }
    }

    private fun read(target: File): List<Login> {
        if (!target.exists()) return emptyList()
        val json = JSONArray(String(decrypt(target.readText()), Charsets.UTF_8))
        return (0 until json.length()).map { i ->
            val item = json.getJSONObject(i)
            Login(
                guid = item.getString("guid"),
                origin = item.getString("origin"),
                username = item.getString("username"),
                password = item.getString("password"),
                updatedAt = item.getLong("updatedAt"),
            )
        }.sortedByDescending { it.updatedAt }
    }

    private fun encrypt(plain: ByteArray): String {
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encoded = Base64.encodeToString(cipher.doFinal(plain), Base64.NO_WRAP)
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        return "$iv:$encoded"
    }

    private fun decrypt(stored: String): ByteArray {
        val parts = stored.split(":")
        require(parts.size == 2) { "Unexpected saved passwords format" }
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP))
        )
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP))
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /** Host without scheme, port or a leading www., so https://www.example.com matches example.com. */
    fun normalizeHost(origin: String): String =
        (Uri.parse(origin).host ?: origin).lowercase().removePrefix("www.")
}
