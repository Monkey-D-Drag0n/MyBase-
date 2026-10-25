package com.example.mybase.data.local

import com.example.mybase.core.model.LibraryResource
import com.example.mybase.core.model.ResourceType
import java.util.Base64

/** Versioned, line based storage. Every user supplied field is URL-safe Base64 encoded. */
internal object LibraryResourceCodec {
    private const val VERSION = "LIB2"
    private const val EMPTY = "~"

    fun encode(resources: List<LibraryResource>): String = resources.joinToString("\n") { item ->
        listOf(
            VERSION, pack(item.id), pack(item.title), item.type.storedValue, pack(item.reference),
            pack(item.description), pack(item.category), packList(item.tags), item.createdAtMillis.toString(), item.updatedAtMillis.toString(), pack(item.mimeType)
        ).joinToString("|")
    }

    fun decode(serialized: String): List<LibraryResource> = serialized.lineSequence()
        .filter(String::isNotBlank)
        .mapNotNull { row ->
            runCatching {
                val fields = row.split('|')
                val version = fields.firstOrNull()
                if ((version != VERSION && version != "LIB1") || fields.size < 5) return@mapNotNull null
                val id = unpack(fields[1])
                val title = unpack(fields[2])
                if (id.isBlank() || title.isBlank()) return@mapNotNull null
                val created = fields.getOrNull(8)?.toLongOrNull() ?: 0L
                LibraryResource(
                    id = id, title = title, type = ResourceType.fromStored(fields[3]),
                    reference = fields.getOrNull(4)?.let(::unpack).orEmpty(),
                    description = fields.getOrNull(5)?.let(::unpack).orEmpty(),
                    category = fields.getOrNull(6)?.let(::unpack).orEmpty(),
                    tags = fields.getOrNull(7)?.let(::unpackList).orEmpty(), createdAtMillis = created,
                    updatedAtMillis = fields.getOrNull(9)?.toLongOrNull() ?: created,
                    mimeType = if (version == VERSION) fields.getOrNull(10)?.let(::unpack).orEmpty() else ""
                )
            }.getOrNull()
        }.toList()

    private fun pack(value: String): String = if (value.isEmpty()) EMPTY else Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun unpack(value: String): String = if (value == EMPTY) "" else String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
    private fun packList(values: List<String>): String = values.size.toString() + ":" + values.joinToString(":") { pack(it) }
    private fun unpackList(value: String): List<String> {
        val pieces = value.split(':')
        val count = pieces.firstOrNull()?.toIntOrNull()?.coerceIn(0, 500) ?: return emptyList()
        return pieces.drop(1).take(count).map(::unpack)
    }
}
