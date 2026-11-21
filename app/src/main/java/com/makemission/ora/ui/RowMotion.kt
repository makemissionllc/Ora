package com.makemission.ora.ui

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import com.makemission.ora.ui.theme.OraMotion

/**
 * The motion every list row shares: a new row fades in, a removed row fades out, and the rows below it slide
 * up to close the gap (or down to make room) instead of jumping. Rows need stable keys for this to work.
 */
fun LazyItemScope.oraRowMotion(): Modifier =
    Modifier.animateItem(fadeInSpec = OraMotion.standard(), placementSpec = OraMotion.standard(), fadeOutSpec = OraMotion.leave())
