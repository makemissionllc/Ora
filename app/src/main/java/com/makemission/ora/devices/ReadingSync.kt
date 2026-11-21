package com.makemission.ora.devices

import android.content.Context
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.profiles.ProfilesRepository
import com.makemission.ora.readinglist.ReadingItem
import com.makemission.ora.readinglist.ReadingListRepository
import com.makemission.ora.readinglist.ReadingState
import org.json.JSONArray
import org.json.JSONObject

/**
 * Keeps the reading list the same on every linked device. Only the first (default) profile is synced, because profile
 * ids are made per device and do not match across devices. Each side sends its list and removals, merges what comes
 * back ([com.makemission.ora.readinglist.ReadingMerge]) and ends with the same list. Addresses are never logged, only counts.
 */
object ReadingSync {
    fun repo(context: Context): ReadingListRepository = ReadingListRepository.shared(context, ProfilesRepository.DEFAULT_ID)

    fun toJson(state: ReadingState): JSONObject {
        val items = JSONArray()
        state.items.forEach { items.put(JSONObject().put("url", it.url).put("title", it.title).put("addedAt", it.addedAt).put("read", it.read).put("readAt", it.readAt)) }
        val removed = JSONObject()
        state.removed.forEach { (url, time) -> removed.put(url, time) }
        return JSONObject().put("items", items).put("removed", removed)
    }

    /** Reads a list from a message. Anything that is not a plain web address is dropped, so a paired device cannot plant other links. */
    fun fromJson(body: JSONObject): ReadingState {
        val array = body.optJSONArray("items") ?: JSONArray()
        val items = (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val url = o.optString("url")
            if (!TabInbox.isWebAddress(url)) null
            else ReadingItem(url, o.optString("title", url).take(200), o.optLong("addedAt"), o.optBoolean("read"), o.optLong("readAt"))
        }
        val removedObject = body.optJSONObject("removed") ?: JSONObject()
        val removed = removedObject.keys().asSequence().associateWith { removedObject.optLong(it) }
        return ReadingState(items, removed)
    }

    /** The handler for a "reading" message: merge the other device's list, reply with the merged one. */
    fun handle(context: Context, body: JSONObject): JSONObject {
        val repo = repo(context)
        repo.mergeFrom(fromJson(body))
        return toJson(repo.state())
    }

    /** Sends this device's list to [deviceId], merges the answer and says how many pages arrived. Blocking: call off the main thread. */
    fun syncWith(context: Context, link: DeviceLink, deviceId: String, announce: Boolean = true) {
        val device = link.store.find(deviceId) ?: return
        val repo = repo(context)
        val reply = link.ask(deviceId, DeviceHub.TYPE_READING, toJson(repo.state())) ?: return
        if (!reply.has("items")) return
        val added = repo.mergeFrom(fromJson(reply))
        if (added > 0 && announce) InAppNotices.show("Synced $added ${if (added == 1) "page" else "pages"} from ${device.name}")
    }

    /** Syncs with every paired device that is on the network. */
    fun syncAll(context: Context, link: DeviceLink, announce: Boolean = true) {
        link.store.devices.value.filter { link.isOnline(it.id) }.forEach {
            try { syncWith(context, link, it.id, announce) } catch (e: Exception) { BrowserLog.warn("devices", "Reading list sync with \"${it.name}\" failed", e) }
        }
    }
}
