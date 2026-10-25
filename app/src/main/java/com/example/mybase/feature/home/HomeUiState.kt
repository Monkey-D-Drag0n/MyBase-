package com.example.mybase.feature.home

import com.example.mybase.core.model.CapturedEntry

data class HomeUiState(
    val entries: List<CapturedEntry> = emptyList(),
    val isCaptureVisible: Boolean = false,
    val captureTitle: String = "",
    val captureKind: String = "Note",
    val isSearchVisible: Boolean = false,
    val searchQuery: String = ""
) {
    val searchResults: List<CapturedEntry>
        get() = if (searchQuery.isBlank()) emptyList() else entries
            .filter { it.title.contains(searchQuery, ignoreCase = true) }
            .take(4)
}
