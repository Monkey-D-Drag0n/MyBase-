package com.example.mybase.data.repository

import com.example.mybase.core.model.PdfTextHighlight
import com.example.mybase.core.model.PdfTextHighlightBounds
import com.example.mybase.data.local.PdfTextHighlightLocalDataSource
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalPdfTextHighlightRepositoryTest {
    @Test fun highlightsSurviveReloadAndOnlySelectedMarkOrDocumentIsRemoved() {
        val store = MemoryStore()
        val first = PdfTextHighlight("a", "stable-id", 2, "selected text", listOf(PdfTextHighlightBounds(2f, 3f, 22f, 15f)))
        val second = PdfTextHighlight("b", "other", 4, "other text", listOf(PdfTextHighlightBounds(1f, 1f, 10f, 10f)))
        val repository = LocalPdfTextHighlightRepository(store)
        repository.add(first); repository.add(first); repository.add(second)
        val restored = LocalPdfTextHighlightRepository(store)
        assertEquals(listOf(first), restored.forDocument("stable-id"))
        restored.remove("a")
        assertEquals(listOf(second), restored.highlights.value)
        restored.removeDocument("other")
        assertEquals(emptyList<PdfTextHighlight>(), restored.highlights.value)
    }

    private class MemoryStore : PdfTextHighlightLocalDataSource {
        private var values = emptyList<PdfTextHighlight>()
        override fun read() = values
        override fun write(highlights: List<PdfTextHighlight>) { values = highlights }
    }
}
