package com.example.mybase.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.core.designsystem.MomoIllustration
import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.core.model.LibraryResource
import com.example.mybase.feature.focus.StopwatchUiState
import com.example.mybase.feature.library.readableResourceTitle
import com.example.mybase.ui.theme.MyBaseColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    dashboard: DailyDashboardState,
    stopwatch: StopwatchUiState,
    onCapture: () -> Unit,
    onCreateTask: () -> Unit,
    onCreateNote: () -> Unit,
    onAddResource: () -> Unit,
    onStartFocus: () -> Unit,
    onPauseFocus: () -> Unit,
    onStopwatch: () -> Unit,
    onToggleTask: (CapturedEntry) -> Unit,
    onOpenTask: (CapturedEntry) -> Unit,
    onOpenNote: (CapturedEntry) -> Unit,
    onOpenResource: (LibraryResource) -> Unit,
    onOpenKnowledge: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MyBaseColors.Cream)) {
        val compact = maxWidth < 760.dp
        val today = LocalDate.now()
        // Compact layouts include a Material navigation bar that already accounts for this inset.
        // Wide layouts use a side rail, so reserve the system navigation area in the scroll range.
        val bottomContentPadding = if (compact) 28.dp else
            16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = if (compact) 18.dp else 30.dp, end = if (compact) 18.dp else 30.dp, top = 16.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(if (compact) 13.dp else 15.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Your day, gently", fontSize = if (compact) 25.sp else 30.sp, fontWeight = FontWeight.Bold, color = MyBaseColors.Ink)
                        Text(today.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())), fontSize = 13.sp, color = MyBaseColors.Muted)
                    }
                    Text("✿", fontSize = 27.sp, color = MyBaseColors.Rose)
                }
            }
            if (compact) {
                item { WelcomeStrip(onCapture) }
                item { TodayOverview(dashboard, onCreateTask, onToggleTask, onOpenTask) }
                item { QuickActions(onCreateTask, onCreateNote, onAddResource, onStartFocus, compact = true) }
                item { FocusWidget(stopwatch, onStartFocus, onPauseFocus, onStopwatch) }
                item { ContinueReading(dashboard, onOpenResource, onOpenLibrary) }
                item { RecentKnowledge(dashboard, onCreateNote, onOpenNote, onOpenKnowledge) }
            } else {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            WelcomeStrip(onCapture)
                            TodayOverview(dashboard, onCreateTask, onToggleTask, onOpenTask)
                        }
                        Column(Modifier.weight(.85f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            QuickActions(onCreateTask, onCreateNote, onAddResource, onStartFocus, compact = false)
                            FocusWidget(stopwatch, onStartFocus, onPauseFocus, onStopwatch)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ContinueReading(dashboard, onOpenResource, onOpenLibrary, Modifier.weight(1f))
                        RecentKnowledge(dashboard, onCreateNote, onOpenNote, onOpenKnowledge, Modifier.weight(1f))
                    }
                }
            }
            if (dashboard.overdueTasks.isNotEmpty()) item {
                DashboardPanel(title = "A little past due", subtitle = "Whenever you’re ready", accent = MyBaseColors.Blush) {
                    dashboard.overdueTasks.take(3).forEach { task ->
                        TaskLine(task, onToggle = { onToggleTask(task) }, onClick = { onOpenTask(task) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStrip(onCapture: () -> Unit) {
    Row(Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(MyBaseColors.Blush).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("A fresh little start", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MyBaseColors.RoseText)
            Text("One thing at a time.", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MyBaseColors.Ink)
            Text("Your plans and ideas, gathered in one place.", fontSize = 13.sp, color = MyBaseColors.Ink)
            Button(onClick = onCapture, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = MyBaseColors.ButtonRose)) { Text("＋ Quick capture") }
        }
        MomoIllustration(Modifier.size(76.dp).padding(start = 8.dp))
    }
}

@Composable
private fun TodayOverview(state: DailyDashboardState, onCreateTask: () -> Unit, onToggle: (CapturedEntry) -> Unit, onOpen: (CapturedEntry) -> Unit) {
    val pending = state.todayTasks.size - state.completedDueToday
    DashboardPanel("Today’s tasks", "$pending pending · ${state.completedDueToday} complete", MyBaseColors.Mint) {
        LinearProgressIndicator(progress = { state.todayProgress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = MyBaseColors.MintDark, trackColor = MyBaseColors.Blush)
        when {
            state.todayTasks.isEmpty() -> {
                Text("Nothing due today. Add a task if something comes to mind.", fontSize = 13.sp, color = MyBaseColors.Muted)
                TextButton(onClick = onCreateTask) { Text("＋ Create a task", color = MyBaseColors.MintDark) }
            }
            else -> state.todayTasks.take(4).forEach { TaskLine(it, { onToggle(it) }, { onOpen(it) }) }
        }
    }
}

@Composable
private fun QuickActions(onTask: () -> Unit, onNote: () -> Unit, onResource: () -> Unit, onFocus: () -> Unit, compact: Boolean) {
    DashboardPanel("Quick actions", "A small step is still a step", MyBaseColors.Lavender) {
        val actions = listOf(Triple("New task", "✓", onTask), Triple("New note", "✎", onNote), Triple("Add resource", "▤", onResource), Triple("Start focus", "◷", onFocus))
        actions.chunked(if (compact) 2 else 2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, glyph, action) ->
                    Row(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).background(MyBaseColors.Cream).clickable(onClick = action).padding(horizontal = 10.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(glyph, color = MyBaseColors.LavenderDark, fontSize = 17.sp)
                        Text(label, Modifier.padding(start = 8.dp), color = MyBaseColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusWidget(state: StopwatchUiState, onStart: () -> Unit, onPause: () -> Unit, onOpen: () -> Unit) {
    val focus = mapDashboardFocus(state)
    val status = if (focus.isRunning) "Running" else if (state.elapsedMillis > 0L) "Paused" else "Ready"
    val statusTint = if (focus.isRunning) MyBaseColors.Mint else if (state.elapsedMillis > 0L) MyBaseColors.Blush else MyBaseColors.White
    DashboardPanel("Focus", "A little quiet time, when you’re ready", MyBaseColors.Lavender, surface = MyBaseColors.Lavender) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(focus.elapsedText, Modifier.weight(1f), fontSize = 31.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, color = MyBaseColors.Ink)
            Text("●  $status", Modifier.clip(RoundedCornerShape(20.dp)).background(statusTint).padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MyBaseColors.Ink)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = if (focus.primaryAction == DashboardFocusAction.Pause) onPause else onStart, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = MyBaseColors.ButtonRose, contentColor = MyBaseColors.White), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp)) {
                Text(when (focus.primaryAction) { DashboardFocusAction.Start -> "Start"; DashboardFocusAction.Resume -> "Resume"; DashboardFocusAction.Pause -> "Pause" })
            }
            TextButton(onClick = onOpen) { Text("Open Focus", color = MyBaseColors.Muted) }
        }
    }
}

@Composable
private fun ContinueReading(state: DailyDashboardState, onOpen: (LibraryResource) -> Unit, onLibrary: () -> Unit, modifier: Modifier = Modifier) {
    DashboardPanel("Continue reading", "Saved PDF reading positions", MyBaseColors.Blush, modifier) {
        if (state.continueReading.isEmpty()) {
            Text("Open a PDF from Library to save your place and continue here.", fontSize = 13.sp, color = MyBaseColors.Muted)
            TextButton(onClick = onLibrary) { Text("Open Library", color = MyBaseColors.RoseText) }
        } else state.continueReading.forEach { item ->
            Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).clickable { onOpen(item.resource) }.padding(vertical = 6.dp)) {
                Text(readableResourceTitle(item.resource), fontWeight = FontWeight.SemiBold, color = MyBaseColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                Text("PDF · page ${item.pageNumber}", fontSize = 12.sp, color = MyBaseColors.Muted)
            }
        }
    }
}

@Composable
private fun RecentKnowledge(state: DailyDashboardState, onCreate: () -> Unit, onOpen: (CapturedEntry) -> Unit, onKnowledge: () -> Unit, modifier: Modifier = Modifier) {
    DashboardPanel("Recent knowledge", "Your newest notes", MyBaseColors.Mint, modifier) {
        if (state.recentNotes.isEmpty()) {
            Text("Create a note in Knowledge and it will be easy to find here.", fontSize = 13.sp, color = MyBaseColors.Muted)
            TextButton(onClick = onCreate) { Text("＋ Create a note", color = MyBaseColors.MintDark) }
        } else state.recentNotes.forEach { note ->
            Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).clickable { onOpen(note) }.padding(vertical = 6.dp)) {
                Text(note.title.ifBlank { "Untitled note" }, fontWeight = FontWeight.SemiBold, color = MyBaseColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note.body.isNotBlank()) Text(note.body.replace('\n', ' '), fontSize = 12.sp, color = MyBaseColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (note == state.recentNotes.last()) TextButton(onClick = onKnowledge) { Text("See all notes", color = MyBaseColors.MintDark) }
        }
    }
}

@Composable
private fun TaskLine(task: CapturedEntry, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(if (task.completed) MyBaseColors.Mint else MyBaseColors.Blush).clickable(onClick = onToggle), contentAlignment = Alignment.Center) {
            Text(if (task.completed) "✓" else "○", color = if (task.completed) MyBaseColors.MintDark else MyBaseColors.RoseText, fontSize = 15.sp)
        }
        Column(Modifier.weight(1f).clickable(onClick = onClick).padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
            Text(task.title, color = if (task.completed) MyBaseColors.Muted else MyBaseColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (task.priority.label != "Normal") Text("${task.priority.label} priority", color = MyBaseColors.Muted, fontSize = 10.sp)
        }
        Text("›", color = MyBaseColors.Muted, fontSize = 20.sp)
    }
}

@Composable
private fun DashboardPanel(title: String, subtitle: String, accent: Color, modifier: Modifier = Modifier, surface: Color = MyBaseColors.White, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(surface).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(accent))
            Text(title, Modifier.padding(start = 8.dp).weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MyBaseColors.Ink)
        }
        Text(subtitle, fontSize = 11.sp, color = MyBaseColors.Muted)
        content()
    }
}
