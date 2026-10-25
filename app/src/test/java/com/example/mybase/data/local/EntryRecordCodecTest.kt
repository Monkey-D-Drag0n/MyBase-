package com.example.mybase.data.local

import com.example.mybase.core.model.CapturedEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryRecordCodecTest {
    @Test
    fun decodesLegacyAndCurrentRecordsIncludingCompletion() {
        val records = "First note|Note|1710000000000\nFinish report|Task|1710000001000|true\nOpen task|Task|1710000002000|false"

        assertEquals(
            listOf(
                CapturedEntry("First note", "Note", 1710000000000L),
                CapturedEntry("Finish report", "Task", 1710000001000L, completed = true),
                CapturedEntry("Open task", "Task", 1710000002000L, completed = false)
            ),
            EntryRecordCodec.decode(records)
        )
    }

    @Test
    fun ignoresMalformedRowsAndPreservesValidRows() {
        val decoded = EntryRecordCodec.decode("bad row\nValid|Note|not-a-time|false\n")

        assertEquals(listOf(CapturedEntry("Valid", "Note", 0L)), decoded)
    }

    @Test
    fun writesCompletionAndSanitizesRecordDelimiters() {
        val entries = listOf(
            CapturedEntry("A|B\nC", "Task", 42L, completed = true),
            CapturedEntry("Keep me", "Note", 43L)
        )

        val encoded = EntryRecordCodec.encode(entries)

        assertEquals(
            "A B C|Task|42|true||normal|${entries[0].id}|||\nKeep me|Note|43|false||normal|${entries[1].id}|||",
            encoded
        )
        assertEquals(entries.map { it.copy(title = it.title.replace('|', ' ').replace('\n', ' ')) }, EntryRecordCodec.decode(encoded))
    }

    @Test fun roundTripsNoteBodyCategoriesAndTagsWithDelimiters() {
        val entry = CapturedEntry("Note", "Note", 44L, body = "one|two\nthree", categories = listOf("Work", "Ideas"), tags = listOf("a|b", "tag"))
        assertEquals(entry, EntryRecordCodec.decode(EntryRecordCodec.encode(listOf(entry))).single())
    }

    @Test fun oldCurrentFormatDefaultsNewFields() {
        val decoded = EntryRecordCodec.decode("Title|Note|1|false||normal|saved-id").single()
        assertEquals("", decoded.body)
        assertEquals(emptyList<String>(), decoded.categories)
        assertEquals(emptyList<String>(), decoded.tags)
    }
}
