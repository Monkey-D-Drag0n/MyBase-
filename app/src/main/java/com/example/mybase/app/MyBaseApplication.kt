package com.example.mybase.app

import android.app.Application
import com.example.mybase.data.local.SharedPreferencesEntryLocalDataSource
import com.example.mybase.data.local.SharedPreferencesStringStore
import com.example.mybase.data.local.SharedPreferencesLibraryResourceLocalDataSource
import com.example.mybase.data.local.SharedPreferencesDocumentReadingStateLocalDataSource
import com.example.mybase.data.local.SharedPreferencesPdfHighlightLocalDataSource
import com.example.mybase.data.repository.EntryRepository
import com.example.mybase.data.repository.LocalEntryRepository
import com.example.mybase.data.repository.LibraryResourceRepository
import com.example.mybase.data.repository.LocalLibraryResourceRepository
import com.example.mybase.data.repository.DocumentReadingStateRepository
import com.example.mybase.data.repository.LocalDocumentReadingStateRepository
import com.example.mybase.data.repository.PdfHighlightRepository
import com.example.mybase.data.repository.LocalPdfHighlightRepository
import com.example.mybase.data.local.SharedPreferencesPdfTextHighlightLocalDataSource
import com.example.mybase.data.repository.PdfTextHighlightRepository
import com.example.mybase.data.repository.LocalPdfTextHighlightRepository

class MyBaseApplication : Application() {
    val entryRepository: EntryRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        LocalEntryRepository(SharedPreferencesEntryLocalDataSource(SharedPreferencesStringStore(preferences)))
    }

    val libraryResourceRepository: LibraryResourceRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        LocalLibraryResourceRepository(SharedPreferencesLibraryResourceLocalDataSource(SharedPreferencesStringStore(preferences)))
    }

    val documentReadingStateRepository: DocumentReadingStateRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        LocalDocumentReadingStateRepository(SharedPreferencesDocumentReadingStateLocalDataSource(SharedPreferencesStringStore(preferences)))
    }

    val pdfHighlightRepository: PdfHighlightRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        LocalPdfHighlightRepository(SharedPreferencesPdfHighlightLocalDataSource(SharedPreferencesStringStore(preferences)))
    }

    val pdfTextHighlightRepository: PdfTextHighlightRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        LocalPdfTextHighlightRepository(SharedPreferencesPdfTextHighlightLocalDataSource(SharedPreferencesStringStore(preferences)))
    }

    private companion object {
        const val PREFERENCES_NAME = "mybase_local"
    }
}
