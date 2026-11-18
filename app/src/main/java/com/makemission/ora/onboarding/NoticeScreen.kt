package com.makemission.ora.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors

/**
 * The full notice. Opened from the "Before you start" screen and from Settings > About > Notice and privacy.
 * Back closes this page only. Copy notice puts the text on the clipboard so someone can check the wording.
 */
@Composable
fun NoticeScreen(tablet: Boolean, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    val context = LocalContext.current
    Box(
        Modifier.fillMaxSize().background(browserColors().GridBackground).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = if (tablet) 600.dp else 900.dp).fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Notice and privacy", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
                Text(
                    "Done",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = browserColors().Accent,
                    modifier = Modifier.heightIn(min = 44.dp).clickable(onClick = onDone).padding(horizontal = 8.dp),
                )
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NoticeFull.forEach { (head, body) ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(browserColors().Surface).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(head, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
                        Text(body, fontSize = 14.sp, lineHeight = 21.sp, color = browserColors().SecondaryText)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Copy notice",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = browserColors().Accent,
                    modifier = Modifier.heightIn(min = 44.dp).clickable { copyNotice(context) }.padding(vertical = 12.dp),
                )
            }
        }
    }
}

private fun copyNotice(context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Ora notice", noticeAsText()))
    BrowserLog.info("notice", "Notice copied")
    InAppNotices.show("Notice copied")
}
