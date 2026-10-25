package com.example.mybase.feature.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.data.local.SharedPreferencesEntryLocalDataSource
import com.example.mybase.data.local.StringPreferenceStore
import com.example.mybase.data.repository.LocalEntryRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Test
    fun captureAddsEntryAndClosesCaptureState() = runTest {
        withMainDispatcher { store ->
            val repository = repository()
            val viewModel = homeViewModel(store, repository)

            viewModel.showCapture()
            viewModel.updateCaptureTitle("Plan the week")
            viewModel.updateCaptureKind("Task")
            viewModel.capture()
            runCurrent()

            assertFalse(viewModel.uiState.value.isCaptureVisible)
            assertEquals("", viewModel.uiState.value.captureTitle)
            assertEquals("Plan the week", viewModel.uiState.value.entries.single().title)
            assertEquals("Task", repository.entries.value.single().kind)
        }
    }

    @Test
    fun togglingTaskPersistsCompletionAndNotesCannotBeToggled() = runTest {
        withMainDispatcher { store ->
            val note = CapturedEntry("Idea", "Note", 10L)
            val task = CapturedEntry("Email Sam", "Task", 20L)
            val repository = repository(listOf(note, task))
            val viewModel = homeViewModel(store, repository)

            viewModel.toggleTask(note)
            viewModel.toggleTask(task)
            runCurrent()

            assertEquals(listOf(note, task.copy(completed = true)), repository.entries.value)
            val restoredViewModel = homeViewModel(store, repository, key = "restored-home")
            assertEquals(repository.entries.value, restoredViewModel.uiState.value.entries)
        }
    }

    @Test
    fun searchStateFiltersSavedEntriesAndCanBeDismissed() = runTest {
        withMainDispatcher { store ->
            val repository = repository(listOf(
                CapturedEntry("Study Kotlin", "Note", 1L),
                CapturedEntry("Call home", "Task", 2L)
            ))
            val viewModel = homeViewModel(store, repository)

            viewModel.showSearch()
            viewModel.updateSearchQuery("kOtLiN")

            assertTrue(viewModel.uiState.value.isSearchVisible)
            assertEquals(listOf("Study Kotlin"), viewModel.uiState.value.searchResults.map { it.title })
            viewModel.hideSearch()
            assertFalse(viewModel.uiState.value.isSearchVisible)
        }
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

    private fun homeViewModel(store: ViewModelStore, repository: LocalEntryRepository, key: String = "home"): HomeViewModel =
        ViewModelProvider(store, HomeViewModelFactory(repository)).get(key, HomeViewModel::class.java)

    private fun repository(initial: List<CapturedEntry> = emptyList()): LocalEntryRepository {
        val store = InMemoryStringPreferenceStore()
        return LocalEntryRepository(SharedPreferencesEntryLocalDataSource(store)).also {
            if (initial.isNotEmpty()) it.saveEntries(initial)
        }
    }

    private class InMemoryStringPreferenceStore : StringPreferenceStore {
        private val values = mutableMapOf<String, String>()
        override fun getString(key: String, defaultValue: String): String = values[key] ?: defaultValue
        override fun putString(key: String, value: String) { values[key] = value }
    }
}
