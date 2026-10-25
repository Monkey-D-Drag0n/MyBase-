package com.example.mybase.feature.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.core.model.TaskPriority
import com.example.mybase.ui.theme.MyBaseColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlannerScreen(viewModel: PlannerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 850.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Planner", fontSize = 25.sp, color = MyBaseColors.Ink)
                    Text("A gentle plan for what matters", fontSize = 13.sp, color = MyBaseColors.Muted)
                }
                Button(
                    onClick = viewModel::showCreateTask,
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MyBaseColors.ButtonRose)
                ) { Text("＋  Add task") }
            }

            if (!wide) {
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.tab == PlannerTab.Day, onClick = viewModel::showDayTab, label = { Text("Day") })
                    FilterChip(selected = state.tab == PlannerTab.Upcoming, onClick = viewModel::showUpcomingTab, label = { Text("Upcoming") })
                }
            }

            Spacer(Modifier.height(14.dp))
            if (wide) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DayPanel(
                        state = state,
                        modifier = Modifier.weight(1.15f),
                        onPreviousDay = { viewModel.moveSelectedDay(-1) },
                        onNextDay = { viewModel.moveSelectedDay(1) },
                        onToday = viewModel::selectToday,
                        onSelectCalendarDay = viewModel::selectDay,
                        onCreateTask = viewModel::showCreateTask,
                        onComplete = viewModel::toggleCompletion,
                        onEdit = viewModel::editTask,
                        onDelete = viewModel::deleteTask
                    )
                    UpcomingPanel(
                        state = state,
                        modifier = Modifier.weight(.85f),
                        onComplete = viewModel::toggleCompletion,
                        onEdit = viewModel::editTask,
                        onDelete = viewModel::deleteTask
                    )
                }
            } else {
                when (state.tab) {
                    PlannerTab.Day -> DayPanel(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onPreviousDay = { viewModel.moveSelectedDay(-1) },
                        onNextDay = { viewModel.moveSelectedDay(1) },
                        onToday = viewModel::selectToday,
                        onSelectCalendarDay = viewModel::selectDay,
                        onCreateTask = viewModel::showCreateTask,
                        onComplete = viewModel::toggleCompletion,
                        onEdit = viewModel::editTask,
                        onDelete = viewModel::deleteTask
                    )
                    PlannerTab.Upcoming -> UpcomingPanel(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onComplete = viewModel::toggleCompletion,
                        onEdit = viewModel::editTask,
                        onDelete = viewModel::deleteTask
                    )
                }
            }
        }
    }

    if (state.isEditorVisible && !state.isDatePickerVisible) {
        TaskEditorDialog(
            state = state,
            onTitleChange = viewModel::updateDraftTitle,
            onPriorityChange = viewModel::updateDraftPriority,
            onChooseDate = viewModel::showDatePicker,
            onClearDate = { viewModel.setDraftDueDate(null) },
            onSave = viewModel::saveTask,
            onDismiss = viewModel::hideEditor,
            onDelete = state.editingTaskId?.let { id -> ({ viewModel.deleteTask(id) }) }
        )
    }
    if (state.isDatePickerVisible) {
        PlannerDatePickerDialog(
            initialEpochDay = state.draftDueDateEpochDay,
            tasks = state.tasks,
            onSelect = viewModel::setDraftDueDate,
            onDismiss = viewModel::hideDatePicker
        )
    }
}

@Composable
private fun DayPanel(
    state: PlannerUiState,
    modifier: Modifier,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onSelectCalendarDay: (Long) -> Unit,
    onCreateTask: () -> Unit,
    onComplete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val day = LocalDate.ofEpochDay(state.selectedDayEpochDay)
    val today = day == LocalDate.now()
    LazyColumn(
        modifier.clip(RoundedCornerShape(24.dp)).background(MyBaseColors.White),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", Modifier.clickable(onClick = onPreviousDay), fontSize = 29.sp, color = MyBaseColors.Muted)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(if (today) "Today" else day.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())), fontSize = 18.sp, color = MyBaseColors.Ink)
                    Text(day.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())), fontSize = 12.sp, color = MyBaseColors.Muted)
                }
                if (!today) FilterChip(selected = false, onClick = onToday, label = { Text("Today") })
                Text("›", Modifier.clickable(onClick = onNextDay), fontSize = 29.sp, color = MyBaseColors.Muted)
            }
        }
        item {
            PlannerMonthCalendar(
                selectedEpochDay = state.selectedDayEpochDay,
                tasks = state.tasks,
                onSelectDay = onSelectCalendarDay
            )
        }
        if (state.selectedDayTasks.isEmpty()) item { EmptyPlannerMessage("Nothing planned for this day", "Add a task here, or move to another day.", onCreateTask) }
        else items(state.selectedDayTasks, key = { it.id }) { task -> TaskRow(task, onComplete, onEdit, onDelete) }

        if (state.unscheduledTasks.isNotEmpty()) {
            item { PlannerSectionTitle("Unscheduled", "Give these tasks a day when you’re ready") }
            items(state.unscheduledTasks, key = { it.id }) { task -> TaskRow(task, onComplete, onEdit, onDelete) }
        }
    }
}

@Composable
private fun UpcomingPanel(
    state: PlannerUiState,
    modifier: Modifier,
    onComplete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier.clip(RoundedCornerShape(24.dp)).background(MyBaseColors.White),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 2.dp)) {
                Text("Coming up", fontSize = 18.sp, color = MyBaseColors.Ink)
                Text("Open tasks with a due date", fontSize = 12.sp, color = MyBaseColors.Muted)
            }
        }
        if (state.overdueTasks.isNotEmpty()) {
            item { PlannerSectionTitle("Past due", "A kind reminder, not a judgment") }
            items(state.overdueTasks, key = { it.id }) { task -> TaskRow(task, onComplete, onEdit, onDelete) }
        }
        if (state.upcomingTasks.isNotEmpty()) {
            item { PlannerSectionTitle("Upcoming", "Sorted by due day") }
            items(state.upcomingTasks, key = { it.id }) { task -> TaskRow(task, onComplete, onEdit, onDelete) }
        }
        if (state.overdueTasks.isEmpty() && state.upcomingTasks.isEmpty()) {
            item { EmptyPlannerMessage("No upcoming tasks", "Tasks with due dates will be gathered here.") }
        }
    }
}

@Composable
private fun PlannerMonthCalendar(
    selectedEpochDay: Long,
    tasks: List<CapturedEntry>,
    onSelectDay: (Long) -> Unit
) {
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    var month by remember(selectedEpochDay) { mutableStateOf(YearMonth.from(selectedDate)) }
    val locale = Locale.getDefault()
    val firstDay = java.time.temporal.WeekFields.of(locale).firstDayOfWeek
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(MyBaseColors.Sidebar).padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = month.minusMonths(1) }, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 1.dp)) { Text("‹", color = MyBaseColors.LavenderDark, fontSize = 22.sp) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), Modifier.weight(1f), color = MyBaseColors.Ink, textAlign = TextAlign.Center, fontSize = 13.sp)
            TextButton(onClick = { month = month.plusMonths(1) }, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 1.dp)) { Text("›", color = MyBaseColors.LavenderDark, fontSize = 22.sp) }
        }
        Row(Modifier.fillMaxWidth()) {
            (0..6).forEach { offset ->
                Text(firstDay.plus(offset.toLong()).getDisplayName(java.time.format.TextStyle.NARROW, locale), Modifier.weight(1f), color = MyBaseColors.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
        PlannerCalendarMapper.monthCells(month, firstDay).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) Spacer(Modifier.weight(1f).height(36.dp))
                    else {
                        val counts = PlannerCalendarMapper.countsForDay(tasks, date.toEpochDay())
                        val selected = date.toEpochDay() == selectedEpochDay
                        val marker = when {
                            counts.hasPending -> MyBaseColors.ButtonRose
                            counts.completed > 0 -> MyBaseColors.MintDark
                            else -> Color.Transparent
                        }
                        val description = buildString {
                            append(date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", locale)))
                            when {
                                counts.pending > 0 && counts.completed > 0 -> append(", ${counts.pending} pending and ${counts.completed} completed tasks")
                                counts.pending > 0 -> append(", ${counts.pending} pending tasks")
                                counts.completed > 0 -> append(", ${counts.completed} completed tasks")
                                else -> append(", no tasks")
                            }
                        }
                        Column(
                            Modifier.weight(1f).padding(horizontal = 1.dp).height(36.dp).clip(CircleShape)
                                .background(if (selected) MyBaseColors.Lavender else Color.Transparent)
                                .semantics { contentDescription = description }
                                .clickable { onSelectDay(date.toEpochDay()) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(date.dayOfMonth.toString(), color = if (selected) MyBaseColors.LavenderDark else MyBaseColors.Ink, fontSize = 11.sp)
                            Box(Modifier.size(4.dp).clip(CircleShape).background(marker))
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalendarLegend(MyBaseColors.ButtonRose, "Pending")
            CalendarLegend(MyBaseColors.MintDark, "Completed only")
        }
    }
}

@Composable
private fun CalendarLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(label, color = MyBaseColors.Muted, fontSize = 9.sp)
    }
}

@Composable
private fun TaskRow(
    task: CapturedEntry,
    onComplete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MyBaseColors.Cream).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Checkbox(checked = task.completed, onCheckedChange = { onComplete(task.id) })
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(task.title, color = if (task.completed) MyBaseColors.Muted else MyBaseColors.Ink, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(task.priority.label, color = priorityColor(task.priority), fontSize = 11.sp)
                    task.dueDateEpochDay?.let { epoch ->
                        Text("·  ${LocalDate.ofEpochDay(epoch).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))}", color = MyBaseColors.Muted, fontSize = 11.sp)
                    }
                }
            }
            androidx.compose.material3.TextButton(onClick = { onEdit(task.id) }) { Text("Edit", color = MyBaseColors.ButtonRose) }
            androidx.compose.material3.TextButton(onClick = { onDelete(task.id) }) { Text("Delete", color = MyBaseColors.Muted) }
        }
    }
}

@Composable
private fun PlannerSectionTitle(title: String, detail: String) {
    Column(Modifier.padding(top = 5.dp, bottom = 1.dp)) {
        Text(title, fontSize = 14.sp, color = MyBaseColors.Ink)
        Text(detail, fontSize = 11.sp, color = MyBaseColors.Muted)
    }
}

@Composable
private fun EmptyPlannerMessage(title: String, detail: String, onCreate: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✿", fontSize = 27.sp, color = MyBaseColors.Rose)
        Text(title, fontSize = 14.sp, color = MyBaseColors.Ink)
        Text(detail, fontSize = 12.sp, color = MyBaseColors.Muted)
        if (onCreate != null) androidx.compose.material3.TextButton(onClick = onCreate) { Text("Add a task", color = MyBaseColors.ButtonRose) }
    }
}

private fun priorityColor(priority: TaskPriority) = when (priority) {
    TaskPriority.Low -> MyBaseColors.MintDark
    TaskPriority.Normal -> MyBaseColors.LavenderDark
    TaskPriority.High -> MyBaseColors.ButtonRose
}

