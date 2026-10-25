package com.example.mybase.feature.library

import com.example.mybase.core.model.LibraryResource
import com.example.mybase.core.model.ResourceType

enum class DocumentPreviewKind { Pdf, PlainText, Markdown, External }

fun previewKind(resource: LibraryResource): DocumentPreviewKind {
    val name = resource.title.lowercase()
    val mime = resource.mimeType.lowercase()
    return when {
        resource.type == ResourceType.Pdf || mime.startsWith("application/pdf") || name.endsWith(".pdf") -> DocumentPreviewKind.Pdf
        mime.startsWith("text/markdown") || mime.startsWith("text/x-markdown") || mime.startsWith("text/md") || name.endsWith(".md") || name.endsWith(".markdown") -> DocumentPreviewKind.Markdown
        mime.startsWith("text/") || name.endsWith(".txt") -> DocumentPreviewKind.PlainText
        else -> DocumentPreviewKind.External
    }
}
