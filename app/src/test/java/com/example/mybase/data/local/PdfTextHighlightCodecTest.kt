package com.example.mybase.data.local

import com.example.mybase.core.model.PdfTextHighlight
import com.example.mybase.core.model.PdfTextHighlightBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfTextHighlightCodecTest {
    @Test fun exactTextAndMultiplePagePointRectsRoundTrip() {
        val highlight = PdfTextHighlight("h:一", "content://provider/📚", 8, "A|B\nΔ🙂", listOf(
            PdfTextHighlightBounds(12f, 22f, 108f, 41f), PdfTextHighlightBounds(10f, 44f, 100f, 60f)
        ))
        assertEquals(listOf(highlight), PdfTextHighlightCodec.decode(PdfTextHighlightCodec.encode(listOf(highlight))))
    }

    @Test fun corruptRowsAreSkippedAndLegacyRegionStoreRemainsSeparate() {
        val text = PdfTextHighlight("text", "doc", 1, "word", listOf(PdfTextHighlightBounds(1f, 1f, 20f, 12f)))
        assertEquals(listOf(text), PdfTextHighlightCodec.decode("corrupt\n${PdfTextHighlightCodec.encode(listOf(text))}"))
        assertTrue(PdfTextHighlightCodec.decode("H1|legacy|doc|0|0.1|0.2|0.5|0.6|1").isEmpty())
    }
}
