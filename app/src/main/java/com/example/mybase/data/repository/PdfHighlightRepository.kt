package com.example.mybase.data.repository

import com.example.mybase.core.model.PdfHighlight
import com.example.mybase.data.local.PdfHighlightLocalDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PdfHighlightRepository {
    val highlights: kotlinx.coroutines.flow.StateFlow<List<PdfHighlight>>
    fun forDocument(documentId: String): List<PdfHighlight>
    fun add(highlight: PdfHighlight)
    fun remove(id: String)
    fun removeDocument(documentId: String)
}

class LocalPdfHighlightRepository(private val source: PdfHighlightLocalDataSource) : PdfHighlightRepository {
    private val mutable = MutableStateFlow(source.read().filter(PdfHighlight::isValid).distinctBy { it.id })
    override val highlights = mutable.asStateFlow()
    override fun forDocument(documentId: String) = mutable.value.filter { it.documentId == documentId }
    override fun add(highlight: PdfHighlight) {
        if (!highlight.isValid() || mutable.value.any { it.id == highlight.id }) return
        save(listOf(highlight) + mutable.value)
    }
    override fun remove(id: String) { save(mutable.value.filterNot { it.id == id }) }
    override fun removeDocument(documentId: String) { save(mutable.value.filterNot { it.documentId == documentId }) }
    private fun save(values: List<PdfHighlight>) { source.write(values); mutable.value = values }
}
