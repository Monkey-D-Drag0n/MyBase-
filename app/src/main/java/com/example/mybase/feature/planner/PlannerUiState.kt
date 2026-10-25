package com.example.mybase.feature.planner

import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.core.model.TaskPriority
import java.time.LocalDate

enum class PlannerTab { Day, Upcoming }

data class PlannerUiState(
    val tasks: List<CapturedEntry> = emptyList(),
    val selectedDayTasks: List<CapturedEntry> = emptyList(),
    val unscheduledTasks: List<CapturedEntry> = emptyList(),
    val overdueTasks: List<CapturedEntry> = emptyList(),
    val upcomingTasks: List<CapturedEntry> = emptyList(),
    val selectedDayEpochDay: Long = LocalDate.now().toEpochDay(),
    val tab: PlannerTab = PlannerTab.Day,
    val isEditorVisible: Boolean = false,
    val isDatePickerVisible: Boolean = false,
    val editingTaskId: String? = null,
    val draftTitle: String = "",
    val draftDueDateEpochDay: Long? = selectedDayEpochDay,
    val draftPriority: TaskPriority = TaskPriority.Normal
)
