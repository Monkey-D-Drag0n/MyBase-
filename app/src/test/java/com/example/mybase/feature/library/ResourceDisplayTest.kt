package com.example.mybase.feature.library

import com.example.mybase.core.model.LibraryResource
import com.example.mybase.core.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceDisplayTest {
    @Test fun rawContentUriTitleFallsBackToDecodedDocumentName() {
        assertEquals("Study notes.pdf", readableResourceTitle(resource("content://provider/tree/a%20folder/Study%20notes.pdf", "content://provider/document/3")))
    }

    @Test fun normalTitleIsPreserved() {
        assertEquals("My favorite paper", readableResourceTitle(resource("My favorite paper", "content://provider/document/3")))
    }

    private fun resource(title: String, reference: String) = LibraryResource("id", title, ResourceType.Pdf, reference, createdAtMillis = 0)
}
