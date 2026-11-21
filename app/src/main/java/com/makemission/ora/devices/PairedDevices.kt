package com.makemission.ora.devices

import android.content.Context
import android.os.Build
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A device this one has paired with. [secret] is the shared key (base64); it never leaves the two devices. */
data class PairedDevice(
    val id: String,
    val name: String,
    val secret: String,
    val pairedAt: Long,
    /** When something was last sent to it, so the Send list can put the device you use most at the top. */
    val lastUsed: Long = 0L,
)

/** One message waiting for a device that was not reachable. It is sent when the device next appears, or dropped once [expiresAt] passes. */
data class QueuedMessage(val to: String, val id: String, val type: String, val body: String, val expiresAt: Long)

/**
 * Who this device is and which devices it is paired with, kept in private preferences (`ora_devices`).
 * Also holds the outbox for devices that were asleep or away. Nothing here is ever sent to a server.
 */
class PairedDevices(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ora_devices", Context.MODE_PRIVATE)

    /** This device's id, made once and never changed. It is how other devices recognise this one. */
    val deviceId: String = prefs.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_ID, it).apply() }

    private val _devices = MutableStateFlow(loadDevices())
    val devices: StateFlow<List<PairedDevice>> = _devices.asStateFlow()

    private val _queue = MutableStateFlow(loadQueue())
    val queue: StateFlow<List<QueuedMessage>> = _queue.asStateFlow()

    fun find(id: String): PairedDevice? = _devices.value.firstOrNull { it.id == id }

    /** Adds the device, or replaces it when the same device pairs again (new secret, new name). */
    fun add(device: PairedDevice) {
        saveDevices(_devices.value.filterNot { it.id == device.id } + device)
    }

    fun forget(id: String) {
        saveDevices(_devices.value.filterNot { it.id == id })
        saveQueue(_queue.value.filterNot { it.to == id })
    }

    fun touch(id: String, time: Long = System.currentTimeMillis()) {
        saveDevices(_devices.value.map { if (it.id == id) it.copy(lastUsed = time) else it })
    }

    fun enqueue(message: QueuedMessage) {
        saveQueue(_queue.value + message)
    }

    fun dequeue(id: String) {
        if (_queue.value.any { it.id == id }) saveQueue(_queue.value.filterNot { it.id == id })
    }

    /** Drops queued messages that have waited too long. Returns how many went. */
    fun dropExpired(now: Long = System.currentTimeMillis()): Int {
        val keep = _queue.value.filter { it.expiresAt > now }
        val gone = _queue.value.size - keep.size
        if (gone > 0) saveQueue(keep)
        return gone
    }

    private fun saveDevices(list: List<PairedDevice>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("secret", it.secret).put("at", it.pairedAt).put("used", it.lastUsed)) }
        prefs.edit().putString(KEY_DEVICES, array.toString()).apply()
        _devices.value = list
    }

    private fun saveQueue(list: List<QueuedMessage>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("to", it.to).put("id", it.id).put("type", it.type).put("body", it.body).put("exp", it.expiresAt)) }
        prefs.edit().putString(KEY_QUEUE, array.toString()).apply()
        _queue.value = list
    }

    private fun loadDevices(): List<PairedDevice> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                PairedDevice(o.getString("id"), o.getString("name"), o.getString("secret"), o.optLong("at"), o.optLong("used"))
            }
        }.getOrElse {
            BrowserLog.warn("devices", "The paired device list could not be read, starting empty", it)
            emptyList()
        }
    }

    private fun loadQueue(): List<QueuedMessage> {
        val raw = prefs.getString(KEY_QUEUE, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                QueuedMessage(o.getString("to"), o.getString("id"), o.getString("type"), o.getString("body"), o.getLong("exp"))
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val KEY_ID = "device_id"
        private const val KEY_DEVICES = "devices"
        private const val KEY_QUEUE = "queue"

        /** What a new device is called until the person renames it: the phone or tablet model. */
        fun defaultName(): String = Build.MODEL?.takeIf { it.isNotBlank() } ?: "This device"
    }
}
