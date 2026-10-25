package com.example.mybase.data.document

import android.content.ContentResolver
import android.net.Uri
import java.io.InputStreamReader
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class DocumentText(val text: String, val truncated: Boolean)

interface DocumentTextSource { suspend fun read(uri: String, maxCharacters: Int = 300_000): DocumentText }

class AndroidDocumentTextSource(private val resolver: ContentResolver) : DocumentTextSource {
    override suspend fun read(uri: String, maxCharacters: Int): DocumentText {
        val stream = resolver.openInputStream(Uri.parse(uri)) ?: error("This document is unavailable")
        stream.use { input ->
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
            InputStreamReader(input, decoder).use { reader ->
                val limit = maxCharacters.coerceIn(1, 1_000_000)
                val text = StringBuilder(minOf(limit, 32_768))
                val buffer = CharArray(8_192)
                while (text.length < limit) {
                    val read = reader.read(buffer, 0, minOf(buffer.size, limit - text.length))
                    if (read < 0) break
                    text.append(buffer, 0, read)
                }
                val truncated = text.length == limit && reader.read() >= 0
                val content = text.toString().removePrefix("\uFEFF")
                return DocumentText(content, truncated)
            }
        }
    }
}
