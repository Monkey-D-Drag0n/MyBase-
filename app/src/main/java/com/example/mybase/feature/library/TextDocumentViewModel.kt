package com.example.mybase.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybase.core.model.LibraryResource
import com.example.mybase.data.document.DocumentTextSource
import com.example.mybase.data.document.DocumentText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TextDocumentUiState(
    val documentId: String = "",
    val uri: String = "",
    val title: String = "",
    val kind: DocumentPreviewKind = DocumentPreviewKind.PlainText,
    val text: String = "",
    val markdownBlocks: List<MarkdownBlock> = emptyList(),
    val loading: Boolean = false,
    val truncated: Boolean = false,
    val error: String? = null,
    val showRaw: Boolean = false
)

class TextDocumentViewModel(private val source: DocumentTextSource) : ViewModel() {
    private val mutableState = MutableStateFlow(TextDocumentUiState())
    val uiState = mutableState.asStateFlow()
    private var requestId = 0

    fun open(resource: LibraryResource) {
        val kind = previewKind(resource)
        if (kind != DocumentPreviewKind.PlainText && kind != DocumentPreviewKind.Markdown) return
        val token = ++requestId
        mutableState.value = TextDocumentUiState(resource.id, resource.reference, resource.title, kind, loading = true)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val document = source.read(resource.reference)
                    document to if (kind == DocumentPreviewKind.Markdown) MarkdownParser.parse(document.text) else emptyList()
                }
            }
            if (token != requestId) return@launch
            result.fold(
                onSuccess = { (document, blocks) -> mutableState.update { it.copy(text = document.text, markdownBlocks = blocks, truncated = document.truncated, loading = false) } },
                onFailure = { error -> mutableState.update { it.copy(loading = false, error = error.message ?: "This document could not be read.") } }
            )
        }
    }

    fun showRaw(show: Boolean) = mutableState.update { it.copy(showRaw = show) }
    fun close() { requestId++; mutableState.value = TextDocumentUiState() }
}

class TextDocumentViewModelFactory(private val source: DocumentTextSource) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = TextDocumentViewModel(source) as T
}
