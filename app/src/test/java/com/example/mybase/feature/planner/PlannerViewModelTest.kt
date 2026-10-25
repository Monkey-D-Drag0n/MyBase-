package com.example.mybase.feature.planner

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.core.model.TaskPriority
import com.example.mybase.data.local.SharedPreferencesEntryLocalDataSource
import com.example.mybase.data.local.StringPreferenceStore
import com.example.mybase.data.repository.LocalEntryRepository
import com.example.mybase.feature.home.HomeViewModel
import com.example.mybase.feature.home.HomeViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModelTest {
    @Test
    fun createsTaskWithSelectedDueDayAndPriorityAndItSurvivesRepositoryRecreation() = runTest {
        withMainDispatcher { store ->
            val repository = repository()
            val viewModel = plannerViewModel(store, repository)
            val dueDay = LocalDate.of(2031, 4, 12).toEpochDay()
            viewModel.selectDay(dueDay)
            viewModel.showCreateTask()
            viewModel.updateDraftTitle("Prepare slides")
            viewModel.updateDraftPriority(TaskPriority.High)
            viewModel.saveTask()
            runCurrent()

            val saved = repository.entries.value.single()
            assertEquals("Prepare slides", saved.title)
            assertEquals(dueDay, saved.dueDateEpochDay)
            assertEquals(TaskPriority.High, saved.priority)
            assertEquals("Task", saved.kind)
            assertFalse(viewModel.uiState.value.isEditorVisible)

            val reopened = LocalEntryRepository(SharedPreferencesEntryLocalDataSource(repository.store))
            assertEquals(saved, reopened.entries.value.single())
        }
    }

    @Test
    fun editingChangesTaskFieldsAndKeepsCompletionIdentityAndCreationTime() = runTest {
        withMainDispatcher { store ->
            val task = CapturedEntry("Old title", "Task", 50L, completed = true, dueDateEpochDay = 22L, priority = TaskPriority.Low, id = "task-1")
            val repository = repository(listOf(task))
            val viewModel = plannerViewModel(store, repository)
            viewModel.editTask(task.id)
            viewModel.updateDraftTitle("New title")
            viewModel.updateDraftPriority(TaskPriority.High)
            viewModel.setDraftDueDate(44L)
            viewModel.saveTask()
            runCurrent()

            assertEquals(task.copy(title = "New title", dueDateEpochDay = 44L, priority = TaskPriority.High), repository.entries.value.single())
            assertEquals(repository.entries.value, LocalEntryRepository(SharedPreferencesEntryLocalDataSource(repository.store)).entries.value)
        }
    }

    @Test
    fun plannerCompletionUpdatesTheHomeDashboardState() = runTest {
        withMainDispatcher { store ->
            val task = CapturedEntry("Send invoice", "Task", 60L, id = "task-2")
            val repository = repository(listOf(task))
            val homeViewModel = homeViewModel(store, repository)
            val plannerViewModel = plannerViewModel(store, repository)

            plannerViewModel.toggleCompletion(task.id)
            runCurrent()

            assertTrue(repository.entries.value.single().completed)
            assertTrue(homeViewModel.uiState.value.entries.single().completed)
            assertTrue(
                LocalEntryRepository(SharedPreferencesEntryLocalDataSource(repository.store))
                    .entries.value.single().completed
            )
        }
    }

    @Test
    fun deletingTaskRemovesItFromPlannerAndRepository() = runTest {
        withMainDispatcher { store ->
            val target = CapturedEntry("Remove me", "Task", 70L, id = "delete-me")
            val other = CapturedEntry("Keep me", "Task", 71L, id = "keep-me")
            val repository = repository(listOf(target, other))
            val viewModel = plannerViewModel(store, repository)

            viewModel.deleteTask(target.id)
            runCurrent()

            assertEquals(listOf(other), repository.entries.value)
            assertEquals(listOf(other), viewModel.uiState.value.tasks)
            assertEquals(listOf(other), LocalEntryRepository(SharedPreferencesEntryLocalDataSource(repository.store)).entries.value)
        }
    }

    @Test
    fun dayAndUpcomingFiltersIncludeDueAndUnscheduledTasksAppropriately() = runTest {
        withMainDispatcher { store ->
            val today = LocalDate.now().toEpochDay()
            val todayTask = CapturedEntry("Today", "Task", 10L, dueDateEpochDay = today, id = "today")
            val tomorrowTask = CapturedEntry("Tomorrow", "Task", 11L, dueDateEpochDay = today + 1, id = "tomorrow")
            val overdueTask = CapturedEntry("Past due", "Task", 12L, dueDateEpochDay = today - 1, id = "overdue")
            val unscheduledTask = CapturedEntry("Unscheduled", "Task", 13L, id = "unscheduled")
            val completedTask = CapturedEntry("Done tomorrow", "Task", 14L, completed = true, dueDateEpochDay = today + 1, id = "done")
            val viewModel = plannerViewModel(store, repository(listOf(todayTask, tomorrowTask, overdueTask, unscheduledTask, completedTask)))
            runCurrent()

            assertEquals(listOf(todayTask), viewModel.uiState.value.selectedDayTasks)
            assertEquals(listOf(unscheduledTask), viewModel.uiState.value.unscheduledTasks)
            assertEquals(listOf(overdueTask), viewModel.uiState.value.overdueTasks)
            assertEquals(listOf(todayTask, tomorrowTask), viewModel.uiState.value.upcomingTasks)

            viewModel.selectDay(today + 1)
            assertEquals(listOf(tomorrowTask, completedTask), viewModel.uiState.value.selectedDayTasks)
            viewModel.showUpcomingTab()
            assertEquals(PlannerTab.Upcoming, viewModel.uiState.value.tab)
        }
    }

    @Test
    fun saveIgnoresBlankTitlesAndDeleteOfUnknownIdDoesNothing() = runTest {
        withMainDispatcher { store ->
            val repository = repository()
            val viewModel = plannerViewModel(store, repository)
            viewModel.showCreateTask()
            viewModel.updateDraftTitle("   ")
            viewModel.saveTask()
            viewModel.deleteTask("missing")

            assertTrue(repository.entries.value.isEmpty())
            assertTrue(viewModel.uiState.value.isEditorVisible)
        }
    }

    private fun repository(initial: List<CapturedEntry> = emptyList()): TestRepository {
        val store = MemoryStringPreferenceStore()
        val repository = LocalEntryRepository(SharedPreferencesEntryLocalDataSource(store))
        if (initial.isNotEmpty()) repository.saveEntries(initial)
        return TestRepository(repository, store)
    }

    private suspend fun withMainDispatcher(block: suspend (ViewModelStore) -> Unit) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val store = ViewModelStore()
        try {
            block(store)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private fun plannerViewModel(store: ViewModelStore, repository: TestRepository, key: String = "planner"): PlannerViewModel =
        ViewModelProvider(store, PlannerViewModelFactory(repository)).get(key, PlannerViewModel::class.java)

    private fun homeViewModel(store: ViewModelStore, repository: TestRepository): HomeViewModel =
        ViewModelProvider(store, HomeViewModelFactory(repository)).get("home", HomeViewModel::class.java)

    private data class TestRepository(
        val delegate: LocalEntryRepository,
        val store: MemoryStringPreferenceStore
    ) : com.example.mybase.data.repository.EntryRepository by delegate

    private class MemoryStringPreferenceStore : StringPreferenceStore {
        private val values = mutableMapOf<String, String>()
        override fun getString(key: String, defaultValue: String): String = values[key] ?: defaultValue
        override fun putString(key: String, value: String) { values[key] = value }
    }
}
