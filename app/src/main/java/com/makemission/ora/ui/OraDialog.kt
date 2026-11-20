package com.makemission.ora.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/** Where the finger last went down, in window pixels. Set once at the root; dialogs grow out of this point. */
object LastPress {
    @Volatile
    var position: Offset = Offset.Unspecified
}

/**
 * Confirmation dialog with the same slots as the stock `AlertDialog`, drawn in Ora's colours.
 * The card grows out of the control that was pressed (scale from [OraMotion.MORPH_SCALE], fade in) and shrinks back into
 * it when dismissed by Back or a tap outside. Buttons that close it by clearing the caller's state remove it at once.
 */
@Composable
fun OraDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    val colors = browserColors()
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    var cardTopLeft by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val origin = remember { LastPress.position }
    androidx.compose.runtime.LaunchedEffect(visible.isIdle, visible.currentState) {
        if (visible.isIdle && !visible.currentState) onDismissRequest()
    }
    val close = { visible.targetState = false }
    val pivot = if (cardSize.width > 0 && origin.isSpecified) {
        TransformOrigin(
            ((origin.x - cardTopLeft.x) / cardSize.width).coerceIn(0f, 1f),
            ((origin.y - cardTopLeft.y) / cardSize.height).coerceIn(0f, 1f),
        )
    } else TransformOrigin.Center

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        // The card centres in the space above the keyboard, so a field inside a dialog is never covered.
        Box(Modifier.fillMaxSize().imePadding()) {
            AnimatedVisibility(visibleState = visible, modifier = Modifier.matchParentSize(), enter = fadeIn(OraMotion.overlayFadeIn()), exit = fadeOut(OraMotion.overlayFadeOut())) {
                Box(Modifier.fillMaxSize().background(colors.Scrim.copy(alpha = 0.4f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close))
            }
            AnimatedVisibility(
                visibleState = visible,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE, transformOrigin = pivot),
                exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE, transformOrigin = pivot),
            ) {
                Column(
                    Modifier
                        .padding(24.dp)
                        .widthIn(max = 360.dp)
                        .onGloballyPositioned { cardTopLeft = it.positionInWindow(); cardSize = it.size }
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.Surface)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (title != null) ProvideTextStyle(TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)) { title() }
                    if (text != null) Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        ProvideTextStyle(TextStyle(fontSize = 14.sp, color = colors.SecondaryText)) { text() }
                    }
                    Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        dismissButton?.invoke()
                        confirmButton()
                    }
                }
            }
        }
    }
}
