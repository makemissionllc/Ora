package com.makemission.ora.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import kotlin.math.PI
import kotlin.math.sin

/** The scene each Settings page shows, and the plain sentence under it. */
enum class SettingsScene(val caption: String) {
    APPEARANCE("Pick light, dark or pure dark, and the whole browser follows."),
    TABS("Tabs sit in a bar or a sidebar. Tap one to bring it to the front."),
    PRIVACY("Ora blocks trackers and can lock itself, so what you do stays yours."),
    SEARCH("Type in the address bar and Ora suggests, using the engine you chose."),
    HOME("Choose what a new tab shows: your tiles, or any page you like."),
    DEVICES("Send a page from your phone and it opens on your tablet, over your own Wi-Fi."),
    LABS("Try early ideas. Each one is off until you turn it on."),
    DIAGNOSTICS("Ora keeps a readable log, so when something breaks you can see why."),
    ABOUT("What is new, which version this is, and how to set Ora up again."),
}

/**
 * The drawn, looping scene at the top of a Settings page: a small hand taps something, and the device (or two
 * devices) shows what that setting does, the way Google's settings pages explain themselves. Flat shapes in the
 * palette only (no gradients, no shadows). With system animations off it shows one still frame. The caption under
 * it says the same in words, and is what a screen reader reads.
 * Previous component: the Settings page it sits in, which itself grows out of its category row; the scene is part
 * of that page, and swaps with a fade when the tablet rail changes category.
 */
@Composable
fun SettingsIllustration(scene: SettingsScene, modifier: Modifier = Modifier) {
    val colors = browserColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius))
            .background(colors.Surface)
            .semantics { contentDescription = scene.caption },
    ) {
        AnimatedContent(
            targetState = scene,
            transitionSpec = { fadeIn(OraMotion.standard()) togetherWith fadeOut(OraMotion.leave()) },
            label = "settings illustration",
        ) { shown -> SceneCanvas(shown) }
        Text(
            scene.caption,
            fontSize = 14.sp, lineHeight = 19.sp, color = colors.SecondaryText,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        )
    }
}

@Composable
private fun SceneCanvas(scene: SettingsScene) {
    val colors = browserColors()
    val loop = rememberInfiniteTransition(label = "illustration")
    val running by loop.animateFloat(0f, 1f, OraMotion.illustrationLoop(), label = "t")
    // Still frame when animations are off: the moment the result is fully visible.
    val t = if (OraMotion.reduced) 0.62f else running
    Canvas(Modifier.fillMaxWidth().height(BrowserTokens.SettingsIllustrationHeight)) {
        val u = size.height / 150f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val ink = colors.SecondaryText
        val soft = colors.AddressBarBackground
        val accent = colors.Accent
        val strong = colors.AddressBarText
        val p = Pal(ink, soft, accent, strong, colors.Surface)
        when (scene) {
            SettingsScene.APPEARANCE -> appearance(t, u, cx, cy, p)
            SettingsScene.TABS -> tabs(t, u, cx, cy, p)
            SettingsScene.PRIVACY -> privacy(t, u, cx, cy, p)
            SettingsScene.SEARCH -> search(t, u, cx, cy, p)
            SettingsScene.HOME -> home(t, u, cx, cy, p)
            SettingsScene.DEVICES -> devices(t, u, cx, cy, p)
            SettingsScene.LABS -> labs(t, u, cx, cy, p)
            SettingsScene.DIAGNOSTICS -> diagnostics(t, u, cx, cy, p)
            SettingsScene.ABOUT -> about(t, u, cx, cy, p)
        }
    }
}

private class Pal(val ink: Color, val soft: Color, val accent: Color, val strong: Color, val card: Color)

// --- small helpers ---------------------------------------------------------------------------------------------------

/** 0 before [a], 1 after [b], eased in between. */
private fun seg(t: Float, a: Float, b: Float): Float {
    val x = ((t - a) / (b - a)).coerceIn(0f, 1f)
    return x * x * (3 - 2 * x)
}

/** 1 while a tap is happening at [at] (a quick press and release), else 0. */
private fun press(t: Float, at: Float, width: Float = 0.06f): Float {
    val d = (t - at) / width
    return if (d < 0f || d > 1f) 0f else sin(d * PI.toFloat())
}

private fun mix(a: Float, b: Float, k: Float) = a + (b - a) * k

/** A rounded filled rectangle given by its centre. */
private fun DrawScope.box(c: Offset, w: Float, h: Float, r: Float, color: Color) =
    drawRoundRect(color, Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(r, r))

private fun DrawScope.frame(c: Offset, w: Float, h: Float, r: Float, fill: Color, line: Color, u: Float) {
    box(c, w, h, r, fill)
    drawRoundRect(line, Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(r, r), style = Stroke(2f * u))
}

/**
 * The tapping hand: a fingertip at [tip], a palm below it. It dips and shrinks a little on [press], and a soft
 * disc grows from the fingertip and fades, so the tap is easy to see. [show] fades the whole hand.
 */
private fun DrawScope.hand(tip: Offset, press: Float, show: Float, u: Float, p: Pal) {
    if (show <= 0.01f) return
    val dip = press * 4f * u
    val body = lerp(p.strong, p.card, 0.15f).copy(alpha = show)
    val edge = p.card.copy(alpha = show)
    val top = Offset(tip.x, tip.y + dip)
    if (press > 0.02f) drawCircle(p.accent.copy(alpha = 0.35f * press * show), radius = (10f + 14f * press) * u, center = tip)
    // Index finger and the other knuckles, then the palm.
    drawRoundRect(body, Offset(top.x - 4.5f * u, top.y), Size(9f * u, 30f * u), CornerRadius(4.5f * u, 4.5f * u))
    drawRoundRect(edge, Offset(top.x - 4.5f * u, top.y), Size(9f * u, 30f * u), CornerRadius(4.5f * u, 4.5f * u), style = Stroke(1.5f * u))
    drawRoundRect(body, Offset(top.x - 15f * u, top.y + 18f * u), Size(30f * u, 30f * u), CornerRadius(12f * u, 12f * u))
    drawRoundRect(edge, Offset(top.x - 15f * u, top.y + 18f * u), Size(30f * u, 30f * u), CornerRadius(12f * u, 12f * u), style = Stroke(1.5f * u))
}

/** Where the hand is, moving through [stops] (time, point) with easing, and how visible it is. */
private fun handAt(t: Float, stops: List<Pair<Float, Offset>>): Offset {
    if (t <= stops.first().first) return stops.first().second
    for (i in 1 until stops.size) {
        if (t <= stops[i].first) {
            val (t0, a) = stops[i - 1]
            val (t1, b) = stops[i]
            val k = seg(t, t0, t1)
            return Offset(mix(a.x, b.x, k), mix(a.y, b.y, k))
        }
    }
    return stops.last().second
}

/** The hand slides in, stays, and slides out, so each loop starts and ends clean. */
private fun handShow(t: Float) = seg(t, 0.02f, 0.12f) * (1f - seg(t, 0.86f, 0.97f))

// --- scenes ----------------------------------------------------------------------------------------------------------

private fun DrawScope.appearance(t: Float, u: Float, cx: Float, cy: Float, p: Pal) {
    val dark = seg(t, 0.32f, 0.42f) * (1f - seg(t, 0.72f, 0.82f))
    val screen = lerp(p.soft, p.strong, dark)
    frame(Offset(cx - 34 * u, cy), 62 * u, 116 * u, 12 * u, screen, p.ink, u)
    // Sun turning into a moon: a disc with a bite that slides in.
    val sunC = Offset(cx - 34 * u, cy - 22 * u)
    val disc = lerp(p.accent, p.soft, dark)
    drawCircle(disc, 11 * u, sunC)
    drawCircle(screen, 9 * u * dark, Offset(sunC.x + 6 * u, sunC.y - 3 * u))
    box(Offset(cx - 34 * u, cy + 12 * u), 40 * u, 8 * u, 4 * u, lerp(p.ink, p.card, 0.5f).copy(alpha = 0.6f))
    box(Offset(cx - 34 * u, cy + 28 * u), 28 * u, 8 * u, 4 * u, lerp(p.ink, p.card, 0.5f).copy(alpha = 0.6f))
    // The switch the hand taps.
    val sw = Offset(cx + 52 * u, cy - 6 * u)
    box(sw, 44 * u, 24 * u, 12 * u, lerp(p.soft, p.accent, dark))
    drawCircle(if (dark > 0.5f) p.card else p.ink, 8 * u, Offset(sw.x + mix(-10f, 10f, dark) * u, sw.y))
    val tip = handAt(t, listOf(0f to Offset(cx + 92 * u, cy + 60 * u), 0.28f to sw, 0.5f to sw, 0.62f to Offset(cx + 92 * u, cy + 60 * u), 0.70f to sw, 0.8f to sw, 1f to Offset(cx + 92 * u, cy + 60 * u)))
    hand(tip, maxOf(press(t, 0.32f), press(t, 0.74f)), handShow(t), u, p)
}

private fun DrawScope.tabs(t: Float, u: Float, cx: Float, cy: Float, p: Pal) {
    val k = seg(t, 0.34f, 0.5f) * (1f - seg(t, 0.76f, 0.9f))
    // Three tab cards fanned; the back one comes to the front when tapped.
    val slots = listOf(Offset(cx - 44 * u, cy + 10 * u), Offset(cx, cy), Offset(cx + 44 * u, cy - 10 * u))
    fun pos(i: Int): Offset = if (i == 0) Offset(mix(slots[0].x, slots[2].x, k), mix(slots[0].y, slots[2].y, k)) else Offset(mix(slots[i].x, slots[i - 1].x, k), mix(slots[i].y, slots[i - 1].y, k))
    val order = if (k < 0.5f) listOf(2, 1, 0) else listOf(2, 1, 0).let { listOf(1, 2, 0) }
    order.forEach { i ->
        val c = pos(i)
        val front = i == 0 && k > 0.5f
        frame(c, 72 * u, 84 * u, 10 * u, if (front) p.soft else lerp(p.soft, p.card, 0.35f), if (front) p.accent else p.ink, u)
        box(Offset(c.x, c.y - 28 * u), 52 * u, 8 * u, 4 * u, (if (front) p.accent else p.ink).copy(alpha = 0.7f))
        box(Offset(c.x - 6 * u, c.y - 4 * u), 40 * u, 7 * u, 3.5f * u, p.ink.copy(alpha = 0.3f))
    }
    val target = Offset(slots[0].x + 4 * u, slots[0].y + 30 * u)
    val tip = handAt(t, listOf(0f to Offset(cx - 110 * u, cy + 70 * u), 0.3f to target, 0.5f to target, 0.64f to Offset(cx - 110 * u, cy + 70 * u), 1f to Offset(cx - 110 * u, cy + 70 * u)))
    hand(tip, press(t, 0.34f), handShow(t), u, p)
}

private fun DrawScope.privacy(t: Float, u: Float, cx: Float, cy: Float, p: Pal) {
    val locked = seg(t, 0.34f, 0.46f) * (1f - seg(t, 0.78f, 0.9f))
    val shield = Path().apply {
        moveTo(cx, cy - 56 * u)
        lineTo(cx + 44 * u, cy - 40 * u)
        lineTo(cx + 44 * u, cy + 6 * u)
        quadraticTo(cx + 40 * u, cy + 40 * u, cx, cy + 58 * u)
        quadraticTo(cx - 40 * u, cy + 40 * u, cx - 44 * u, cy + 6 * u)
        lineTo(cx - 44 * u, cy - 40 * u)
        close()
    }
    drawPath(shield, lerp(p.soft, p.accent.copy(alpha = 0.25f), locked))
    drawPath(shield, lerp(p.ink, p.accent, locked), style = Stroke(2.5f * u))
    // Padlock: the shackle drops onto the body as it locks.
    val lift = (1f - locked) * 9 * u
    drawArc(p.strong, 180f, 180f, false, Offset(cx - 11 * u, cy - 22 * u - lift), Size(22 * u, 24 * u), style = Stroke(4 * u))
    box(Offset(cx, cy + 4 * u), 38 * u, 28 * u, 7 * u, p.strong)
    drawCircle(p.card, 4 * u, Offset(cx, cy + 3 * u))
    // Tracker dots bounce off the shield once it is up.
    for (i in 0 until 3) {
        val phase = ((t * 3f + i / 3f) % 1f)
        val from = Offset(cx - 110 * u + i * 6 * u, cy - 30 * u + i * 26 * u)
        val x = mix(from.x, cx - 52 * u, minOf(phase * 1.6f, 1f))
        val y = from.y - (if (phase > 0.62f) (phase - 0.62f) * 60 * u * locked else 0f)
        drawCircle(p.ink.copy(alpha = (1f - seg(phase, 0.75f, 1f)) * (0.35f + 0.4f * locked)), 5 * u, Offset(x, y))
    }
    val tip = handAt(t, listOf(0f to Offset(cx + 120 * u, cy + 70 * u), 0.3f to Offset(cx + 6 * u, cy + 14 * u), 0.5f to Offset(cx + 6 * u, cy + 14 * u), 0.62f to Offset(cx + 120 * u, cy + 70 * u), 1f to Offset(cx + 120 * u, cy + 70 * u)))
    hand(tip, press(t, 0.34f), handShow(t), u, p)
}

private fun DrawScope.search(t: Float, u: Float, cx: Float, cy: Float, p: Pal) {
    val bar = Offset(cx, cy - 34 * u)
    box(bar, 190 * u, 34 * u, 17 * u, p.soft)
    drawCircle(p.ink, 7 * u, Offset(bar.x - 80 * u, bar.y - 1 * u), style = Stroke(2.2f * u))
    drawLine(p.ink, Offset(bar.x - 75 * u, bar.y + 4 * u), Offset(bar.x - 70 * u, bar.y + 9 * u), 2.2f * u)
    // Typed letters appear one after another.
    for (i in 0 until 5) {
        val a = seg(t, 0.36f + i * 0.04f, 0.4f + i * 0.04f)
        box(Offset(bar.x - 52 * u + i * 13 * u, bar.y), 9 * u, 9 * u * a, 3 * u, p.strong.copy(alpha = a))
    }
    // Suggestions slide in under the bar.
    for (i in 0 until 3) {
        val a = seg(t, 0.58f + i * 0.05f, 0.66f + i * 0.05f) * (1f - seg(t, 0.86f, 0.95f))
        val y = cy + (8 + i * 28) * u + (1f - a) * 10 * u
        box(Offset(cx - (3 - i) * 8 * u, y), (150 - i * 14) * u, 20 * u, 10 * u, p.soft.copy(alpha = a))
        drawCircle(p.accent.copy(alpha = a), 4 * u, Offset(cx - 80 * u - (3 - i) * 8 * u + 12 * u, y))
    }
    val tip = handAt(t, listOf(0f to Offset(cx + 120 * u, cy + 70 * u), 0.3f to Offset(bar.x + 50 * u, bar.y + 4 * u), 0.5f to Offset(bar.x + 50 * u, bar.y + 4 * u), 0.62f to Offset(cx + 120 * u, cy + 70 * u), 1f to Offset(cx + 120 * u, cy + 70 * u)))
    hand(tip, press(t, 0.32f), handShow(t), u, p)
}

private fun DrawScope.home(t: Float, u: Float, cx: Float, cy: Float, p: Pal) {
    val c = Offset(cx - 28 * u, cy)
    frame(c, 76 * u, 124 * u, 12 * u, p.soft, p.ink, u)
    // Tiles pop onto the new tab, one by one, after the home button is tapped.
    for (i in 0 until 4) {
        val a = seg(t, 0.4f + i * 0.05f, 0.5f + i * 0.05f) * (1f - seg(t, 0.82f, 0.92f))
        val tc = Offset(c.x - 15 * u + (i % 2) * 30 * u, c.y - 28 * u + (i / 2) * 30 * u)
        box(tc, 24 * u * a, 24 * u * a, 7 * u, if (i == 0) p.accent else p.ink.copy(alpha = 0.45f))
    }
    val homeBtn = Offset(cx + 60 * u, cy + 8 * u)
    val on = press(t, 0.34f)
    drawCircle(p.soft, 26 * u, homeBtn)
    val house = Path().apply {
        moveTo(homeBtn.x - 14 * u, homeBtn.y)
        lineTo(homeBtn.x, homeBtn.y - 13 * u)
        lineTo(homeBtn.x + 14 * u, homeBtn.y)
        lineTo(homeBtn.x + 10 * u, homeBtn.y)
        lineTo(homeBtn.x + 10 * u, homeBtn.y + 12 * u)
        lineTo(homeBtn.x - 10 * u, homeBtn.y + 12 * u)
        lineTo(homeBtn.x - 10 * u, homeBtn.y)
        close()
    }
    drawPath(house, lerp(p.strong, p.accent, on))
    val tip = handAt(t, listOf(0f to Offset(cx + 120 * u, cy + 70 * u), 0.3f to homeBtn, 0.5f to homeBtn, 0.62f to Offset(cx + 120 * u, cy + 70 * u), 1f to Offset(cx + 120 * u, cy + 70 * u)))
    hand(tip, press(t, 0.34f), handShow(t), u, p)
}

private fun DrawScope.devices(t: Float, u: Float, cx: Float, cy: Float, p: Pal) {
    val phone = Offset(cx - 70 * u, cy + 6 * u)
    val tablet = Offset(cx + 66 * u, cy + 6 * u)
    frame(phone, 46 * u, 84 * u, 9 * u, p.soft, p.ink, u)
    frame(tablet, 96 * u, 70 * u, 9 * u, p.soft, p.ink, u)
    box(Offset(phone.x, phone.y - 24 * u), 28 * u, 7 * u, 3.5f * u, p.ink.copy(alpha = 0.4f))
    // The page lifts off the phone, travels in an arc, and lands on the tablet.
    val fly = seg(t, 0.4f, 0.66f)
    val land = seg(t, 0.66f, 0.74f)
    val gone = seg(t, 0.84f, 0.94f)
    val from = Offset(phone.x, phone.y - 8 * u)
    val to = Offset(tablet.x, tablet.y)
    val x = mix(from.x, to.x, fly)
    val y = mix(from.y, to.y, fly) - sin(fly * PI.toFloat()) * 44 * u
    if (fly > 0f && fly < 1f) {
        val s = 0.7f + 0.3f * sin(fly * PI.toFloat())
        frame(Offset(x, y), 26 * u * s, 32 * u * s, 5 * u, p.card, p.accent, u)
    }
    if (land > 0f) {
        val s = (0.6f + 0.4f * land) * (1f - gone)
        frame(to, 52 * u * s, 44 * u * s, 6 * u, p.card, p.accent, u)
        box(Offset(to.x, to.y - 10 * u * s), 34 * u * s, 6 * u * s, 3 * u, p.accent.copy(alpha = 0.7f))
        box(Offset(to.x - 4 * u * s, to.y + 6 * u * s), 26 * u * s, 5 * u * s, 2.5f * u, p.ink.copy(alpha = 0.35f))
    }
    // Three dots between the devices pulse in turn while the page travels.
    for (i in 0 until 3) {
        val a = if (fly in 0.02f..0.99f) 0.25f + 0.6f * ((sin((t * 12f - i) * 1.2f) + 1f) / 2f) else 0.25f
        drawCircle(p.ink.copy(alpha = a), 3 * u, Offset(cx - 14 * u + i * 14 * u, cy + 48 * u))
    }
    val tap = Offset(phone.x, phone.y + 12 * u)
    val tip = handAt(t, listOf(0f to Offset(cx - 130 * u, cy + 80 * u), 0.3f to tap, 0.46f to tap, 0.6f to Offset(cx - 130 * u, cy + 80 * u), 1f to Offset(cx - 130 * u, cy + 80 * u)))
    hand(tip, press(t, 0.36f), handShow(t), u, p)
}

private fun DrawScope.labs(t: Float, u: Float, cx: Float, cy: Float, p: Pal) {
    val on = seg(t, 0.34f, 0.5f) * (1f - seg(t, 0.8f, 0.92f))
    val flask = Path().apply {
        moveTo(cx - 12 * u, cy - 56 * u)
        lineTo(cx + 12 * u, cy - 56 * u)
        lineTo(cx + 12 * u, cy - 20 * u)
        lineTo(cx + 44 * u, cy + 44 * u)
        quadraticTo(cx + 48 * u, cy + 56 * u, cx + 34 * u, cy + 56 * u)
        lineTo(cx - 34 * u, cy + 56 * u)
        quadraticTo(cx - 48 * u, cy + 56 * u, cx - 44 * u, cy + 44 * u)
        lineTo(cx - 12 * u, cy - 20 * u)
        close()
    }
    drawPath(flask, p.soft)
    // Liquid rises when the experiment is switched on.
    val level = cy + 56 * u - (14 + 22 * on) * u
    clipPath(flask) { drawRect(p.accent.copy(alpha = 0.85f), Offset(cx - 60 * u, level), Size(120 * u, 120 * u)) }
    drawPath(flask, p.ink, style = Stroke(2.5f * u))
    for (i in 0 until 3) {
        val ph = (t * 2f + i / 3f) % 1f
        drawCircle(p.card.copy(alpha = on * (1f - ph)), (3 + i) * u, Offset(cx - 10 * u + i * 10 * u, level - ph * 28 * u * on))
    }
    val sw = Offset(cx + 86 * u, cy - 4 * u)
    box(sw, 40 * u, 22 * u, 11 * u, lerp(p.soft, p.accent, on))
    drawCircle(if (on > 0.5f) p.card else p.ink, 7 * u, Offset(sw.x + mix(-9f, 9f, on) * u, sw.y))
    val tip = handAt(t, listOf(0f to Offset(cx + 128 * u, cy + 70 * u), 0.3f to sw, 0.5f to sw, 0.62f to Offset(cx + 128 * u, cy + 70 * u), 1f to Offset(cx + 128 * u, cy + 70 * u)))
    hand(tip, press(t, 0.34f), handShow(t), u, p)
}

private fun DrawScope.diagnostics(t: Float, u: Float, cx: Float, cy: Float, p: Pal) {
    val panel = Offset(cx, cy - 4 * u)
    frame(panel, 210 * u, 100 * u, 12 * u, p.soft, p.ink, u)
    val draw = seg(t, 0.3f, 0.78f)
    val pts = listOf(0f to 0.6f, 0.15f to 0.55f, 0.28f to 0.25f, 0.4f to 0.7f, 0.55f to 0.4f, 0.7f to 0.45f, 0.85f to 0.2f, 1f to 0.35f)
    val path = Path()
    var last = Offset.Zero
    pts.forEachIndexed { i, (px, py) ->
        val o = Offset(panel.x - 90 * u + px * 180 * u, panel.y - 34 * u + py * 68 * u)
        if (px <= draw) { if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y); last = o }
    }
    if (draw > 0.01f) drawPath(path, p.accent, style = Stroke(3f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    if (draw in 0.02f..0.99f) drawCircle(p.accent, 5 * u, last)
    // Log lines under the chart.
    for (i in 0 until 2) box(Offset(panel.x - 40 * u + i * 8 * u, panel.y + 34 * u + i * 0 * u), (110 - i * 30) * u, 5 * u, 2.5f * u, p.ink.copy(alpha = 0.3f * seg(t, 0.5f + i * 0.08f, 0.6f + i * 0.08f)))
    val tip = handAt(t, listOf(0f to Offset(cx + 130 * u, cy + 78 * u), 0.2f to Offset(panel.x + 70 * u, panel.y + 30 * u), 0.34f to Offset(panel.x + 70 * u, panel.y + 30 * u), 0.5f to Offset(cx + 130 * u, cy + 78 * u), 1f to Offset(cx + 130 * u, cy + 78 * u)))
    hand(tip, press(t, 0.26f), handShow(t), u, p)
}

private fun DrawScope.about(t: Float, u: Float, cx: Float, cy: Float, p: Pal) {
    val on = seg(t, 0.32f, 0.46f) * (1f - seg(t, 0.8f, 0.92f))
    frame(Offset(cx - 30 * u, cy), 70 * u, 120 * u, 12 * u, p.soft, p.ink, u)
    val badge = Offset(cx - 30 * u, cy - 14 * u)
    drawCircle(lerp(p.ink.copy(alpha = 0.35f), p.accent, on), (18 + 4 * on) * u, badge)
    box(Offset(badge.x, badge.y + 4 * u), 5 * u, 16 * u, 2.5f * u, p.card)
    drawCircle(p.card, 3 * u, Offset(badge.x, badge.y - 9 * u))
    box(Offset(cx - 30 * u, cy + 30 * u), 42 * u * on, 7 * u, 3.5f * u, p.ink.copy(alpha = 0.4f))
    // A small star twinkles beside the badge once it is tapped.
    val s = on * (0.8f + 0.2f * sin(t * 18f))
    val star = Offset(cx + 30 * u, cy - 40 * u)
    drawLine(p.accent, Offset(star.x - 10 * u * s, star.y), Offset(star.x + 10 * u * s, star.y), 3 * u)
    drawLine(p.accent, Offset(star.x, star.y - 10 * u * s), Offset(star.x, star.y + 10 * u * s), 3 * u)
    val tip = handAt(t, listOf(0f to Offset(cx + 110 * u, cy + 70 * u), 0.28f to badge, 0.46f to badge, 0.6f to Offset(cx + 110 * u, cy + 70 * u), 1f to Offset(cx + 110 * u, cy + 70 * u)))
    hand(tip, press(t, 0.34f), handShow(t), u, p)
}
