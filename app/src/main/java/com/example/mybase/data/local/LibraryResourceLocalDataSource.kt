package com.example.mybase.data.local

import com.example.mybase.core.model.LibraryResource

interface LibraryResourceLocalDataSource {
    fun readResources(): List<LibraryResource>
    fun writeResources(resources: List<LibraryResource>)
}

class SharedPreferencesLibraryResourceLocalDataSource(
    private val preferences: StringPreferenceStore
) : LibraryResourceLocalDataSource {
    override fun readResources(): List<LibraryResource> = LibraryResourceCodec.decode(preferences.getString(KEY_LIBRARY_RESOURCES, ""))
    override fun writeResources(resources: List<LibraryResource>) = preferences.putString(KEY_LIBRARY_RESOURCES, LibraryResourceCodec.encode(resources))

    private companion object {
        const val KEY_LIBRARY_RESOURCES = "library.resources.v1"
    }
}
