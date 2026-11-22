package com.makemission.ora.browser

import com.makemission.ora.ui.pressFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.onboarding.TextAction
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle as DayStyle
import java.time.temporal.IsoFields
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Shows the page prompt at the front of [PagePrompts]. It is an [OraDialog], so the card grows out of the spot the
 * user touched (the date field, the list, the button that asked) and shrinks back into it on Back or a tap outside.
 */
@Composable
fun PagePromptHost() {
    val queue by PagePrompts.queue.collectAsState()
    val prompt = queue.firstOrNull() ?: return
    key(prompt) {
        val leave = { PagePrompts.finish(prompt); prompt.cancel() }
        when (prompt) {
            is PagePrompt.Message -> MessageDialog(prompt, leave)
            is PagePrompt.Text -> TextDialog(prompt, leave)
            is PagePrompt.Choices -> ChoiceDialog(prompt, leave)
            is PagePrompt.DateTime -> DateTimeDialog(prompt, leave)
        }
    }
}

private fun answered(prompt: PagePrompt, run: () -> Unit) {
    PagePrompts.finish(prompt)
    run()
}

@Composable
private fun MessageDialog(prompt: PagePrompt.Message, leave: () -> Unit) {
    OraDialog(
        onDismissRequest = leave,
        title = { Text(prompt.title?.takeIf { it.isNotBlank() } ?: "This page says") },
        text = { Text(prompt.message) },
        dismissButton = if (prompt.asksYesNo) {
            { TextAction("Cancel") { answered(prompt) { prompt.onDone(false) } } }
        } else null,
        confirmButton = { TextAction(prompt.okLabel) { answered(prompt) { prompt.onDone(true) } } },
    )
}

@Composable
private fun TextDialog(prompt: PagePrompt.Text, leave: () -> Unit) {
    var value by remember { mutableStateOf(prompt.initial) }
    val colors = browserColors()
    OraDialog(
        onDismissRequest = leave,
        title = { Text(prompt.title?.takeIf { it.isNotBlank() } ?: "This page asks") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (prompt.message.isNotBlank()) Text(prompt.message)
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, color = colors.AddressBarText),
                    cursorBrush = SolidColor(colors.Accent),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.RadiusSmall)).background(colors.AddressBarBackground).padding(14.dp),
                )
            }
        },
        dismissButton = { TextAction("Cancel") { answered(prompt) { prompt.onDone(null) } } },
        confirmButton = { TextAction("OK") { answered(prompt) { prompt.onDone(value) } } },
    )
}

@Composable
private fun ChoiceDialog(prompt: PagePrompt.Choices, leave: () -> Unit) {
    val colors = browserColors()
    var picked by remember { mutableStateOf(prompt.rows.filter { it.selected }.map { it.id }.toSet()) }
    OraDialog(
        onDismissRequest = leave,
        title = prompt.title?.takeIf { it.isNotBlank() }?.let { t -> { Text(t) } },
        text = {
            Column {
                if (prompt.rows.none { !it.heading }) Text("Nothing to choose from.")
                prompt.rows.forEach { row ->
                    if (row.heading) {
                        Text(row.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
                        return@forEach
                    }
                    val on = row.id in picked
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn44()
                            .pressFeedback().clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
                            .background(if (on) colors.AddressBarBackground else Color.Transparent)
                            .clickable(enabled = !row.disabled) {
                                if (prompt.multiple) picked = if (on) picked - row.id else picked + row.id
                                else answered(prompt) { prompt.onDone(listOf(row.id)) }
                            }
                            .padding(start = if (row.indent) 24.dp else 12.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            row.label.ifBlank { " " },
                            modifier = Modifier.weight(1f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (row.disabled) colors.DisabledIcon else colors.AddressBarText,
                        )
                        if (on) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = colors.Accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        dismissButton = { TextAction("Cancel") { answered(prompt) { prompt.onDone(null) } } },
        confirmButton = {
            if (prompt.multiple) TextAction("Done") { answered(prompt) { prompt.onDone(prompt.rows.filter { it.id in picked }.map { it.id }) } }
        },
    )
}

private fun Modifier.heightIn44(): Modifier = this.then(Modifier.height(BrowserTokens.PromptCell))

// ---- Date and time -------------------------------------------------------------------------------------------

private fun parseDate(text: String?): LocalDate? = text?.let {
    runCatching {
        when {
            "-W" in it -> LocalDate.now().with(IsoFields.WEEK_BASED_YEAR, it.substringBefore("-W").toLong())
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, it.substringAfter("-W").take(2).toLong())
            it.length == 7 -> YearMonth.parse(it).atDay(1)
            else -> LocalDate.parse(it.take(10))
        }
    }.getOrNull()
}

private fun parseTime(text: String?): LocalTime? = text?.let {
    runCatching { LocalTime.parse(if ('T' in it) it.substringAfter('T') else it).withSecond(0).withNano(0) }.getOrNull()
}

@Composable
private fun DateTimeDialog(prompt: PagePrompt.DateTime, leave: () -> Unit) {
    val colors = browserColors()
    val kind = prompt.kind
    val today = LocalDate.now()
    val start = parseDate(prompt.initial) ?: today
    val minDate = parseDate(prompt.min)
    val maxDate = parseDate(prompt.max)
    var date by remember { mutableStateOf(start) }
    var month by remember { mutableStateOf(YearMonth.from(start)) }
    var time by remember { mutableStateOf(parseTime(prompt.initial) ?: LocalTime.now().withSecond(0).withNano(0)) }
    val hasDate = kind != PickerKind.TIME
    val hasTime = kind == PickerKind.TIME || kind == PickerKind.DATE_TIME

    fun value(): String = when (kind) {
        PickerKind.DATE -> date.toString()
        PickerKind.MONTH -> YearMonth.from(date).toString()
        PickerKind.WEEK -> "%d-W%02d".format(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
        PickerKind.TIME -> "%02d:%02d".format(time.hour, time.minute)
        PickerKind.DATE_TIME -> LocalDateTime.of(date, time).toString().take(16)
    }

    OraDialog(
        onDismissRequest = leave,
        title = { Text(prompt.title?.takeIf { it.isNotBlank() } ?: when (kind) {
            PickerKind.TIME -> "Choose a time"
            PickerKind.MONTH -> "Choose a month"
            PickerKind.WEEK -> "Choose a week"
            PickerKind.DATE_TIME -> "Choose a date and time"
            PickerKind.DATE -> "Choose a date"
        }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasDate) {
                    if (kind == PickerKind.MONTH) MonthGrid(month, date, minDate, maxDate, { month = it }) { date = it }
                    else CalendarGrid(month, date, kind == PickerKind.WEEK, minDate, maxDate, { month = it }) { date = it }
                }
                if (hasTime) TimeSteppers(time, (prompt.stepSeconds ?: 60) / 60) { time = it }
                // Quality of life: jump straight to today or now, and clear a field the page lets you empty.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    TextAction(if (kind == PickerKind.TIME) "Now" else "Today") {
                        date = today; month = YearMonth.from(today); time = LocalTime.now().withSecond(0).withNano(0)
                    }
                    TextAction("Clear") { answered(prompt) { prompt.onDone("") } }
                }
            }
        },
        dismissButton = { TextAction("Cancel") { answered(prompt) { prompt.onDone(null) } } },
        confirmButton = { TextAction("Set") { answered(prompt) { prompt.onDone(value()) } } },
    )
}

private fun inRange(day: LocalDate, min: LocalDate?, max: LocalDate?) = (min == null || !day.isBefore(min)) && (max == null || !day.isAfter(max))

@Composable
private fun PickerHeader(title: String, onPrev: () -> Unit, onNext: () -> Unit) {
    val colors = browserColors()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(BrowserTokens.PromptCell).pressFeedback().clip(CircleShape).clickable(onClick = onPrev), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous", tint = colors.IconColor)
        }
        Text(title, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)
        Box(Modifier.size(BrowserTokens.PromptCell).clip(CircleShape).clickable(onClick = onNext), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Next", tint = colors.IconColor)
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth, selected: LocalDate, wholeWeek: Boolean, min: LocalDate?, max: LocalDate?,
    onMonth: (YearMonth) -> Unit, onPick: (LocalDate) -> Unit,
) {
    val colors = browserColors()
    val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val lead = (month.atDay(1).dayOfWeek.value - first.value + 7) % 7
    val weeks = (lead + month.lengthOfMonth() + 6) / 7
    Column {
        PickerHeader(
            "${month.month.getDisplayName(DayStyle.FULL, Locale.getDefault())} ${month.year}",
            { onMonth(month.minusMonths(1)) }, { onMonth(month.plusMonths(1)) },
        )
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                val d = DayOfWeek.of(((first.value - 1 + i) % 7) + 1)
                Text(d.getDisplayName(DayStyle.NARROW, Locale.getDefault()), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
            }
        }
        Spacer(Modifier.height(4.dp))
        for (w in 0 until weeks) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val n = w * 7 + i - lead + 1
                    if (n < 1 || n > month.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f).height(BrowserTokens.PromptCell))
                        continue
                    }
                    val day = month.atDay(n)
                    val ok = inRange(day, min, max)
                    val on = if (wholeWeek) day.get(IsoFields.WEEK_BASED_YEAR) == selected.get(IsoFields.WEEK_BASED_YEAR) &&
                        day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) == selected.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) else day == selected
                    Box(Modifier.weight(1f).height(BrowserTokens.PromptCell), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(40.dp).pressFeedback().clip(CircleShape)
                                .background(if (on) colors.Accent else Color.Transparent)
                                .clickable(enabled = ok) { onPick(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$n", fontSize = 14.sp, fontWeight = if (on || day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal,
                                color = when { on -> colors.GridBackground; !ok -> colors.DisabledIcon; else -> colors.AddressBarText },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, selected: LocalDate, min: LocalDate?, max: LocalDate?, onYear: (YearMonth) -> Unit, onPick: (LocalDate) -> Unit) {
    val colors = browserColors()
    Column {
        PickerHeader("${month.year}", { onYear(month.minusYears(1)) }, { onYear(month.plusYears(1)) })
        for (row in 0 until 4) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 3) {
                    val m = row * 3 + col + 1
                    val day = LocalDate.of(month.year, m, 1)
                    val ok = (min == null || !day.plusMonths(1).minusDays(1).isBefore(min)) && (max == null || !day.isAfter(max))
                    val on = selected.year == month.year && selected.monthValue == m
                    Box(Modifier.weight(1f).height(BrowserTokens.PromptCell + 4.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.height(40.dp).width(80.dp).pressFeedback().clip(CircleShape).background(if (on) colors.Accent else Color.Transparent).clickable(enabled = ok) { onPick(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.month.getDisplayName(DayStyle.SHORT, Locale.getDefault()), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                color = when { on -> colors.GridBackground; !ok -> colors.DisabledIcon; else -> colors.AddressBarText },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSteppers(time: LocalTime, stepMinutes: Int, onChange: (LocalTime) -> Unit) {
    val colors = browserColors()
    val step = stepMinutes.coerceIn(1, 60).let { if (it == 1) 5 else it }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Stepper("%02d".format(time.hour), "hour", { onChange(time.plusHours(1)) }, { onChange(time.minusHours(1)) }) { typed ->
            typed.toIntOrNull()?.let { onChange(time.withHour(it.coerceIn(0, 23))) }
        }
        Text(":", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText, modifier = Modifier.padding(horizontal = 8.dp))
        Stepper("%02d".format(time.minute), "minute", { onChange(time.plusMinutes(step.toLong())) }, { onChange(time.minusMinutes(step.toLong())) }) { typed ->
            typed.toIntOrNull()?.let { onChange(time.withMinute(it.coerceIn(0, 59))) }
        }
    }
}

/** A number with a chevron above and below. The number can also be typed. Going up past the top wraps round. */
@Composable
private fun Stepper(value: String, what: String, up: () -> Unit, down: () -> Unit, typed: (String) -> Unit) {
    val colors = browserColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(BrowserTokens.PromptCell).pressFeedback().clip(CircleShape).clickable(onClick = up), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "More $what", tint = colors.IconColor)
        }
        BasicTextField(
            value = value,
            onValueChange = { typed(it.filter(Char::isDigit).takeLast(2)) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(colors.Accent),
            modifier = Modifier.width(72.dp).clip(RoundedCornerShape(BrowserTokens.RadiusSmall)).background(colors.AddressBarBackground).padding(vertical = 10.dp),
        )
        Box(Modifier.size(BrowserTokens.PromptCell).pressFeedback().clip(CircleShape).clickable(onClick = down), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Less $what", tint = colors.IconColor)
        }
    }
}
