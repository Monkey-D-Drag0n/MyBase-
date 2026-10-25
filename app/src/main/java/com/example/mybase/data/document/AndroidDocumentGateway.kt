package com.example.mybase.data.document

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.mybase.core.model.ResourceType

data class DocumentLocation(
    val uri: String,
    val name: String,
    val mimeType: String,
    val isDirectory: Boolean,
    val sizeBytes: Long? = null,
    val modifiedAtMillis: Long? = null
)

interface DocumentAccess {
    fun retainFilePermission(uri: Uri): Boolean
    fun retainTreePermission(uri: Uri): Boolean
    fun persistedFolders(): List<DocumentLocation>
    fun queryLocation(uri: String): DocumentLocation?
    fun children(folderUri: String): List<DocumentLocation>
    fun resourceType(mimeType: String, displayName: String): ResourceType
    fun isReadable(uri: String): Boolean
}

/** SAF access stays here; callers only receive content URIs and display metadata. */
class AndroidDocumentGateway(private val resolver: ContentResolver) : DocumentAccess {
    override fun retainFilePermission(uri: Uri): Boolean = runCatching {
        resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        true
    }.getOrDefault(false)

    override fun retainTreePermission(uri: Uri): Boolean {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return runCatching {
            resolver.takePersistableUriPermission(uri, flags)
            true
        }.getOrElse {
            runCatching {
                resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                true
            }.getOrDefault(false)
        }
    }

    override fun persistedFolders(): List<DocumentLocation> = resolver.persistedUriPermissions
        .filter { it.isReadPermission && DocumentsContract.isTreeUri(it.uri) }
        .mapNotNull { queryLocation(it.uri.toString()) }

    override fun queryLocation(uri: String): DocumentLocation? = runCatching {
        val parsedUri = Uri.parse(uri)
        val cursor = runCatching {
            resolver.query(parsedUri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)
        }.getOrNull() ?: resolver.query(parsedUri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        cursor?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val name = nameIndex.takeIf { it >= 0 }?.let(cursor::getString).orEmpty().ifBlank { "Untitled document" }
            val mime = mimeIndex.takeIf { it >= 0 }?.let(cursor::getString).orEmpty().ifBlank { resolver.getType(parsedUri).orEmpty() }
            val size = sizeIndex.takeIf { it >= 0 }?.let(cursor::getLong)?.takeIf { it >= 0 }
            val modified = modifiedIndex.takeIf { it >= 0 }?.let(cursor::getLong)?.takeIf { it > 0 }
            val directory = mime == DocumentsContract.Document.MIME_TYPE_DIR
            DocumentLocation(parsedUri.normalizeScheme().toString(), name, mime, directory, size, modified)
        }
    }.getOrNull()

    override fun children(folderUri: String): List<DocumentLocation> {
        val parsedFolderUri = Uri.parse(folderUri)
        val treeUri = if (DocumentsContract.isTreeUri(parsedFolderUri)) parsedFolderUri else return emptyList()
        return runCatching {
            val folderId = DocumentsContract.getDocumentId(parsedFolderUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderId)
            resolver.query(childrenUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                buildList {
                    while (cursor.moveToNext()) {
                        val id = idIndex.takeIf { it >= 0 }?.let(cursor::getString) ?: continue
                        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        val mime = mimeIndex.takeIf { it >= 0 }?.let(cursor::getString).orEmpty()
                        add(DocumentLocation(uri.normalizeScheme().toString(), nameIndex.takeIf { it >= 0 }?.let(cursor::getString).orEmpty().ifBlank { "Untitled document" }, mime, mime == DocumentsContract.Document.MIME_TYPE_DIR, sizeIndex.takeIf { it >= 0 }?.let(cursor::getLong)?.takeIf { it >= 0 }))
                    }
                }.sortedWith(compareByDescending<DocumentLocation> { it.isDirectory }.thenBy { it.name.lowercase() })
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    override fun resourceType(mimeType: String, displayName: String): ResourceType = when {
        mimeType.equals("application/pdf", true) || displayName.endsWith(".pdf", true) -> ResourceType.Pdf
        mimeType.startsWith("text/", true) || mimeType.startsWith("application/", true) -> ResourceType.Document
        else -> ResourceType.Other
    }

    override fun isReadable(uri: String): Boolean = runCatching { resolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false }.getOrDefault(false)
}
