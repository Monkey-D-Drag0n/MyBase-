package com.example.mybase.data.local

import com.example.mybase.core.model.PdfHighlight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfHighlightCodecTest {
    @Test fun normalizedPageCoordinatesSurviveSerialization() {
        val mark = PdfHighlight("h-1", "library|資料", 3, .12f, .25f, .77f, .58f)
        assertEquals(listOf(mark), PdfHighlightCodec.decode(PdfHighlightCodec.encode(listOf(mark))))
    }
    @Test fun corruptOrOutOfPageAnnotationsAreSkippedWithoutAffectingValidOnes() {
        val good = PdfHighlight("good", "doc", 0, .1f, .1f, .4f, .5f)
        val corrupt = "H1|bad|doc|-1|0|0|2|2|10"
        assertEquals(listOf(good), PdfHighlightCodec.decode("$corrupt\n${PdfHighlightCodec.encode(listOf(good))}"))
        assertTrue(PdfHighlightCodec.decode("broken metadata").isEmpty())
    }
}
