package com.example.mybase.core.model

/** A PDF region annotation in normalized page coordinates. It is stored by MyBase, never written into the source PDF. */
data class PdfHighlight(
    val id: String,
    val documentId: String,
    val page: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val colorArgb: Int = 0x66FFE45C
) {
    fun isValid(): Boolean = documentId.isNotBlank() && page >= 0 && left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f && right > left && bottom > top
}
