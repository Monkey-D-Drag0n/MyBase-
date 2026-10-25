package com.example.mybase.data.local

import com.example.mybase.core.model.PdfTextHighlight

interface PdfTextHighlightLocalDataSource {
    fun read(): List<PdfTextHighlight>
    fun write(highlights: List<PdfTextHighlight>)
}

class SharedPreferencesPdfTextHighlightLocalDataSource(private val preferences: StringPreferenceStore) : PdfTextHighlightLocalDataSource {
    override fun read() = PdfTextHighlightCodec.decode(preferences.getString(KEY, ""))
    override fun write(highlights: List<PdfTextHighlight>) = preferences.putString(KEY, PdfTextHighlightCodec.encode(highlights))
    private companion object { const val KEY = "library.pdf_text_highlights.v1" }
}
