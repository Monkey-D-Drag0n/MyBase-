package com.example.mybase.feature.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.core.model.TaskPriority
import com.example.mybase.data.repository.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class PlannerViewModel(
    private val entryRepository: EntryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(withFilteredTasks(PlannerUiState(), taskEntries()))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            entryRepository.entries.collect { _uiState.update { state -> withFilteredTasks(state, taskEntries()) } }
        }
    }

    fun showDayTab() = _uiState.update { it.copy(tab = PlannerTab.Day) }
    fun showUpcomingTab() = _uiState.update { it.copy(tab = PlannerTab.Upcoming) }
    fun selectDay(epochDay: Long) = _uiState.update { withFilteredTasks(it.copy(selectedDayEpochDay = epochDay), it.tasks) }
    fun moveSelectedDay(days: Long) = _uiState.update {
        val updated = it.copy(selectedDayEpochDay = it.selectedDayEpochDay + days)
        withFilteredTasks(updated, it.tasks)
    }
    fun selectToday() = selectDay(LocalDate.now().toEpochDay())

    fun showCreateTask() = _uiState.update {
        it.copy(
            isEditorVisible = true,
            isDatePickerVisible = false,
            editingTaskId = null,
            draftTitle = "",
            draftDueDateEpochDay = it.selectedDayEpochDay,
            draftPriority = TaskPriority.Normal
        )
    }

    fun editTask(id: String) {
        val task = entryRepository.entries.value.firstOrNull { it.id == id && it.kind == TASK_KIND } ?: return
        _uiState.update {
            it.copy(
                isEditorVisible = true,
                isDatePickerVisible = false,
                editingTaskId = task.id,
                draftTitle = task.title,
                draftDueDateEpochDay = task.dueDateEpochDay,
                draftPriority = task.priority
            )
        }
    }

    fun updateDraftTitle(title: String) = _uiState.update { it.copy(draftTitle = title) }
    fun updateDraftPriority(priority: TaskPriority) = _uiState.update { it.copy(draftPriority = priority) }
    fun setDraftDueDate(epochDay: Long?) = _uiState.update { it.copy(draftDueDateEpochDay = epochDay, isDatePickerVisible = false) }
    fun showDatePicker() = _uiState.update { it.copy(isDatePickerVisible = true) }
    fun hideDatePicker() = _uiState.update { it.copy(isDatePickerVisible = false) }

    fun hideEditor() = _uiState.update {
        it.copy(isEditorVisible = false, isDatePickerVisible = false, editingTaskId = null, draftTitle = "")
    }

    fun saveTask() {
        val state = _uiState.value
        val title = state.draftTitle.trim()
        if (title.isBlank()) return
        val id = state.editingTaskId
        if (id == null) {
            entryRepository.addEntry(
                CapturedEntry(
                    title = title,
                    kind = TASK_KIND,
                    createdAtMillis = System.currentTimeMillis(),
                    dueDateEpochDay = state.draftDueDateEpochDay,
                    priority = state.draftPriority,
                    id = UUID.randomUUID().toString()
                )
            )
        } else {
            entryRepository.updateEntry(id) { current ->
                current.copy(title = title, dueDateEpochDay = state.draftDueDateEpochDay, priority = state.draftPriority)
            }
        }
        hideEditor()
    }

    fun toggleCompletion(id: String) {
        entryRepository.updateEntry(id) { task ->
            if (task.kind == TASK_KIND) task.copy(completed = !task.completed) else task
        }
    }

    fun deleteTask(id: String) {
        entryRepository.deleteEntry(id)
        if (_uiState.value.editingTaskId == id) hideEditor()
    }

    private fun taskEntries(): List<CapturedEntry> = entryRepository.entries.value.filter { it.kind == TASK_KIND }

    private fun withFilteredTasks(state: PlannerUiState, tasks: List<CapturedEntry>): PlannerUiState {
        val today = LocalDate.now().toEpochDay()
        return state.copy(
            tasks = tasks,
            selectedDayTasks = tasks.filter { it.dueDateEpochDay == state.selectedDayEpochDay }.sortedWith(TASK_ORDER),
            unscheduledTasks = tasks.filter { it.dueDateEpochDay == null }.sortedWith(TASK_ORDER),
            overdueTasks = tasks.filter { it.dueDateEpochDay != null && it.dueDateEpochDay < today && !it.completed }.sortedWith(TASK_ORDER),
            upcomingTasks = tasks.filter { it.dueDateEpochDay != null && it.dueDateEpochDay >= today && !it.completed }.sortedWith(TASK_ORDER)
        )
    }

    private companion object {
        const val TASK_KIND = "Task"
        val TASK_ORDER = compareBy<CapturedEntry> { it.dueDateEpochDay ?: Long.MAX_VALUE }
            .thenByDescending { it.priority.ordinal }
            .thenBy { it.createdAtMillis }
    }
}
