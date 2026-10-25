package com.example.mybase.core.model

/** Text selection stored by MyBase (new bounds normalized; older TH1 page-point bounds remain readable). */
data class PdfTextHighlight(
    val id: String,
    val documentId: String,
    val page: Int,
    val text: String,
    val bounds: List<PdfTextHighlightBounds>,
    val colorArgb: Int = 0x66FFE45C
) {
    fun isValid(): Boolean = id.isNotBlank() && documentId.isNotBlank() && page >= 0 && text.isNotBlank() &&
        bounds.isNotEmpty() && bounds.all { it.isValid() }
}

data class PdfTextHighlightBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun isValid(): Boolean = left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() &&
        left >= 0f && top >= 0f && right > left && bottom > top
}
