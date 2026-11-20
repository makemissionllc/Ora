package com.makemission.ora.browser

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Half-size page pictures for the tab switch, about 24 MB in all, least recently used first out. Memory only, so a
 * private tab's picture never reaches disk; [retainOnly] drops the pictures of tabs that no longer exist.
 */
object ThumbnailCache {
    private val cache = object : LruCache<Long, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.allocationByteCount
    }

    fun get(tabId: Long): Bitmap? = cache.get(tabId)

    /** Stores a half-size copy of [full]. The caller keeps and frees [full]. */
    fun putHalf(tabId: Long, full: Bitmap): Bitmap {
        val half = Bitmap.createScaledBitmap(full, maxOf(1, full.width / 2), maxOf(1, full.height / 2), true)
        cache.put(tabId, half)
        return half
    }

    fun retainOnly(ids: Set<Long>) {
        cache.snapshot().keys.filter { it !in ids }.forEach { cache.remove(it) }
    }
}

internal class Page(val tabId: Long, val label: String, val bitmap: ImageBitmap?)

/**
 * The tab switch. A sideways drag on the address bar or bottom toolbar lifts the page into a capsule, the neighbour
 * tabs peek in either side, and release settles the nearest one before it grows back to full screen. The pictures are
 * bitmaps, so the new GeckoSession is swapped in behind the overlay and no blank frame shows.
 */
class TabSwitchState(private val scope: CoroutineScope, private val reducedMotion: () -> Boolean) {
    /** Set from the screen each composition. */
    var enabled = false
    var tabs: BrowserTabs? = null
    var capture: (((Bitmap?) -> Unit) -> Unit)? = null
    var pageWidthPx = 0f
    var gapPx = 0f

    internal var pages by mutableStateOf<List<Page>>(emptyList())
    internal var centerIndex by mutableStateOf(0)
    internal var visible by mutableStateOf(false)
    internal val drag = Animatable(0f)
    internal val lift = Animatable(0f)
    internal val alpha = Animatable(1f)
    internal var engaged = false
        private set

    private var fullCapture: Bitmap? = null
    private var captureDone = false
    private var captureStartMs = 0L
    private var pending: Pair<Long, CompletableDeferred<Unit>>? = null
    private var switchStartMs = 0L
    private val step get() = pageWidthPx * SCALE + gapPx

    /** Touch down on the toolbar or capsule: start the capture now, so it is ready when the lift starts. */
    fun touch() {
        if (!enabled || visible) return
        val tabsNow = tabs ?: return
        tabsNow.selected ?: return
        fullCapture = null
        captureDone = false
        captureStartMs = SystemClock.uptimeMillis()
        // Private tabs keep their picture in memory only, which is where the cache lives.
        capture?.invoke { bmp ->
            fullCapture = bmp
            captureDone = true
            BrowserLog.info("tabs", "Tab switch capture took ${SystemClock.uptimeMillis() - captureStartMs} ms")
        }
    }

    private fun neighbours(): Triple<BrowserTab?, BrowserTab, BrowserTab?>? {
        val t = tabs ?: return null
        val cur = t.selected ?: return null
        val list = tabsInSpace(t.tabs, { it.isPrivate }, cur.isPrivate)
        val i = list.indexOfFirst { it.id == cur.id }
        if (i < 0) return null
        return Triple(list.getOrNull(i - 1), cur, list.getOrNull(i + 1))
    }

    private fun page(tab: BrowserTab, bitmap: Bitmap? = null): Page =
        Page(tab.id, displayUrl(tab.url), (bitmap ?: ThumbnailCache.get(tab.id))?.asImageBitmap() ?: tab.thumbnail)

    /** A drag past the 12 dp threshold: lift the page and show the capsules. Returns false when there is nothing to switch to. */
    fun beginLift(): Boolean {
        if (!enabled) return false
        val (prev, cur, next) = neighbours() ?: return false
        if (prev == null && next == null) return false
        if (reducedMotion()) { engaged = true; return true }
        val list = listOfNotNull(prev, cur, next)
        centerIndex = if (prev != null) 1 else 0
        pages = list.map { if (it.id == cur.id) page(it, fullCapture) else page(it) }
        engaged = true
        visible = true
        scope.launch {
            drag.snapTo(0f); alpha.snapTo(1f)
            lift.animateTo(1f, OraMotion.spatial())
            // The lift is done: keep only the half-size copy of the current page and free the full-size one.
            fullCapture?.let { full ->
                val half = ThumbnailCache.putHalf(cur.id, full)
                pages = pages.map { if (it.tabId == cur.id) Page(it.tabId, it.label, half.asImageBitmap()) else it }
                fullCapture = null
                androidx.compose.runtime.withFrameNanos { }
                full.recycle()
            }
        }
        return true
    }

    fun dragBy(totalDx: Float) {
        if (!engaged || reducedMotion()) return
        val dirBlocked = (totalDx < 0 && centerIndex >= pages.size - 1) || (totalDx > 0 && centerIndex <= 0)
        // Rubber band at the end of the list.
        val shown = if (dirBlocked) totalDx * 0.25f else totalDx
        scope.launch { drag.snapTo(shown) }
    }

    /** Release: settle the nearest capsule, swap the session behind the overlay, then zoom in. */
    fun release(totalDx: Float, velocityX: Float) {
        if (!engaged) return
        val list = neighbours()
        val t = tabs
        if (list == null || t == null) { scope.launch { reset() }; return }
        val (prev, cur, next) = list
        val projected = totalDx + velocityX * 0.12f
        val move = when {
            abs(projected) < pageWidthPx * 0.25f -> 0
            projected < 0 -> if (next != null) 1 else 0
            else -> if (prev != null) -1 else 0
        }
        val target = when (move) { 1 -> next!!; -1 -> prev!!; else -> cur }
        if (reducedMotion()) { finish(target, cur, moveReduced = true); return }
        scope.launch { settleTo(target, cur, (move - 0) * 1) }
    }

    /** Jump from the group strip: lift, slide straight to the chosen capsule, then zoom in. */
    fun jump(tabId: Long): Boolean {
        val t = tabs ?: return false
        val cur = t.selected ?: return false
        if (visible || tabId == cur.id) return false
        val list = tabsInSpace(t.tabs, { it.isPrivate }, cur.isPrivate)
        val target = list.firstOrNull { it.id == tabId } ?: return false
        if (target.isPrivate != cur.isPrivate || pageWidthPx <= 0f) return false
        if (reducedMotion()) { engaged = true; finish(target, cur, moveReduced = true); return true }
        val forward = list.indexOf(target) > list.indexOf(cur)
        pages = if (forward) listOf(page(cur), page(target)) else listOf(page(target), page(cur))
        centerIndex = if (forward) 0 else 1
        engaged = true
        visible = true
        scope.launch {
            drag.snapTo(0f); alpha.snapTo(1f)
            capture?.invoke { bmp -> bmp?.let { b -> ThumbnailCache.putHalf(cur.id, b); b.recycle() } }
            lift.animateTo(1f, OraMotion.spatial())
            settleTo(target, cur, if (forward) 1 else -1)
        }
        return true
    }

    private suspend fun settleTo(target: BrowserTab, cur: BrowserTab, move: Int) {
        val t = tabs ?: return reset()
        drag.animateTo(-move * step, OraMotion.spatial())
        if (target.id != cur.id) {
            // Swap behind the overlay: the new session attaches while the capsule still covers it.
            val painted = CompletableDeferred<Unit>()
            pending = target.id to painted
            switchStartMs = SystemClock.uptimeMillis()
            t.select(target.id)
            val showTarget = pages.firstOrNull { it.tabId == target.id }
            pages = listOfNotNull(showTarget); centerIndex = 0
            drag.snapTo(0f)
            val hit = withTimeoutOrNull(CAP_MS) { painted.await() } != null
            BrowserLog.info("tabs", if (hit) "Tab switch first composite after ${SystemClock.uptimeMillis() - switchStartMs} ms" else "Tab switch hit the $CAP_MS ms cap, zooming in with the picture")
            lift.animateTo(0f, OraMotion.spatial())
            // The cap was hit: the picture stays until the live page draws, then crossfades away.
            if (!hit) withTimeoutOrNull(LATE_MS) { painted.await() }
            alpha.animateTo(0f, OraMotion.enter())
        } else {
            lift.animateTo(0f, OraMotion.spatial())
        }
        reset()
    }

    /** Reduced motion: a 120 ms crossfade, no travel and no zoom. */
    private fun finish(target: BrowserTab, cur: BrowserTab, moveReduced: Boolean) {
        scope.launch {
            val t = tabs ?: return@launch reset()
            if (target.id != cur.id) {
                pages = listOf(page(target)); centerIndex = 0
                lift.snapTo(0f); alpha.snapTo(0f); drag.snapTo(0f)
                visible = true
                alpha.animateTo(1f, OraMotion.reduced())
                val painted = CompletableDeferred<Unit>()
                pending = target.id to painted
                switchStartMs = SystemClock.uptimeMillis()
                t.select(target.id)
                withTimeoutOrNull(CAP_MS) { painted.await() }
                alpha.animateTo(0f, OraMotion.reduced())
            }
            reset()
        }
    }

    private suspend fun reset() {
        visible = false
        engaged = false
        pending = null
        pages = emptyList()
        drag.snapTo(0f); lift.snapTo(0f); alpha.snapTo(1f)
    }

    /** Called by a tab's content delegate when its session first draws. */
    fun painted(tabId: Long) {
        pending?.takeIf { it.first == tabId }?.second?.complete(Unit)
    }

    companion object {
        const val SCALE = 0.87f
        const val CAP_MS = 300L
        const val LATE_MS = 1500L
        const val LIFT_THRESHOLD_DP = 12f
    }
}

/** The one tab switch of the open browser screen; the address bar gesture and the group strip reach it here. */
object TabSwitchGesture {
    var state: TabSwitchState? = null
}

/** The capsules, drawn over the page. */
@Composable
internal fun TabSwitchOverlay(state: TabSwitchState, modifier: Modifier = Modifier) {
    if (!state.visible) return
    val colors = browserColors()
    val lift = state.lift.value
    val density = LocalDensity.current
    Box(modifier.fillMaxSize().graphicsLayer { alpha = state.alpha.value }) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = lift }.background(colors.GridBackground))
        val radius = BrowserTokens.TabSwitchCorner
        state.pages.forEachIndexed { i, page ->
            val offset = i - state.centerIndex
            val isCenter = offset == 0
            val shape = RoundedCornerShape(radius * lift)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = 1f - (1f - TabSwitchState.SCALE) * lift
                        scaleX = s; scaleY = s
                        translationX = state.drag.value + offset * (size.width * TabSwitchState.SCALE + state.gapPx)
                        this.shape = shape
                        clip = true
                        alpha = if (isCenter) 1f else lift
                    }
                    .background(colors.ToolbarBackground)
            ) {
                if (page.bitmap != null) Image(page.bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
                else Text(page.label.take(1).uppercase(), Modifier.align(Alignment.Center), fontSize = 48.sp, color = colors.SecondaryText)
            }
        }
        // The domain of the capsule nearest the middle.
        val nearest = (state.centerIndex - (state.drag.value / (state.pageWidthPx * TabSwitchState.SCALE + state.gapPx).coerceAtLeast(1f))).roundToInt()
        state.pages.getOrNull(nearest)?.let { page ->
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).graphicsLayer { alpha = lift },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { Text(page.label, fontSize = 14.sp, color = colors.AddressBarText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}
