package com.makemission.ora.browser

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.pressFeedback
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * Save image as: name, folder and format for one image. Opened from the "Save image as" row of the long-press
 * menu (the previous component), so it grows out of the press point and shrinks back into it.
 */
@Composable
fun SaveImageSheet(
    imageUrl: String,
    pageUrl: String,
    isPrivate: Boolean,
    settings: SettingsRepository,
    /** The press point in pixels, where the sheet grows from. */
    originX: Int,
    originY: Int,
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

    var fetched by remember { mutableStateOf<FetchedImage?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(imageUrl) {
        ImageSaver.fetch(context, imageUrl, pageUrl, isPrivate) { result ->
            if (result == null) failed = true else fetched = result
        }
    }
    val startParts = remember(imageUrl) { imageNameParts(imageUrl, null) }
    var name by remember { mutableStateOf(startParts.first) }
    var format by remember { mutableStateOf(ImageFormat.ORIGINAL) }
    val folder by settings.imageSaveFolder.collectAsState()
    val alwaysAsk by settings.imageAlwaysAsk.collectAsState()
    val ext = format.ext ?: imageNameParts(imageUrl, fetched?.mime).second

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            settings.setImageSaveFolder(uri.toString())
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().pressFeedback().background(colors.GridBackground.copy(alpha = 0.5f)).clickable(onClick = close)) {
        val density = LocalDensity.current
        val origin = TransformOrigin(
            (with(density) { originX.toDp() } / maxWidth).coerceIn(0f, 1f),
            (with(density) { originY.toDp() } / maxHeight).coerceIn(0f, 1f),
        )
        AnimatedVisibility(
            visibleState = visible,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(initialScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.overlayScaleIn(), transformOrigin = origin),
            exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(targetScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.overlayScaleOut(), transformOrigin = origin),
        ) {
            Column(
                modifier = Modifier
                    .padding(BrowserTokens.QuickMenuEdgeGap * 2)
                    .widthIn(max = 380.dp)
                    .heightIn(max = maxHeight - BrowserTokens.QuickMenuEdgeGap * 4)
                    .pressFeedback().clip(RoundedCornerShape(BrowserTokens.CardRadius))
                    .background(colors.ToolbarBackground)
                    .clickable(onClick = {})
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Save image as", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.AddressBarText)
                // File name, with the extension as a chip.
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(BrowserTokens.RadiusSmall)).background(colors.AddressBarBackground).padding(start = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = name, onValueChange = { name = it.take(80) }, singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = colors.AddressBarText),
                        cursorBrush = SolidColor(colors.AddressBarText),
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.clip(RoundedCornerShape(BrowserTokens.RadiusSmall)).background(colors.ToolbarBackground).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(".$ext", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
                    }
                }
                // Where it goes. Change opens the system folder picker and the choice is remembered.
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Save to", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
                        Text(if (folder == null) "Pictures › Ora" else "The folder you chose", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                    }
                    if (folder != null) Text("Reset", fontSize = 14.sp, color = colors.SecondaryText, modifier = Modifier.pressFeedback().clickable { settings.setImageSaveFolder(null) }.padding(12.dp))
                    Text("Change", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText, modifier = Modifier.clickable { picker.launch(null) }.padding(12.dp))
                }
                // Format. Converting re-encodes the image and drops its metadata.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Format", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
                    Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ImageFormat.values().forEach { f ->
                            Box(Modifier.weight(1f)) { SegmentLabel(f.label, format == f, Modifier, pill = true) { format = f } }
                        }
                    }
                    Text(
                        if (format == ImageFormat.ORIGINAL) "Keeps the file exactly as the site sent it."
                        else "Re-encodes the image and drops its metadata.",
                        fontSize = 12.sp, color = colors.SecondaryText,
                    )
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).pressFeedback().clickable { settings.setImageAlwaysAsk(!alwaysAsk) }, verticalAlignment = Alignment.CenterVertically) {
                    Text("Always ask where to save", fontSize = 15.sp, color = colors.AddressBarText, modifier = Modifier.weight(1f))
                    MineralSwitch(checked = alwaysAsk, label = "Switch", onChange = { settings.setImageAlwaysAsk(it) })
                }
                if (failed) Text("Could not load this image.", fontSize = 13.sp, color = com.makemission.ora.ui.theme.Mineral.Ember)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                    PillButton("Cancel", onClick = close)
                    PillButton(if (fetched == null && !failed) "Loading" else "Save", primary = true, onClick = {
                        val image = fetched
                        if (image == null) return@PillButton
                        val bytes = ImageSaver.convert(image.bytes, format)
                        if (bytes == null) { InAppNotices.show("Could not convert this image"); return@PillButton }
                        val mime = format.mime ?: image.mime?.substringBefore(';')?.trim()?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
                        val file = name.trim().ifBlank { startParts.first } + "." + ext
                        val where = ImageSaver.save(context, bytes, file, mime, folder)
                        if (!isPrivate) BrowserLog.info("page", "Save image as: ${if (where != null) "saved" else "failed"} (${LogPrivacy.domainOf(imageUrl)}, ${format.label})")
                        InAppNotices.show(if (where != null) "Saved to $where" else "Could not save the image")
                        if (where != null) close()
                    })
                }
            }
        }
    }
}
