package com.example.mybase.feature.library

import com.example.mybase.data.document.PdfSelectionBounds
import com.example.mybase.data.document.PdfTextSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfTextSelectionTest {
    @Test fun selectionBecomesTextHighlightWithoutChangingTextOrPage() {
        val selection = PdfTextSelection(6, "first line\nsecond line • Δ", listOf(PdfSelectionBounds(.1f, .2f, .8f, .25f)))
        val saved = selection.toHighlight("resource-id", "stable-highlight-id")
        assertEquals("resource-id", saved.documentId)
        assertEquals("stable-highlight-id", saved.id)
        assertEquals(6, saved.page)
        assertEquals(selection.text, saved.text)
        assertEquals(.1f, saved.bounds.single().left)
    }

    @Test fun copyUsesExactSelectedTextAndEmptySelectionCannotCopy() {
        val value = " exact\nselected text — 日本語 🙂 "
        assertEquals(value, textForPdfClipboard(PdfTextSelection(0, value, listOf(PdfSelectionBounds(0f, 0f, 1f, 1f)))))
        assertNull(textForPdfClipboard(null))
        assertNull(textForPdfClipboard(PdfTextSelection(0, "  ", listOf(PdfSelectionBounds(0f, 0f, 1f, 1f)))))
    }

    @Test fun selectionUiStateShowsTextOrExplainsWhyNothingWasSelected() {
        val selected = PdfTextSelection(2, "real PDF text", listOf(PdfSelectionBounds(.1f, .1f, .4f, .2f)))
        val state = PdfReaderUiState(supportsTextSelection = true).withTextSelection(selected)
        assertEquals("real PDF text", state.textSelection?.text)
        assertEquals(null, state.selectionMessage)
        val empty = state.withTextSelection(null)
        assertEquals(null, empty.textSelection)
        assertEquals("No selectable text was found in that selection.", empty.selectionMessage)
    }
}
