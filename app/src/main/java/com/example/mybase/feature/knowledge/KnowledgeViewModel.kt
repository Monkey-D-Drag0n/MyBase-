package com.example.mybase.feature.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.data.repository.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class KnowledgeUiState(
    val notes: List<CapturedEntry> = emptyList(), val query: String = "",
    val selectedCategory: String? = null, val selectedTag: String? = null,
    val isEditorVisible: Boolean = false, val editingId: String? = null,
    val title: String = "", val body: String = "", val categoriesText: String = "", val tagsText: String = "",
    val originalNote: CapturedEntry? = null,
    val showDiscardConfirmation: Boolean = false,
    val pendingDeleteId: String? = null
) {
    val categories get() = notes.flatMap { it.categories }.distinct().sorted()
    val tags get() = notes.flatMap { it.tags }.distinct().sorted()
    val filteredNotes get() = notes.filter { note ->
        (query.isBlank() || note.title.contains(query, true) || note.body.contains(query, true)) &&
            (selectedCategory == null || selectedCategory in note.categories) && (selectedTag == null || selectedTag in note.tags)
    }
    val hasUnsavedChanges: Boolean
        get() = if (originalNote == null) title.isNotBlank() || body.isNotBlank() || categoriesText.isNotBlank() || tagsText.isNotBlank()
        else title != originalNote.title || body != originalNote.body || categoriesText != originalNote.categories.joinToString(", ") || tagsText != originalNote.tags.joinToString(", ")
    val canSave: Boolean get() = title.isNotBlank() || body.isNotBlank()
}

class KnowledgeViewModel(private val repository: EntryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(KnowledgeUiState(notes = repository.entries.value.filter { it.kind == "Note" }))
    val uiState = _uiState.asStateFlow()
    init { viewModelScope.launch { repository.entries.collect { entries -> _uiState.update { it.copy(notes = entries.filter { e -> e.kind == "Note" }) } } } }
    fun setSearchQuery(query: String) = _uiState.update { it.copy(query = query) }
    fun selectCategory(value: String?) = _uiState.update { it.copy(selectedCategory = if (it.selectedCategory == value) null else value) }
    fun selectTag(value: String?) = _uiState.update { it.copy(selectedTag = if (it.selectedTag == value) null else value) }
    fun createNote() = _uiState.update { it.copy(isEditorVisible = true, editingId = null, title = "", body = "", categoriesText = "", tagsText = "", originalNote = null, showDiscardConfirmation = false) }
    fun editNote(note: CapturedEntry) {
        if (note.kind != "Note") return
        _uiState.update { it.copy(isEditorVisible = true, editingId = note.id, title = note.title, body = note.body, categoriesText = note.categories.joinToString(", "), tagsText = note.tags.joinToString(", "), originalNote = note, showDiscardConfirmation = false) }
    }
    fun updateTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun updateBody(value: String) = _uiState.update { it.copy(body = value) }
    fun updateCategories(value: String) = _uiState.update { it.copy(categoriesText = value) }
    fun updateTags(value: String) = _uiState.update { it.copy(tagsText = value) }
    fun dismissEditor() = _uiState.update { state ->
        if (state.hasUnsavedChanges) state.copy(showDiscardConfirmation = true) else closeEditor(state)
    }
    fun keepEditing() = _uiState.update { it.copy(showDiscardConfirmation = false) }
    fun discardChanges() = _uiState.update { closeEditor(it) }
    fun saveNote() {
        val s = _uiState.value
        if (!s.canSave) return
        val categories = s.categoriesText.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
        val tags = s.tagsText.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
        if (s.editingId != null) repository.updateEntry(s.editingId) { it.copy(title=s.title.trim(), body=s.body, categories=categories, tags=tags) }
        else repository.addEntry(CapturedEntry(s.title.trim(), "Note", System.currentTimeMillis(), body=s.body, categories=categories, tags=tags, id=UUID.randomUUID().toString()))
        _uiState.update { closeEditor(it) }
    }
    fun requestDelete(note: CapturedEntry) {
        if (note.kind == "Note") _uiState.update { it.copy(pendingDeleteId = note.id) }
    }
    fun cancelDelete() = _uiState.update { it.copy(pendingDeleteId = null) }
    fun confirmDelete() {
        val id = _uiState.value.pendingDeleteId ?: return
        if (_uiState.value.notes.any { it.id == id }) repository.deleteEntry(id)
        _uiState.update { it.copy(pendingDeleteId = null) }
    }

    private fun closeEditor(state: KnowledgeUiState) = state.copy(isEditorVisible = false, editingId = null, originalNote = null, showDiscardConfirmation = false, title = "", body = "", categoriesText = "", tagsText = "")
}

class KnowledgeViewModelFactory(private val repository: EntryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = KnowledgeViewModel(repository) as T
}
