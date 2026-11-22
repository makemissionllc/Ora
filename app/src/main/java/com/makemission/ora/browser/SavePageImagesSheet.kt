package com.makemission.ora.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.downloads.DownloadsRepository
import com.makemission.ora.extensions.PageImage
import com.makemission.ora.extensions.PageImagesBridge
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.ui.CheckMark
import com.makemission.ora.ui.LastPress
import com.makemission.ora.ui.pressFeedback
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import org.mozilla.geckoview.GeckoWebExecutor
import org.mozilla.geckoview.WebRequest
import java.util.concurrent.Executors

/** More images than this ask once more before saving, saying how many. */
private const val CONFIRM_ABOVE = 20

/** Small thumbnails for the sheet: fetched through Gecko (so cookies apply), decoded down to a small size and kept in memory only. */
private object ThumbLoader {
    private val cache = object : LruCache<String, Bitmap>(48) {}
    private val pool = Executors.newFixedThreadPool(3) { r -> Thread(r, "ora-thumb").apply { isDaemon = true } }
    private const val MAX_BYTES = 6 * 1024 * 1024

    fun cached(url: String): Bitmap? = cache.get(url)

    fun load(context: Context, url: String, pageUrl: String, isPrivate: Boolean): Bitmap? {
        cache.get(url)?.let { return it }
        val app = context.applicationContext
        val future = pool.submit<Bitmap?> {
            runCatching {
                val executor = GeckoWebExecutor(GeckoEngine.runtime(app))
                val request = WebRequest.Builder(url).method("GET").referrer(pageUrl).build()
                val flags = if (isPrivate) GeckoWebExecutor.FETCH_FLAGS_PRIVATE else GeckoWebExecutor.FETCH_FLAGS_NONE
                val response = executor.fetch(request, flags).poll(15_000) ?: return@runCatching null
                if (response.statusCode !in 200..299) { response.body?.close(); return@runCatching null }
                val bytes = response.body!!.use { stream ->
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val n = stream.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        if (out.size() > MAX_BYTES) return@runCatching null
                    }
                    out.toByteArray()
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= 240 && bounds.outHeight / (sample * 2) >= 240) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull()
        }
        val bitmap = runCatching { future.get() }.getOrNull()
        if (bitmap != null) cache.put(url, bitmap)
        return bitmap
    }
}

/** The image addresses in [found], without repeats (the same file with and without a #fragment counts once), in page order. */
internal fun distinctImages(found: List<PageImage>): List<PageImage> =
    found.filter { it.url.isNotBlank() }.distinctBy { it.url.substringBefore('#') }

/** File names for [urls] that do not repeat inside one batch: a second "photo.jpg" becomes "photo-2.jpg". */
internal fun uniqueNames(urls: List<String>): List<String> {
    val seen = HashMap<String, Int>()
    return urls.map { url ->
        val (base, ext) = imageNameParts(url, null)
        val n = (seen[base.lowercase() + "." + ext] ?: 0) + 1
        seen[base.lowercase() + "." + ext] = n
        if (n == 1) "$base.$ext" else "$base-$n.$ext"
    }
}

/**
 * Save all images: every image on the open page as a grid of thumbnails to pick from, then saved through Downloads.
 * Previous component: the "Save all images" row of the page menu. The card grows out of that row's press point and
 * shrinks back into it, and the footer swaps in place for the large-batch confirmation. In a private tab the
 * downloads are private: they run but are never written to the Downloads list on disk or to history.
 */
@Composable
fun SavePageImagesSheet(
    pageUrl: String,
    isPrivate: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = browserColors()
    val visible = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visible.targetState = true }
    LaunchedEffect(visible.currentState, visible.targetState) {
        if (!visible.currentState && !visible.targetState) onDismiss()
    }
    val close = { visible.targetState = false }
    val press = remember { LastPress.position }

    var images by remember { mutableStateOf<List<PageImage>?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(pageUrl) {
        PageImagesBridge.collect(pageUrl) { found ->
            if (found == null) failed = true else images = distinctImages(found)
            BrowserLog.info("page", "Save all images: ${if (found == null) "page could not be read" else "${distinctImages(found).size} images found"}")
        }
    }
    val selected = remember { mutableStateListOf<String>() }
    var confirming by remember { mutableStateOf(false) }
    var selectedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(images) { if (!selectedOnce && images != null) { selectedOnce = true; selected.addAll(images!!.map { it.url }) } }

    val save = {
        val urls = selected.toList()
        val names = uniqueNames(urls)
        val repo = DownloadsRepository.ensure(context)
        urls.forEachIndexed { i, url -> repo.begin(url, names[i], null, -1L, pageUrl, isPrivate) }
        if (!isPrivate) BrowserLog.info("page", "Save all images: ${urls.size} queued from ${LogPrivacy.domainOf(pageUrl)}")
        InAppNotices.show(if (urls.size == 1) "Saving 1 image to Downloads" else "Saving ${urls.size} images to Downloads")
        close()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.GridBackground.copy(alpha = 0.5f)).clickable(onClick = close)) {
        val density = LocalDensity.current
        val origin = if (press.isSpecified) TransformOrigin(
            (press.x / with(density) { maxWidth.toPx() }).coerceIn(0f, 1f),
            (press.y / with(density) { maxHeight.toPx() }).coerceIn(0f, 1f),
        ) else TransformOrigin.Center
        AnimatedVisibility(
            visibleState = visible,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(initialScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.overlayScaleIn(), transformOrigin = origin),
            exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(targetScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.overlayScaleOut(), transformOrigin = origin),
        ) {
            Column(
                modifier = Modifier
                    .padding(BrowserTokens.QuickMenuEdgeGap * 2)
                    .widthIn(max = 460.dp)
                    .heightIn(max = maxHeight - BrowserTokens.QuickMenuEdgeGap * 4)
                    .pressFeedback().clip(RoundedCornerShape(BrowserTokens.CardRadius))
                    .background(colors.ToolbarBackground)
                    .clickable(onClick = {})
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Save all images", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.AddressBarText)
                val list = images
                when {
                    failed -> Text(
                        if (isPrivate) "Ora can't read the images of this private tab yet. Open the page in a normal tab to save its images."
                        else "Could not read the images on this page. Wait for it to finish loading, then try again.",
                        fontSize = 14.sp, color = colors.SecondaryText,
                    )
                    list == null -> Text("Looking for images…", fontSize = 14.sp, color = colors.SecondaryText)
                    list.isEmpty() -> Text("No images on this page. Scroll down so lazy images load, then try again.", fontSize = 14.sp, color = colors.SecondaryText)
                    else -> {
                        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${selected.size} of ${list.size} selected", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText, modifier = Modifier.weight(1f))
                            val all = selected.size == list.size
                            Text("All", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (all) colors.SecondaryText else colors.AddressBarText,
                                modifier = Modifier.pressFeedback().clickable { selected.clear(); selected.addAll(list.map { it.url }) }.padding(12.dp))
                            Text("None", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (selected.isEmpty()) colors.SecondaryText else colors.AddressBarText,
                                modifier = Modifier.pressFeedback().clickable { selected.clear() }.padding(12.dp))
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(88.dp),
                            modifier = Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 420.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(list, key = { it.url }) { image ->
                                val on = image.url in selected
                                val thumb by produceState(ThumbLoader.cached(image.url), image.url) {
                                    if (value == null) value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ThumbLoader.load(context, image.url, pageUrl, isPrivate) }
                                }
                                Box(
                                    Modifier.aspectRatio(1f).pressFeedback().clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
                                        .background(colors.AddressBarBackground)
                                        .clickable { if (on) selected.remove(image.url) else selected.add(image.url) }
                                        .then(if (on) Modifier.border(2.dp, colors.Accent, RoundedCornerShape(BrowserTokens.RadiusSmall)) else Modifier),
                                ) {
                                    thumb?.let { Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                    if (image.width > 0) Text("${image.width}×${image.height}", fontSize = 10.sp, color = colors.AddressBarText,
                                        modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).clip(RoundedCornerShape(6.dp)).background(colors.ToolbarBackground.copy(alpha = 0.8f)).padding(horizontal = 4.dp, vertical = 1.dp))
                                    Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(RoundedCornerShape(11.dp)).background(if (on) colors.Accent.copy(alpha = 0.9f) else colors.ToolbarBackground.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                                        CheckMark(on, "Selected", size = 16.dp)
                                    }
                                }
                            }
                        }
                    }
                }
                // The footer changes in place: Cancel and Save, or the large-batch question.
                AnimatedContent(
                    targetState = confirming,
                    transitionSpec = { (fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE)) togetherWith (fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE)) },
                    label = "save-images-footer",
                ) { ask ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (ask) Text("Save ${selected.size} images? That can use a lot of data and storage.", fontSize = 14.sp, color = colors.SecondaryText)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                            PillButton(if (ask) "Back" else "Cancel", onClick = { if (ask) confirming = false else close() })
                            val ready = images?.isNotEmpty() == true && selected.isNotEmpty()
                            if (ready) PillButton(
                                if (ask) "Save ${selected.size}" else if (selected.size == 1) "Save 1 image" else "Save ${selected.size} images",
                                primary = true,
                                onClick = { if (!ask && selected.size > CONFIRM_ABOVE) confirming = true else save() },
                            )
                        }
                    }
                }
            }
        }
    }
}
