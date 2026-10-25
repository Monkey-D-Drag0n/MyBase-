package com.example.mybase.data.local

import com.example.mybase.core.model.LibraryResource
import com.example.mybase.core.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryResourceCodecTest {
    @Test fun roundTripsAllResourceFieldsIncludingDelimitersUnicodeAndNewlines() {
        val item = LibraryResource(
            id = "stable|id", title = "東京 | study\nnotes 📚", type = ResourceType.Document,
            reference = "content://documents/資料|1\npage", description = "Résumé\nsecond line | detail",
            category = "École", tags = listOf("café", "pipe|tag", "line\nbreak", ""), createdAtMillis = 123L, updatedAtMillis = 456L,
            mimeType = "text/markdown; charset=utf-8"
        )
        assertEquals(item, LibraryResourceCodec.decode(LibraryResourceCodec.encode(listOf(item))).single())
    }

    @Test fun malformedRowsAreSkippedAndMissingOptionalColumnsDefaultSafely() {
        val decoded = LibraryResourceCodec.decode("not-a-library-row\nLIB1|aWQ|VGl0bGU|link|aHR0cHM6Ly9leGFtcGxlLmNvbQ\nLIB1|too|short")
        assertEquals(1, decoded.size)
        assertEquals("Title", decoded.single().title)
        assertEquals("https://example.com", decoded.single().reference)
        assertTrue(decoded.single().tags.isEmpty())
        assertEquals(0L, decoded.single().createdAtMillis)
        assertEquals("", decoded.single().mimeType)
    }
}
