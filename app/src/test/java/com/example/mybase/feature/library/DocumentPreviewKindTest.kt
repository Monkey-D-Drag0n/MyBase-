package com.example.mybase.feature.library

import com.example.mybase.core.model.LibraryResource
import com.example.mybase.core.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentPreviewKindTest {
    @Test fun detectsTypesFromNameMimeAndLegacyType() {
        assertEquals(DocumentPreviewKind.Pdf, previewKind(item("paper.pdf", "", ResourceType.Document)))
        assertEquals(DocumentPreviewKind.Markdown, previewKind(item("untitled", "text/markdown", ResourceType.Document)))
        assertEquals(DocumentPreviewKind.PlainText, previewKind(item("readme.TXT", "", ResourceType.Document)))
        assertEquals(DocumentPreviewKind.Pdf, previewKind(item("file", "", ResourceType.Pdf)))
        assertEquals(DocumentPreviewKind.External, previewKind(item("archive.zip", "application/zip", ResourceType.Document)))
    }
    private fun item(name: String, mime: String, type: ResourceType) = LibraryResource("id", name, type, "content://doc", createdAtMillis = 1L, mimeType = mime)
}
