package com.example.mybase.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.data.repository.EntryRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(
    private val entryRepository: EntryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(entries = entryRepository.entries.value))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            entryRepository.entries.collect { entries -> _uiState.update { it.copy(entries = entries) } }
        }
    }

    fun showCapture() = _uiState.update { it.copy(isCaptureVisible = true, captureTitle = "", captureKind = "Note") }
    fun hideCapture() = _uiState.update { it.copy(isCaptureVisible = false, captureTitle = "", captureKind = "Note") }
    fun updateCaptureTitle(title: String) = _uiState.update { it.copy(captureTitle = title) }
    fun updateCaptureKind(kind: String) = _uiState.update { it.copy(captureKind = kind) }
    fun showSearch() = _uiState.update { it.copy(isSearchVisible = true, searchQuery = "") }
    fun hideSearch() = _uiState.update { it.copy(isSearchVisible = false) }
    fun updateSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun capture() {
        val title = _uiState.value.captureTitle
        val kind = _uiState.value.captureKind
        if (title.isBlank()) return
        entryRepository.addEntry(
            CapturedEntry(title.trim(), kind, System.currentTimeMillis(), id = UUID.randomUUID().toString())
        )
        hideCapture()
    }

    fun toggleTask(entry: CapturedEntry) {
        if (entry.kind == "Task") {
            entryRepository.updateEntry(entry.id) { current -> current.copy(completed = !current.completed) }
        }
    }
}
