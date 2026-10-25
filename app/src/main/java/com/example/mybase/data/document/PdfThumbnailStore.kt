package com.example.mybase.data.document

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfThumbnailStore(
    private val pageSource: PdfPageSource,
    private val documentAccess: DocumentAccess,
    maxBytes: Int = 3 * 1024 * 1024
) {
    private val cache = BoundedValueCache<String, Bitmap>(maxBytes) { bitmap -> bitmap.allocationByteCount }

    suspend fun load(uri: String, mimeType: String = ""): Bitmap? = withContext(Dispatchers.IO) {
        val key = cacheKey(uri, mimeType)
        if (!documentAccess.isReadable(uri)) { cache.remove(key); return@withContext null }
        cache.get(key)?.takeUnless(Bitmap::isRecycled)?.let { return@withContext it }
        cache.remove(key)
        val generated = runCatching {
            pageSource.open(uri).use { session -> if (session.pageCount > 0) session.render(0, 360) else null }
        }.getOrNull() ?: return@withContext null
        cache.put(key, generated)
        generated
    }

    fun invalidate(uri: String, mimeType: String = "") = cache.remove(cacheKey(uri, mimeType))

    companion object {
        fun cacheKey(uri: String, mimeType: String): String {
            val value = uri.trim()
            val separator = value.indexOf(':')
            val normalized = if (separator > 0) value.substring(0, separator).lowercase() + value.substring(separator) else value
            return "$normalized|${mimeType.lowercase()}"
        }
    }
}
