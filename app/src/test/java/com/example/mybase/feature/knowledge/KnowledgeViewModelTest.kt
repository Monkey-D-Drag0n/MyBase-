package com.example.mybase.feature.knowledge

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
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgeViewModelTest {
    @Test fun searchCategoryAndTagFiltersNotes() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val repo = repository(listOf(CapturedEntry("Kotlin", "Note", 1, body="flows", categories=listOf("Study"), tags=listOf("code")), CapturedEntry("Dinner", "Note", 2)))
            val vm = KnowledgeViewModel(repo); val store = ViewModelStore().also { it.put("vm", vm) }
            vm.setSearchQuery("FLOW")
            assertEquals(listOf("Kotlin"), vm.uiState.value.filteredNotes.map { it.title })
            vm.setSearchQuery("kotlin")
            assertEquals(listOf("Kotlin"), vm.uiState.value.filteredNotes.map { it.title })
            vm.setSearchQuery(""); vm.selectCategory("Study"); vm.selectTag("code")
            assertEquals(listOf("Kotlin"), vm.uiState.value.filteredNotes.map { it.title })
            store.clear()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun createsEditsAndDeletesNote() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val repo = repository(); val vm = KnowledgeViewModel(repo); val store = ViewModelStore().also { it.put("vm", vm) }
            vm.createNote(); vm.updateTitle("Idea"); vm.updateBody("Details"); vm.updateCategories("Work, Personal"); vm.updateTags("quick, seed"); vm.saveNote()
            runCurrent()
            val created = repo.entries.value.single()
            assertEquals(listOf("Work", "Personal"), created.categories)
            vm.editNote(created); vm.updateTitle("Better idea"); vm.saveNote(); runCurrent()
            assertEquals("Better idea", repo.entries.value.single().title)
            assertEquals(created.id, repo.entries.value.single().id)
            repo.addEntry(CapturedEntry("Planner task", "Task", 99L))
            vm.requestDelete(repo.entries.value.first { it.kind == "Note" })
            vm.cancelDelete()
            assertEquals(2, repo.entries.value.size)
            vm.requestDelete(repo.entries.value.first { it.kind == "Note" })
            vm.confirmDelete(); runCurrent()
            assertEquals(listOf("Planner task"), repo.entries.value.map { it.title })
            store.clear()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun bodyOnlyNoteCanBeSavedAndSearchMatchesBodyButNotMetadata() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val store = ViewModelStore()
        try {
            val repo = repository()
            val vm = KnowledgeViewModel(repo); store.put("vm", vm)
            vm.createNote(); vm.updateBody("A warm 🐈 thought with | pipes and newlines\nkept safe"); vm.updateTags("hiddenword"); vm.saveNote(); runCurrent()
            assertEquals("", repo.entries.value.single().title)
            vm.setSearchQuery("WARM 🐈")
            assertEquals(1, vm.uiState.value.filteredNotes.size)
            vm.setSearchQuery("hiddenword")
            assertTrue(vm.uiState.value.filteredNotes.isEmpty())
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun homeQueryCanSeedKnowledgeSearch() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val store = ViewModelStore()
        try { val vm = KnowledgeViewModel(repository()); store.put("vm", vm); vm.setSearchQuery("seed query"); assertEquals("seed query", vm.uiState.value.query) }
        finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun dirtyEditorRequiresDiscardConfirmation() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val store = ViewModelStore()
        try {
            val vm = KnowledgeViewModel(repository()); store.put("vm", vm)
            vm.createNote(); vm.updateBody("draft text"); vm.dismissEditor()
            assertTrue(vm.uiState.value.showDiscardConfirmation)
            assertTrue(vm.uiState.value.isEditorVisible)
            vm.keepEditing(); assertFalse(vm.uiState.value.showDiscardConfirmation)
            vm.dismissEditor(); vm.discardChanges()
            assertFalse(vm.uiState.value.isEditorVisible)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    private fun repository(entries: List<CapturedEntry> = emptyList()) = LocalEntryRepository(SharedPreferencesEntryLocalDataSource(object : StringPreferenceStore {
        private var value = ""
        override fun getString(key: String, defaultValue: String) = value.ifEmpty { defaultValue }
        override fun putString(key: String, value: String) { this.value = value }
    })).also { if (entries.isNotEmpty()) it.saveEntries(entries) }
}
