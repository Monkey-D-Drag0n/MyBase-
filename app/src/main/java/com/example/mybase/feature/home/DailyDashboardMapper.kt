package com.example.mybase.feature.home

import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.core.model.DocumentReadingState
import com.example.mybase.core.model.LibraryResource
import com.example.mybase.core.model.ResourceType
import com.example.mybase.feature.focus.StopwatchUiState
import java.time.LocalDate

/** Data available to the Home dashboard, derived only from persisted application sources. */
data class DailyDashboardState(
    val todayTasks: List<CapturedEntry> = emptyList(),
    val overdueTasks: List<CapturedEntry> = emptyList(),
    val completedDueToday: Int = 0,
    val recentNotes: List<CapturedEntry> = emptyList(),
    val continueReading: List<ReadingResumeItem> = emptyList()
) {
    val todayProgress: Float
        get() = if (todayTasks.isEmpty()) 0f else completedDueToday.toFloat() / todayTasks.size

    val isEmpty: Boolean
        get() = todayTasks.isEmpty() && overdueTasks.isEmpty() && recentNotes.isEmpty() && continueReading.isEmpty()
}

data class ReadingResumeItem(val resource: LibraryResource, val pageNumber: Int)

enum class DashboardFocusAction { Start, Resume, Pause }
data class DashboardFocusState(val elapsedText: String, val isRunning: Boolean, val primaryAction: DashboardFocusAction)

fun mapDashboardFocus(state: StopwatchUiState): DashboardFocusState {
    val seconds = state.elapsedMillis.coerceAtLeast(0L) / 1_000L
    val time = if (seconds >= 3_600L) "%02d:%02d:%02d".format(seconds / 3_600L, (seconds / 60L) % 60L, seconds % 60L)
    else "%02d:%02d".format(seconds / 60L, seconds % 60L)
    val action = when {
        state.isRunning -> DashboardFocusAction.Pause
        state.elapsedMillis > 0L -> DashboardFocusAction.Resume
        else -> DashboardFocusAction.Start
    }
    return DashboardFocusState(time, state.isRunning, action)
}

object DailyDashboardMapper {
    fun map(
        entries: List<CapturedEntry>,
        resources: List<LibraryResource>,
        readingStates: List<DocumentReadingState>,
        today: LocalDate,
        maxRecentNotes: Int = 3,
        maxReadingItems: Int = 3
    ): DailyDashboardState {
        val todayEpoch = today.toEpochDay()
        val tasks = entries.filter { it.kind == TASK_KIND }
        val todayTasks = tasks.filter { it.dueDateEpochDay == todayEpoch }
            .sortedWith(compareBy<CapturedEntry> { it.completed }.thenByDescending { it.priority.ordinal }.thenBy { it.createdAtMillis })
        val overdue = tasks.filter { it.dueDateEpochDay != null && it.dueDateEpochDay < todayEpoch && !it.completed }
            .sortedWith(compareBy<CapturedEntry> { it.dueDateEpochDay }.thenByDescending { it.priority.ordinal })
        val notes = entries.asSequence().filter { it.kind == NOTE_KIND }
            .sortedByDescending { it.createdAtMillis }.take(maxRecentNotes.coerceAtLeast(0)).toList()
        val resourcesById = resources.filter { it.type != ResourceType.Link }.associateBy { it.id }
        // Reading state has no opened-at timestamp; preserve the repository's resource order.
        val reading = readingStates.asSequence()
            .mapNotNull { saved -> resourcesById[saved.documentId]?.let { ReadingResumeItem(it, saved.lastPage + 1) } }
            .take(maxReadingItems.coerceAtLeast(0)).toList()
        return DailyDashboardState(
            todayTasks = todayTasks,
            overdueTasks = overdue,
            completedDueToday = todayTasks.count { it.completed },
            recentNotes = notes,
            continueReading = reading
        )
    }

    private const val TASK_KIND = "Task"
    private const val NOTE_KIND = "Note"
}
