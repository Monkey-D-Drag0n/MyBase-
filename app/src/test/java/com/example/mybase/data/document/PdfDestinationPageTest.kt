package com.example.mybase.data.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfDestinationPageTest {
    @Test fun platformDestinationPageIsUsedAsZeroBasedRendererIndex() {
        assertEquals(0, resolvePdfDestinationPage(0, 5))
        assertEquals(4, resolvePdfDestinationPage(4, 5))
    }

    @Test fun outOfRangeOrNegativeDestinationIsIgnored() {
        assertNull(resolvePdfDestinationPage(-1, 5))
        assertNull(resolvePdfDestinationPage(5, 5))
        assertNull(resolvePdfDestinationPage(0, 0))
    }
}
