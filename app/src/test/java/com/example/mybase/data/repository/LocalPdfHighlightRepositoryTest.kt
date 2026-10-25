package com.example.mybase.data.repository

import com.example.mybase.core.model.PdfHighlight
import com.example.mybase.data.local.PdfHighlightLocalDataSource
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalPdfHighlightRepositoryTest {
    @Test fun annotationsPersistAndRemoveByIdOrDocument() {
        val source = MemorySource()
        val repo = LocalPdfHighlightRepository(source)
        val a = PdfHighlight("a", "doc-a", 1, .1f, .2f, .4f, .6f)
        val b = PdfHighlight("b", "doc-b", 2, .2f, .3f, .7f, .8f)
        repo.add(a); repo.add(a); repo.add(b)
        val restored = LocalPdfHighlightRepository(source)
        assertEquals(listOf(a), restored.forDocument("doc-a"))
        restored.remove("a")
        assertEquals(listOf(b), restored.highlights.value)
        restored.removeDocument("doc-b")
        assertEquals(emptyList<PdfHighlight>(), restored.highlights.value)
    }

    private class MemorySource : PdfHighlightLocalDataSource {
        private var values = emptyList<PdfHighlight>()
        override fun read() = values
        override fun write(highlights: List<PdfHighlight>) { values = highlights }
    }
}
