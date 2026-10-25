package com.example.mybase.data.repository

import com.example.mybase.core.model.PdfTextHighlight
import com.example.mybase.data.local.PdfTextHighlightLocalDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PdfTextHighlightRepository {
    val highlights: kotlinx.coroutines.flow.StateFlow<List<PdfTextHighlight>>
    fun forDocument(documentId: String): List<PdfTextHighlight>
    fun add(highlight: PdfTextHighlight)
    fun remove(id: String)
    fun removeDocument(documentId: String)
}

class LocalPdfTextHighlightRepository(private val source: PdfTextHighlightLocalDataSource) : PdfTextHighlightRepository {
    private val mutable = MutableStateFlow(source.read().filter(PdfTextHighlight::isValid).distinctBy { it.id })
    override val highlights = mutable.asStateFlow()
    override fun forDocument(documentId: String) = mutable.value.filter { it.documentId == documentId }
    override fun add(highlight: PdfTextHighlight) {
        if (!highlight.isValid() || mutable.value.any { it.id == highlight.id }) return
        save(listOf(highlight) + mutable.value)
    }
    override fun remove(id: String) = save(mutable.value.filterNot { it.id == id })
    override fun removeDocument(documentId: String) = save(mutable.value.filterNot { it.documentId == documentId })
    private fun save(values: List<PdfTextHighlight>) { source.write(values); mutable.value = values }
}
