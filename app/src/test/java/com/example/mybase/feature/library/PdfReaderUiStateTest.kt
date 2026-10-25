package com.example.mybase.feature.library

import com.example.mybase.core.model.PdfHighlight
import com.example.mybase.core.model.PdfTextHighlight
import com.example.mybase.core.model.PdfTextHighlightBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PdfReaderUiStateTest {
    @Test fun togglingThumbnailPanelPreservesReadingPageAndAnnotations() {
        val region = PdfHighlight("region", "doc", 4, .1f, .2f, .5f, .6f)
        val original = PdfReaderUiState(page = 4, bookmarks = listOf(2, 4), highlights = listOf(region))
        val hidden = original.toggleThumbnails()
        assertFalse(hidden.showThumbnails)
        assertEquals(original.page, hidden.page)
        assertEquals(original.bookmarks, hidden.bookmarks)
        assertEquals(original.highlights, hidden.highlights)
    }

    @Test fun textHighlightsAndRegionAnnotationsRemainSeparateTypes() {
        val text = PdfTextHighlight("text", "doc", 1, "selected words", listOf(PdfTextHighlightBounds(.1f, .2f, .4f, .25f)))
        val region = PdfHighlight("region", "doc", 1, .5f, .5f, .8f, .8f)
        val state = PdfReaderUiState(textHighlights = listOf(text), highlights = listOf(region))
        assertEquals(listOf(text), state.textHighlights)
        assertEquals(listOf(region), state.highlights)
    }
}
