package com.makemission.ora.passwords

import androidx.compose.foundation.layout.Box
import com.makemission.ora.ui.oraRowMotion
import com.makemission.ora.ui.theme.BrowserTokens
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.makemission.ora.notice.InAppNotices
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.makemission.ora.ui.OraDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.browserColors

/**
 * Passwords page: saved logins grouped as a list. Passwords stay hidden until shown,
 * Copy puts one on the clipboard, and Delete asks first because it cannot be undone.
 */
@Composable
fun PasswordsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val logins by PasswordStore.logins.collectAsState()
    var query by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(setOf<String>()) }
    var pendingDelete by remember { mutableStateOf<PasswordStore.Login?>(null) }

    // Back closes this page, not the whole Settings overlay.
    BackHandler(onBack = onDone)

    val shown = remember(logins, query) {
        val q = query.trim()
        if (q.isEmpty()) logins
        else logins.filter { it.host.contains(q, ignoreCase = true) || it.username.contains(q, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(browserColors().GridBackground)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Passwords", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
            TextButton(onClick = onDone) {
                Text("Done", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        }

        if (logins.isEmpty()) {
            EmptyPasswords()
            return@Column
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search by site or username", color = browserColors().SecondaryText) },
            textStyle = TextStyle(fontSize = 15.sp, color = browserColors().AddressBarText),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        )

        if (shown.isEmpty()) {
            Text(
                "No saved passwords match \"${query.trim()}\"",
                fontSize = 13.sp,
                color = browserColors().SecondaryText,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.guid }) { login ->
                Box(oraRowMotion()) {
                PasswordRow(
                    login = login,
                    isRevealed = login.guid in revealed,
                    onToggleReveal = {
                        revealed = if (login.guid in revealed) revealed - login.guid else revealed + login.guid
                    },
                    onCopy = {
                        copyPassword(context, login)
                    },
                    onDelete = { pendingDelete = login },
                )
                }
            }
        }
    }

    pendingDelete?.let { login ->
        OraDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this password?") },
            text = { Text("The saved login for ${login.host} (${login.username.ifEmpty { "no username" }}) will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val deleted = PasswordStore.delete(login.guid)
                    if (deleted) {
                        InAppNotices.show("Password deleted")
                    } else {
                        BrowserLog.error("passwords", "Deleting a saved login failed")
                        InAppNotices.show("Could not delete the password")
                    }
                    pendingDelete = null
                }) {
                    Text("Delete", color = browserColors().CloseBadge, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = browserColors().Accent)
                }
            },
        )
    }
}

@Composable
private fun EmptyPasswords() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No saved passwords yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
        Spacer(Modifier.height(6.dp))
        Text(
            "When you sign in to a site and choose to save, the login appears here. Password autofill fills it in next time.",
            fontSize = 13.sp,
            color = browserColors().SecondaryText,
        )
    }
}

@Composable
private fun PasswordRow(
    login: PasswordStore.Login,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
            .background(browserColors().AddressBarBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(login.host, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
        Text(
            login.username.ifEmpty { "No username" },
            fontSize = 13.sp,
            color = browserColors().SecondaryText,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (isRevealed) login.password else "••••••••",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = browserColors().AddressBarText,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleReveal) {
                Text(if (isRevealed) "Hide" else "Show", color = browserColors().Accent)
            }
            TextButton(onClick = onCopy) {
                Text("Copy", color = browserColors().Accent)
            }
            TextButton(onClick = onDelete) {
                Text("Delete", color = browserColors().CloseBadge)
            }
        }
    }
}

private fun copyPassword(context: Context, login: PasswordStore.Login) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Password", login.password))
    InAppNotices.show("Password copied")
}
