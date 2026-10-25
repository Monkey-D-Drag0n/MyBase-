package com.example.mybase.data.local

import com.example.mybase.core.model.CapturedEntry

interface EntryLocalDataSource {
    fun readEntries(): List<CapturedEntry>
    fun writeEntries(entries: List<CapturedEntry>)
}

/** Reads the original pipe-delimited preference records and writes the same format. */
class SharedPreferencesEntryLocalDataSource(
    private val preferences: StringPreferenceStore
) : EntryLocalDataSource {
    override fun readEntries(): List<CapturedEntry> =
        EntryRecordCodec.decode(preferences.getString(KEY_ENTRIES, ""))

    override fun writeEntries(entries: List<CapturedEntry>) {
        preferences.putString(KEY_ENTRIES, EntryRecordCodec.encode(entries))
    }

    private companion object {
        const val KEY_ENTRIES = "entries"
    }
}
