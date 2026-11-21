package com.makemission.ora.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * Ora's radio button. The ring changes colour with the Quick token and the dot pops in with [OraMotion.pop], so a
 * choice lands with a small bounce instead of switching on. With a null [onClick] it only shows state, for rows
 * that are clickable as a whole. [enabled] false dims it.
 */
@Composable
fun OraRadio(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = browserColors()
    val ring by animateColorAsState(if (selected) colors.Accent else colors.SecondaryText, OraMotion.quick(), label = "radio ring")
    val dot by animateFloatAsState(if (selected) 1f else 0f, OraMotion.pop(), label = "radio dot")
    val tap = if (onClick != null && enabled) Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick) else Modifier
    Box(
        modifier.then(tap).defaultMinSize(minWidth = 40.dp, minHeight = 40.dp).alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(20.dp).border(2.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(10.dp).scale(dot.coerceAtLeast(0f)).background(ring, CircleShape))
        }
    }
}
