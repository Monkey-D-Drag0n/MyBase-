package com.example.mybase.data.local

import com.example.mybase.core.model.DocumentReadingState
import java.util.Base64

internal object DocumentReadingStateCodec {
    fun encode(states: List<DocumentReadingState>): String = states.joinToString("\n") { state ->
        val id = Base64.getUrlEncoder().withoutPadding().encodeToString(state.documentId.toByteArray(Charsets.UTF_8))
        "$id|${state.lastPage.coerceAtLeast(0)}|${state.bookmarks.filter { it >= 0 }.distinct().sorted().joinToString(",")}"
    }

    fun decode(serialized: String): List<DocumentReadingState> = serialized.lineSequence().mapNotNull { row ->
        runCatching {
            val fields = row.split('|')
            if (fields.size != 3) return@mapNotNull null
            val id = String(Base64.getUrlDecoder().decode(fields[0]), Charsets.UTF_8)
            if (id.isBlank()) return@mapNotNull null
            val page = fields[1].toIntOrNull() ?: return@mapNotNull null
            DocumentReadingState(id, page.coerceAtLeast(0),
                fields[2].split(',').mapNotNull(String::toIntOrNull).filter { it >= 0 }.distinct().sorted())
        }.getOrNull()
    }.toList()
}
