package com.example.mybase.feature.library

import com.example.mybase.core.model.LibraryResource
import java.net.URLDecoder

/** Older/imported records may have used their URI as title. Keep that implementation detail off cards. */
internal fun readableResourceTitle(resource: LibraryResource): String {
    val title = resource.title.trim()
    if (title.isNotEmpty() && !title.startsWith("content://", true) && !title.startsWith("file://", true)) return title
    val titleLeaf = title.substringBefore('?').trimEnd('/').substringAfterLast('/')
    val referenceLeaf = resource.reference.substringBefore('?').trimEnd('/').substringAfterLast('/')
    val leaf = titleLeaf.takeIf { it.isNotBlank() && !it.contains("://") } ?: referenceLeaf
    val decoded = runCatching { URLDecoder.decode(leaf, Charsets.UTF_8.name()) }.getOrDefault(leaf).trim()
    return decoded.takeIf { it.isNotBlank() && !it.contains("://") } ?: "Untitled resource"
}

internal fun readableReference(resource: LibraryResource): String = when {
    resource.type == com.example.mybase.core.model.ResourceType.Link -> runCatching {
        val uri = java.net.URI(resource.reference)
        uri.host?.let { if (uri.port >= 0) "$it:${uri.port}" else it } ?: resource.reference
    }.getOrDefault(resource.reference)
    else -> resource.type.label
}
