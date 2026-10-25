package com.example.mybase.data.local

import com.example.mybase.core.model.PdfHighlight

interface PdfHighlightLocalDataSource { fun read(): List<PdfHighlight>; fun write(highlights: List<PdfHighlight>) }

class SharedPreferencesPdfHighlightLocalDataSource(private val preferences: StringPreferenceStore) : PdfHighlightLocalDataSource {
    override fun read() = PdfHighlightCodec.decode(preferences.getString(KEY, ""))
    override fun write(highlights: List<PdfHighlight>) = preferences.putString(KEY, PdfHighlightCodec.encode(highlights))
    private companion object { const val KEY = "library.pdf_highlights.v1" }
}
