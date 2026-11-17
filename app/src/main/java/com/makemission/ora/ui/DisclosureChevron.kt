package com.makemission.ora.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.browserColors

/** The "›" that marks a row as opening another screen. Shared by the menus and Settings. */
@Composable
fun DisclosureChevron() {
    Text("›", fontSize = 18.sp, color = browserColors().SecondaryText)
}
