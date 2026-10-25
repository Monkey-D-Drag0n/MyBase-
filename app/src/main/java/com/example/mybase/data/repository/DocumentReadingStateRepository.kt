package com.example.mybase.data.repository

import com.example.mybase.core.model.DocumentReadingState
import com.example.mybase.data.local.DocumentReadingStateLocalDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface DocumentReadingStateRepository {
    val states: StateFlow<List<DocumentReadingState>>
    fun stateFor(documentId: String): DocumentReadingState
    fun savePage(documentId: String, page: Int)
    fun addBookmark(documentId: String, page: Int)
    fun removeBookmark(documentId: String, page: Int)
    fun delete(documentId: String)
}

class LocalDocumentReadingStateRepository(private val dataSource: DocumentReadingStateLocalDataSource) : DocumentReadingStateRepository {
    private val mutableStates = MutableStateFlow(dataSource.read())
    override val states = mutableStates.asStateFlow()
    override fun stateFor(documentId: String) = mutableStates.value.firstOrNull { it.documentId == documentId } ?: DocumentReadingState(documentId)
    override fun savePage(documentId: String, page: Int) = update(documentId) { it.copy(lastPage = page.coerceAtLeast(0)) }
    override fun addBookmark(documentId: String, page: Int) = update(documentId) { it.copy(bookmarks = (it.bookmarks + page.coerceAtLeast(0)).distinct().sorted()) }
    override fun removeBookmark(documentId: String, page: Int) = update(documentId) { it.copy(bookmarks = it.bookmarks.filterNot { saved -> saved == page }) }
    override fun delete(documentId: String) = save(mutableStates.value.filterNot { it.documentId == documentId })
    private fun update(id: String, transform: (DocumentReadingState) -> DocumentReadingState) {
        val current = mutableStates.value
        val old = current.firstOrNull { it.documentId == id } ?: DocumentReadingState(id)
        save(if (current.any { it.documentId == id }) current.map { if (it.documentId == id) transform(it) else it } else listOf(transform(old)) + current)
    }
    private fun save(states: List<DocumentReadingState>) { dataSource.write(states); mutableStates.value = states }
}
