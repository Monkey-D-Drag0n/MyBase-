package com.example.mybase.data.local

import com.example.mybase.core.model.DocumentReadingState

interface DocumentReadingStateLocalDataSource {
    fun read(): List<DocumentReadingState>
    fun write(states: List<DocumentReadingState>)
}

class SharedPreferencesDocumentReadingStateLocalDataSource(private val preferences: StringPreferenceStore) : DocumentReadingStateLocalDataSource {
    override fun read() = DocumentReadingStateCodec.decode(preferences.getString(KEY, ""))
    override fun write(states: List<DocumentReadingState>) = preferences.putString(KEY, DocumentReadingStateCodec.encode(states))
    private companion object { const val KEY = "library.document_state.v1" }
}
