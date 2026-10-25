package com.example.mybase.feature.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.core.model.TaskPriority
import com.example.mybase.ui.theme.MyBaseColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.temporal.WeekFields

@Composable
fun TaskEditorDialog(
    state: PlannerUiState,
    onTitleChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onChooseDate: () -> Unit,
    onClearDate: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val dueDateLabel = state.draftDueDateEpochDay?.let {
        LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
    } ?: "No due date"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MyBaseColors.White,
        title = { Text(if (state.editingTaskId == null) "A task for your day" else "Edit task", color = MyBaseColors.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.draftTitle,
                    onValueChange = onTitleChange,
                    label = { Text("Task") },
                    singleLine = true
                )
                Text("Priority", fontSize = 12.sp, color = MyBaseColors.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { priority ->
                        FilterChip(
                            selected = state.draftPriority == priority,
                            onClick = { onPriorityChange(priority) },
                            label = { Text(priority.label) }
                        )
                    }
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    TextButton(onClick = onChooseDate) { Text("Due: $dueDateLabel", color = MyBaseColors.ButtonRose) }
                    if (state.draftDueDateEpochDay != null) TextButton(onClick = onClearDate) { Text("Clear", color = MyBaseColors.Muted) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = state.draftTitle.isNotBlank()) { Text("Save task", color = MyBaseColors.Ink) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = MyBaseColors.ButtonRose) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = MyBaseColors.Muted) }
            }
        }
    )
}

@Composable
fun PlannerDatePickerDialog(
    initialEpochDay: Long?,
    tasks: List<CapturedEntry>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember(initialEpochDay) { mutableStateOf(initialEpochDay?.let(LocalDate::ofEpochDay)) }
    var month by remember(initialEpochDay) { mutableStateOf(YearMonth.from(selectedDate ?: LocalDate.now())) }
    val locale = Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MyBaseColors.White,
        title = { Text("Choose a due date", color = MyBaseColors.Ink) },
        text = { PlannerCalendar(month, selectedDate, tasks, firstDay, locale, onSelectDate = { selectedDate = it }, onMonth = { month = month.plusMonths(it.toLong()) }) },
        confirmButton = {
            TextButton(
                onClick = { selectedDate?.let { onSelect(it.toEpochDay()) } },
                enabled = selectedDate != null
            ) { Text("Choose", color = MyBaseColors.ButtonRose) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MyBaseColors.Muted) } }
    )
}

@Composable
private fun PlannerCalendar(
    month: YearMonth,
    selectedDate: LocalDate?,
    tasks: List<CapturedEntry>,
    firstDay: DayOfWeek,
    locale: Locale,
    onSelectDate: (LocalDate) -> Unit,
    onMonth: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onMonth(-1) }) { Text("‹", color = MyBaseColors.LavenderDark, fontSize = 24.sp) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), Modifier.weight(1f), color = MyBaseColors.Ink)
            TextButton(onClick = { onMonth(1) }) { Text("›", color = MyBaseColors.LavenderDark, fontSize = 24.sp) }
        }
        val weekdays = (0..6).map { firstDay.plus(it.toLong()) }
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(day.getDisplayName(java.time.format.TextStyle.NARROW, locale), Modifier.weight(1f), color = MyBaseColors.Muted, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        val cells = PlannerCalendarMapper.monthCells(month, firstDay)
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) Box(Modifier.weight(1f).size(40.dp))
                    else {
                        val counts = PlannerCalendarMapper.countsForDay(tasks, date.toEpochDay())
                        val selected = date == selectedDate
                        val markerColor = when {
                            counts.hasPending -> MyBaseColors.ButtonRose
                            counts.completed > 0 -> MyBaseColors.MintDark
                            else -> Color.Transparent
                        }
                        val accessible = buildString {
                            append(date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", locale)))
                            when {
                                counts.pending > 0 && counts.completed > 0 -> append(", ${counts.pending} pending, ${counts.completed} completed tasks")
                                counts.pending > 0 -> append(", ${counts.pending} pending tasks")
                                counts.completed > 0 -> append(", ${counts.completed} completed tasks")
                                else -> append(", no tasks")
                            }
                        }
                        Column(
                            Modifier.weight(1f).padding(vertical = 1.dp).semantics { contentDescription = accessible }.clip(CircleShape)
                                .background(if (selected) MyBaseColors.Lavender else Color.Transparent)
                                .clickable { onSelectDate(date) }.padding(vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(date.dayOfMonth.toString(), color = if (selected) MyBaseColors.LavenderDark else MyBaseColors.Ink, fontSize = 12.sp)
                            Box(Modifier.size(5.dp).clip(CircleShape).background(markerColor))
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CalendarLegendDot(MyBaseColors.ButtonRose, "Pending")
            CalendarLegendDot(MyBaseColors.MintDark, "Completed only")
        }
        Text("Pending markers take priority when a day has both.", color = MyBaseColors.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun CalendarLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, color = MyBaseColors.Muted, fontSize = 10.sp)
    }
}
