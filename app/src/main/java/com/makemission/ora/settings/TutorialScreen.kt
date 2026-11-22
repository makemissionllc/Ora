package com.makemission.ora.settings

import com.makemission.ora.ui.pressFeedback
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.browser.KeyShortcuts
import com.makemission.ora.browser.PillButton
import com.makemission.ora.browser.QuickItem
import com.makemission.ora.browser.QuickKind
import com.makemission.ora.browser.Shortcut
import com.makemission.ora.browser.quickMatches
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.onboarding.TextAction
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/** One lesson of the interactive tutorial. [id] is what is saved when it is finished. */
data class Lesson(val id: String, val title: String, val intro: String, val tabletOnly: Boolean = false, val phoneOnly: Boolean = false)

/** The lessons that apply to this device, in order. Keyboard lessons are for tablets; the bottom bar is a phone thing. Pure, so it can be tested. */
fun tutorialLessons(tablet: Boolean): List<Lesson> = listOf(
    Lesson("search", "Search from the address bar", "Type a short keyword, a space, then what to look for. Ora opens that site's own search."),
    Lesson("switcher", "Find anything with the Quick switcher", "One box finds an open tab, a bookmark, a history page or an action. Open it from the ⋯ menu${if (tablet) ", or press Ctrl+K" else ""}."),
    Lesson("swipe", "Swipe to change tabs", "Drag the address bar sideways to move to the next or previous tab."),
    Lesson("hold", "Press and hold for more", "Press and hold a link or image to open it elsewhere, copy it or share it."),
    Lesson("keys", "Work from the keyboard", "A keyboard makes Ora quicker. Try three shortcuts.", tabletOnly = true),
    Lesson("yours", "Make it yours", "Where to change how Ora looks, what its bottom bar does, and what it blocks.", phoneOnly = false),
).filter { (!it.tabletOnly || tablet) && (!it.phoneOnly || !tablet) }

/** The first lesson not finished, or null when all are. Pure. */
fun nextLesson(lessons: List<Lesson>, done: Set<String>): Lesson? = lessons.firstOrNull { it.id !in done }

private val SampleItems = listOf(
    QuickItem(QuickKind.TAB, "t1", "Weather today", "weather.example"),
    QuickItem(QuickKind.TAB, "t2", "Pasta recipes", "recipes.example"),
    QuickItem(QuickKind.BOOKMARK, "b1", "Trail map", "maps.example/trail"),
    QuickItem(QuickKind.HISTORY, "h1", "Train times", "trains.example"),
    QuickItem(QuickKind.ACTION, "a1", "New private tab"),
    QuickItem(QuickKind.ACTION, "a2", "Settings"),
)

/**
 * The interactive tutorial (Settings > About). Each lesson is a card that opens in place and has a small practice
 * area you use for real; finishing the practice ticks the lesson and opens the next. Progress is saved, so you can
 * leave and come back. Nothing here touches your tabs, history or settings.
 * Previous component: the Interactive tutorial row in Settings > About (the screen replaces the settings page, as Diagnostics does).
 */
@Composable
fun TutorialScreen(settings: SettingsRepository, tablet: Boolean, onDone: () -> Unit) {
    val colors = browserColors()
    val done by settings.tutorialDone.collectAsState()
    val lessons = remember(tablet) { tutorialLessons(tablet) }
    var openId by remember { mutableStateOf(nextLesson(lessons, done)?.id) }
    var confirmReset by remember { mutableStateOf(false) }
    val finished = lessons.count { it.id in done }
    BackHandler { onDone() }

    fun complete(id: String) {
        val wasAll = lessons.all { it.id in settings.tutorialDone.value }
        settings.markTutorialLesson(id)
        val next = nextLesson(lessons, settings.tutorialDone.value)
        if (next == null && !wasAll) InAppNotices.show("Tutorial finished. Nicely done.")
        openId = next?.id
    }

    Box(Modifier.fillMaxSize().background(colors.GridBackground), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 680.dp).fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Interactive tutorial", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.AddressBarText)
                    Text(
                        if (finished == lessons.size) "All ${lessons.size} lessons done" else "$finished of ${lessons.size} lessons done",
                        fontSize = 13.sp, color = colors.SecondaryText,
                    )
                }
                PillButton("Done", onClick = onDone, primary = true)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                lessons.forEachIndexed { index, lesson ->
                    val isDone = lesson.id in done
                    LessonCard(
                        number = index + 1, lesson = lesson, done = isDone, open = openId == lesson.id,
                        onToggle = { openId = if (openId == lesson.id) null else lesson.id },
                    ) {
                        when (lesson.id) {
                            "search" -> SearchPractice(isDone) { complete(lesson.id) }
                            "switcher" -> SwitcherPractice(isDone) { complete(lesson.id) }
                            "swipe" -> SwipePractice(isDone) { complete(lesson.id) }
                            "hold" -> HoldPractice(isDone) { complete(lesson.id) }
                            "keys" -> KeysPractice(isDone) { complete(lesson.id) }
                            else -> YoursPractice(isDone) { complete(lesson.id) }
                        }
                    }
                }
                if (finished > 0) {
                    TextButton(onClick = { confirmReset = true }) { Text("Start over", color = Mineral.Ember, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
    if (confirmReset) {
        OraDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Start the tutorial over?") },
            text = { Text("$finished ${if (finished == 1) "lesson goes" else "lessons go"} back to not done. Nothing else changes.") },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = colors.SecondaryText) } },
            confirmButton = {
                TextButton(onClick = { settings.resetTutorial(); confirmReset = false; openId = lessons.first().id }) {
                    Text("Start over", color = Mineral.Ember, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

@Composable
private fun LessonCard(number: Int, lesson: Lesson, done: Boolean, open: Boolean, onToggle: () -> Unit, practice: @Composable () -> Unit) {
    val colors = browserColors()
    val turn by animateFloatAsState(if (open) 90f else 0f, OraMotion.enter(), label = "lesson chevron")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(colors.Surface)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).pressFeedback().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(if (done) colors.Accent else colors.AddressBarBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (done) "✓" else "$number", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (done) colors.GridBackground else colors.SecondaryText)
            }
            Text(lesson.title, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
            Text("›", Modifier.rotate(turn), fontSize = 18.sp, color = colors.SecondaryText)
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(OraMotion.enter()) + expandVertically(OraMotion.enter()),
            exit = fadeOut(OraMotion.exit()) + shrinkVertically(OraMotion.exit()),
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 58.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(lesson.intro, fontSize = 13.sp, lineHeight = 18.sp, color = colors.SecondaryText)
                practice()
            }
        }
    }
}

@Composable
private fun DoneNote(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = browserColors().Accent)
}

@Composable
private fun SearchPractice(done: Boolean, onWin: () -> Unit) {
    val colors = browserColors()
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    fun go() {
        val m = SearchShortcuts.resolve(text, SearchShortcuts.custom, true)
        if (m == null) {
            result = "No shortcut there. Try yt cats, gh compose or w kenya."
        } else {
            result = "That opens a ${m.shortcut.label} search for \"${m.query}\"."
            onWin()
        }
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).background(colors.AddressBarBackground, CircleShape).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text, onValueChange = { text = it; result = null }, singleLine = true,
            textStyle = TextStyle(color = colors.AddressBarText, fontSize = 15.sp), cursorBrush = SolidColor(colors.Accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go), keyboardActions = KeyboardActions(onGo = { go() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner -> if (text.isEmpty()) Text("Try: yt funny cats", color = colors.SecondaryText, fontSize = 15.sp); inner() },
        )
        TextAction("Go") { go() }
    }
    result?.let { Text(it, fontSize = 13.sp, color = colors.AddressBarText) }
    if (done) DoneNote("Done. Add your own keywords in Settings > Search.")
}

@Composable
private fun SwitcherPractice(done: Boolean, onWin: () -> Unit) {
    val colors = browserColors()
    var query by remember { mutableStateOf("") }
    val matches = remember(query) { quickMatches(query, SampleItems, limit = 4) }
    var picked by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).background(colors.AddressBarBackground, RoundedCornerShape(BrowserTokens.RadiusSmall)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            textStyle = TextStyle(color = colors.AddressBarText, fontSize = 15.sp), cursorBrush = SolidColor(colors.Accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner -> if (query.isEmpty()) Text("Type tr, then tap a result", color = colors.SecondaryText, fontSize = 15.sp); inner() },
        )
    }
    if (matches.isEmpty()) Text("Nothing matches", fontSize = 13.sp, color = colors.SecondaryText)
    matches.forEach { item ->
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).pressFeedback().clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
                .clickable { picked = item.title; onWin() }.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(item.kind.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp, color = colors.SecondaryText, modifier = Modifier.widthIn(min = 56.dp))
            Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    picked?.let { Text("You chose \"$it\". In Ora this switches to it.", fontSize = 13.sp, color = colors.AddressBarText) }
    if (done) DoneNote("Done. Up, Down and Enter work here too.")
}

@Composable
private fun SwipePractice(done: Boolean, onWin: () -> Unit) {
    val colors = browserColors()
    val threshold = with(LocalDensity.current) { 80.dp.toPx() }
    var travelled by remember { mutableStateOf(0f) }
    var tab by remember { mutableIntStateOf(1) }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(CircleShape).background(colors.AddressBarBackground)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { travelled = 0f },
                    onDragEnd = {
                        if (kotlin.math.abs(travelled) >= threshold) {
                            tab = if (travelled < 0) minOf(3, tab + 1) else maxOf(1, tab - 1)
                            onWin()
                        }
                    },
                ) { _, delta -> travelled += delta }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("Tab $tab of 3  ·  drag me sideways", fontSize = 15.sp, color = colors.AddressBarText)
    }
    if (done) DoneNote("Done. Tapping a tab in a group strip slides to it as well.")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HoldPractice(done: Boolean, onWin: () -> Unit) {
    val colors = browserColors()
    var held by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).pressFeedback().clip(RoundedCornerShape(BrowserTokens.RadiusSmall)).background(colors.AddressBarBackground)
            .combinedClickable(onClick = {}, onLongClick = { held = true; onWin() }).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text("example.org/article  ·  press and hold", fontSize = 15.sp, color = colors.AddressBarText)
    }
    if (held) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.RadiusSmall)).background(colors.SidebarSelected).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Open in new tab", fontSize = 14.sp, color = colors.AddressBarText)
            Text("Open in private tab", fontSize = 14.sp, color = colors.AddressBarText)
            Text("Copy link  ·  Share  ·  Add to reading list", fontSize = 14.sp, color = colors.SecondaryText)
        }
    }
    if (done) DoneNote("Done. This is the menu you get on a real link.")
}

@Composable
private fun KeysPractice(done: Boolean, onWin: () -> Unit) {
    val colors = browserColors()
    val seen = remember { mutableStateOf(listOf<Shortcut>()) }
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    DisposableEffect(Unit) {
        KeyShortcuts.practice = { s ->
            if (s !in seen.value) {
                seen.value = seen.value + s
                if (seen.value.size >= 3) onWin()
            }
        }
        onDispose { KeyShortcuts.practice = null }
    }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).pressFeedback().clip(RoundedCornerShape(BrowserTokens.RadiusSmall)).background(colors.AddressBarBackground)
            .focusRequester(focus).onFocusChanged { focused = it.isFocused }.clickable { runCatching { focus.requestFocus() } }.padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            if (seen.value.isEmpty()) "Press Ctrl+K, Ctrl+T or Ctrl+L on your keyboard" else seen.value.joinToString("\n") { "✓ ${it.label}  (${it.keys})" },
            fontSize = 14.sp, lineHeight = 20.sp, color = colors.AddressBarText,
        )
    }
    Text("While this lesson is open the keys only show here and do not act on the browser.", fontSize = 12.sp, color = colors.SecondaryText)
    if (done) DoneNote("Done. F1 anywhere lists every shortcut.")
}

@Composable
private fun YoursPractice(done: Boolean, onWin: () -> Unit) {
    val colors = browserColors()
    Text("Appearance: theme, bar position, bottom bar buttons (press and hold the bar).", fontSize = 13.sp, color = colors.AddressBarText)
    Text("Privacy: strict blocking, HTTPS-only and app lock.", fontSize = 13.sp, color = colors.AddressBarText)
    Text("Labs: extras such as the site card behind the padlock.", fontSize = 13.sp, color = colors.AddressBarText)
    Text("About: Run setup again, What's new and Keyboard shortcuts.", fontSize = 13.sp, color = colors.AddressBarText)
    if (done) DoneNote("Done. You have finished the tutorial.") else Box(Modifier.padding(top = 4.dp)) { PillButton("Got it", onClick = onWin, primary = true) }
}
