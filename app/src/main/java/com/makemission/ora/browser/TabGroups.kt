package com.makemission.ora.browser

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.Mineral
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** The six colours a tab group can take. The group's name is always shown next to its colour. */
enum class GroupColor(val key: String, val label: String, val color: Color) {
    LICHEN("lichen", "Lichen", Mineral.Lichen),
    EMBER("ember", "Ember", Mineral.Ember),
    OCHRE("ochre", "Ochre", Mineral.Ochre),
    TIDE("tide", "Tide", Mineral.Tide),
    PLUM("plum", "Plum", Mineral.Plum),
    FOG("fog", "Fog", Mineral.Fog);

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: FOG
    }
}

/** A named, coloured set of tabs that belongs to one profile. [order] is its place in the list. */
data class TabGroup(val id: String, val name: String, val color: GroupColor, val collapsed: Boolean, val order: Int)

/** Keeps one profile's tab groups in private preferences, under `ora_tab_groups_<profileId>`. */
class TabGroupStore(context: Context, profileId: String) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ora_tab_groups_$profileId", Context.MODE_PRIVATE)

    fun save(groups: List<TabGroup>) {
        val array = JSONArray()
        groups.forEach {
            array.put(
                JSONObject().put("id", it.id).put("name", it.name).put("color", it.color.key)
                    .put("collapsed", it.collapsed).put("order", it.order)
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun load(): List<TabGroup> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) {
                val o = array.getJSONObject(it)
                TabGroup(o.getString("id"), o.getString("name"), GroupColor.fromKey(o.optString("color")), o.optBoolean("collapsed"), o.optInt("order", it))
            }.sortedBy { it.order }
        } catch (e: JSONException) {
            BrowserLog.error("groups", "Could not read the saved tab groups", e)
            emptyList()
        }
    }

    private companion object { const val KEY = "groups" }
}
