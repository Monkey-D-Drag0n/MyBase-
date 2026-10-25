package com.example.mybase.data.local

import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.core.model.TaskPriority
import java.util.Base64

/** Codec for the existing newline and pipe-delimited SharedPreferences value. */
internal object EntryRecordCodec {
    fun decode(serialized: String): List<CapturedEntry> {
        val legacyIdCounts = mutableMapOf<String, Int>()
        return serialized.lineSequence()
            .filter(String::isNotBlank)
            .mapNotNull { row ->
                val fields = row.split('|')
                if (fields.size < 3) return@mapNotNull null
                val base = CapturedEntry(
                    title = fields[0],
                    kind = fields[1],
                    createdAtMillis = fields[2].toLongOrNull() ?: 0L,
                    completed = fields.getOrNull(3) == "true",
                    dueDateEpochDay = fields.getOrNull(4)?.toLongOrNull(),
                    priority = TaskPriority.fromStored(fields.getOrNull(5)),
                    body = decodeText(fields.getOrNull(7)),
                    categories = decodeList(fields.getOrNull(8)),
                    tags = decodeList(fields.getOrNull(9))
                )
                val storedId = fields.getOrNull(6).orEmpty()
                if (storedId.isNotBlank()) base.copy(id = storedId)
                else {
                    val occurrence = legacyIdCounts[base.id] ?: 0
                    legacyIdCounts[base.id] = occurrence + 1
                    if (occurrence == 0) base else base.copy(id = "${base.id}:$occurrence")
                }
            }
            .toList()
    }

    fun encode(entries: List<CapturedEntry>): String = entries.joinToString("\n") { entry ->
        listOf(
            entry.title.replace('|', ' ').replace('\n', ' '),
            entry.kind,
            entry.createdAtMillis.toString(),
            entry.completed.toString(),
            entry.dueDateEpochDay?.toString().orEmpty(),
            entry.priority.storedValue,
            entry.id,
            encodeText(entry.body),
            encodeList(entry.categories),
            encodeList(entry.tags)
        ).joinToString("|")
    }

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodeText(value: String?): String = runCatching {
        String(
            Base64.getUrlDecoder().decode(value.orEmpty()),
            Charsets.UTF_8
        )
    }.getOrDefault("")

    private fun encodeList(values: List<String>): String = encodeText(values.joinToString("\u001f"))
    private fun decodeList(value: String?): List<String> =
        decodeText(value).split('\u001f').filter(String::isNotEmpty)
}
