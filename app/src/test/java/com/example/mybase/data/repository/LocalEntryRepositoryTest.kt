package com.example.mybase.data.repository

import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.data.local.SharedPreferencesEntryLocalDataSource
import com.example.mybase.data.local.StringPreferenceStore
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalEntryRepositoryTest {
    @Test
    fun savedNotesAndCompletedTasksAreAvailableFromANewRepositoryInstance() {
        val preferences = InMemoryStringPreferenceStore()
        val repository = LocalEntryRepository(SharedPreferencesEntryLocalDataSource(preferences))
        val saved = listOf(
            CapturedEntry("Read chapter", "Note", 100L, body = "A useful thought", categories = listOf("Reading"), tags = listOf("book", "idea")),
            CapturedEntry("Submit draft", "Task", 200L, completed = true, dueDateEpochDay = 20_000L, priority = com.example.mybase.core.model.TaskPriority.High)
        )

        repository.saveEntries(saved)
        val repositoryAfterRestart = LocalEntryRepository(SharedPreferencesEntryLocalDataSource(preferences))

        assertEquals(saved, repositoryAfterRestart.entries.value)
    }

    private class InMemoryStringPreferenceStore : StringPreferenceStore {
        private val values = mutableMapOf<String, String>()

        override fun getString(key: String, defaultValue: String): String = values[key] ?: defaultValue
        override fun putString(key: String, value: String) {
            values[key] = value
        }
    }
}
