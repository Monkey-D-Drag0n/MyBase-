package com.example.mybase.data.repository

import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.data.local.EntryLocalDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface EntryRepository {
    val entries: StateFlow<List<CapturedEntry>>
    fun saveEntries(entries: List<CapturedEntry>)
    fun addEntry(entry: CapturedEntry)
    fun updateEntry(id: String, transform: (CapturedEntry) -> CapturedEntry)
    fun deleteEntry(id: String)
}

class LocalEntryRepository(
    private val localDataSource: EntryLocalDataSource
) : EntryRepository {
    private val _entries = MutableStateFlow(localDataSource.readEntries())
    override val entries: StateFlow<List<CapturedEntry>> = _entries.asStateFlow()

    override fun saveEntries(entries: List<CapturedEntry>) {
        localDataSource.writeEntries(entries)
        _entries.value = entries
    }

    override fun addEntry(entry: CapturedEntry) = saveEntries(listOf(entry) + _entries.value)

    override fun updateEntry(id: String, transform: (CapturedEntry) -> CapturedEntry) {
        val current = _entries.value
        if (current.none { it.id == id }) return
        saveEntries(current.map { if (it.id == id) transform(it) else it })
    }

    override fun deleteEntry(id: String) {
        val current = _entries.value
        if (current.any { it.id == id }) saveEntries(current.filterNot { it.id == id })
    }
}
