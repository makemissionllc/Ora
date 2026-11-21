package com.makemission.ora.adaptive

import android.content.Context
import android.content.SharedPreferences

/** How much Ora may tune itself from the way it is used. */
enum class AdaptiveMode(val key: String, val label: String, val detail: String) {
    OFF("off", "Off", "Ora never changes anything by itself and shows no tuning ideas."),
    SUGGEST("suggest", "Suggest", "Ora shows ideas from how you use it, and you choose what to apply."),
    AUTO("auto", "Automatic", "Ora applies small, safe changes by itself, tells you, and lets you undo each one.");

    companion object {
        fun fromKey(key: String?): AdaptiveMode = entries.firstOrNull { it.key == key } ?: SUGGEST
    }
}

/** What an insight can do about itself. Only changes that are small and reversible are offered. */
enum class Fix(val label: String) {
    SHORTER_PULL("Make pulling easier"),
    PRELOAD_WIFI("Look ahead on Wi-Fi only"),
}

/** One plain-language finding from the log or from the way Ora is used. */
data class Insight(val id: String, val title: String, val detail: String, val fix: Fix? = null, val problem: Boolean = true)

/** Values the running app reads without touching storage. [com.makemission.ora.settings.SettingsRepository] writes them. */
object Adaptive {
    /** Share of the normal pull-to-refresh distance that is needed. 1 is the designed distance. */
    @Volatile
    var pullScale: Float = 1f

    /** A pull never needs less than this share of the designed distance, so a brush of the finger cannot reload. */
    const val PULL_MIN = 0.6f
}

/**
 * Learns how far this person really pulls. Stores only percentages of the designed pull distance and a count,
 * never pages, so it is safe to keep. Early releases are pulls that stopped short of the refresh point.
 */
object PullLearner {
    private const val PREFS = "ora_adaptive"
    private const val KEY_EARLY = "pull_early"
    private const val KEY_OK = "pull_ok"
    private const val KEEP = 30
    private const val MIN_EARLY = 8

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private fun early(): List<Int> = prefs?.getString(KEY_EARLY, "").orEmpty().split(',').mapNotNull { it.toIntOrNull() }

    /** A pull that stopped at [percent] percent of the designed distance and was let go. */
    fun recordEarly(percent: Int) {
        val store = prefs ?: return
        val next = (early() + percent.coerceIn(0, 100)).takeLast(KEEP)
        store.edit().putString(KEY_EARLY, next.joinToString(",")).apply()
    }

    /** A pull that reached the refresh point. */
    fun recordRegistered() {
        val store = prefs ?: return
        store.edit().putInt(KEY_OK, (store.getInt(KEY_OK, 0) + 1).coerceAtMost(1000)).apply()
    }

    /** Forgets what was learned, so the next idea comes from pulls made after a change. */
    fun reset() {
        prefs?.edit()?.remove(KEY_EARLY)?.remove(KEY_OK)?.apply()
    }

    data class Pulls(val early: Int, val registered: Int)

    fun counts(): Pulls = Pulls(early().size, prefs?.getInt(KEY_OK, 0) ?: 0)

    /**
     * A smaller share of the designed distance that would have caught most of the pulls that stopped short, or null
     * when there is too little to go on, pulling already works, or it is already that easy. The share is the 75th
     * percentile of the early pulls, never below [Adaptive.PULL_MIN] and never above 95 percent.
     */
    fun recommendedScale(current: Float): Float? {
        val early = early()
        val ok = prefs?.getInt(KEY_OK, 0) ?: 0
        if (early.size < MIN_EARLY) return null
        if (early.size.toFloat() / (early.size + ok) < 0.25f) return null
        val p75 = early.sorted()[((early.size - 1) * 0.75f).toInt()] / 100f
        val scale = p75.coerceIn(Adaptive.PULL_MIN, 0.95f)
        return if (scale < current - 0.02f) scale else null
    }
}
