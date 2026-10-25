package com.example.mybase.data.repository

import com.example.mybase.data.local.SharedPreferencesDocumentReadingStateLocalDataSource
import com.example.mybase.data.local.StringPreferenceStore
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalDocumentReadingStateRepositoryTest {
    @Test fun bookmarksAreUniqueAndProgressSurvivesRepositoryReload() {
        val preferences = MemoryPreferences()
        val source = SharedPreferencesDocumentReadingStateLocalDataSource(preferences)
        val repository = LocalDocumentReadingStateRepository(source)
        repository.savePage("library-id", 9)
        repository.addBookmark("library-id", 2)
        repository.addBookmark("library-id", 2)
        repository.addBookmark("library-id", 6)

        val reloaded = LocalDocumentReadingStateRepository(source)
        assertEquals(9, reloaded.stateFor("library-id").lastPage)
        assertEquals(listOf(2, 6), reloaded.stateFor("library-id").bookmarks)
        reloaded.removeBookmark("library-id", 2)
        assertEquals(listOf(6), reloaded.stateFor("library-id").bookmarks)
    }

    @Test fun removingDocumentStateDoesNotTouchOtherDocuments() {
        val repository = LocalDocumentReadingStateRepository(SharedPreferencesDocumentReadingStateLocalDataSource(MemoryPreferences()))
        repository.addBookmark("one", 1); repository.addBookmark("two", 2)
        repository.delete("one")
        assertEquals(listOf("two"), repository.states.value.map { it.documentId })
    }

    @Test fun positionWithoutBookmarksAlsoSurvivesReload() {
        val preferences = MemoryPreferences()
        val source = SharedPreferencesDocumentReadingStateLocalDataSource(preferences)
        LocalDocumentReadingStateRepository(source).savePage("position-only", 17)
        assertEquals(17, LocalDocumentReadingStateRepository(source).stateFor("position-only").lastPage)
    }

    private class MemoryPreferences : StringPreferenceStore {
        private val values = mutableMapOf<String, String>()
        override fun getString(key: String, defaultValue: String) = values[key] ?: defaultValue
        override fun putString(key: String, value: String) { values[key] = value }
    }
}
