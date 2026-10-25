package com.example.mybase.data.repository

import com.example.mybase.core.model.LibraryResource
import com.example.mybase.core.model.ResourceType
import com.example.mybase.data.local.SharedPreferencesLibraryResourceLocalDataSource
import com.example.mybase.data.local.StringPreferenceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLibraryResourceRepositoryTest {
    @Test fun resourcesSurviveRepositoryReloadAndUseIndependentPreferenceKey() {
        val preferences = MemoryPreferences()
        preferences.putString("entries", "legacy|Task|10|false||normal|task-id")
        val repository = LocalLibraryResourceRepository(SharedPreferencesLibraryResourceLocalDataSource(preferences))
        val resource = LibraryResource("r-1", "Kotlin guide", ResourceType.Link, "https://example.com/guide", "A useful guide", "Learning", listOf("kotlin"), 10L, 11L)
        repository.add(resource)

        val afterReload = LocalLibraryResourceRepository(SharedPreferencesLibraryResourceLocalDataSource(preferences))
        assertEquals(listOf(resource), afterReload.resources.value)
        assertEquals("legacy|Task|10|false||normal|task-id", preferences.getString("entries", ""))
    }

    @Test fun updateAndDeleteOnlyAffectMatchingStableId() {
        val repo = LocalLibraryResourceRepository(SharedPreferencesLibraryResourceLocalDataSource(MemoryPreferences()))
        val first = resource("first", "First")
        val second = resource("second", "Second")
        repo.add(first); repo.add(second)
        repo.update(first.id) { it.copy(title = "First updated", updatedAtMillis = 20L) }
        assertEquals(setOf(first.id, second.id), repo.resources.value.map { it.id }.toSet())
        assertEquals("First updated", repo.resources.value.first { it.id == first.id }.title)
        repo.delete(second.id)
        assertEquals(listOf(first.id), repo.resources.value.map { it.id })
    }

    private fun resource(id: String, title: String) = LibraryResource(id, title, ResourceType.Other, "ref", createdAtMillis = 1L)

    private class MemoryPreferences : StringPreferenceStore {
        private val values = mutableMapOf<String, String>()
        override fun getString(key: String, defaultValue: String): String = values[key] ?: defaultValue
        override fun putString(key: String, value: String) { values[key] = value }
    }
}
