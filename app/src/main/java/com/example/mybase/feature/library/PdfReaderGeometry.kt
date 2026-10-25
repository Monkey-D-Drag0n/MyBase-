package com.example.mybase.feature.library

import androidx.compose.ui.geometry.Offset
import com.example.mybase.core.model.PdfTextHighlightBounds
import com.example.mybase.data.document.PdfPageSize
import kotlin.math.max

/** Per-page zoom and pan; pan values are fractions of the currently available travel. */
data class PdfPageTransform(
    val zoom: Float = 1f,
    val panXFraction: Float = 0f,
    val panYFraction: Float = 0f
) {
    fun zoomBy(factor: Float): PdfPageTransform = copy(zoom = (zoom * factor).coerceIn(1f, 4f))

    fun panBy(delta: Offset, viewportWidth: Float, viewportHeight: Float): PdfPageTransform {
        val travelX = max(0f, viewportWidth * (zoom - 1f) / 2f)
        val travelY = max(0f, viewportHeight * (zoom - 1f) / 2f)
        return copy(
            panXFraction = if (travelX == 0f) 0f else (panXFraction + delta.x / travelX).coerceIn(-1f, 1f),
            panYFraction = if (travelY == 0f) 0f else (panYFraction + delta.y / travelY).coerceIn(-1f, 1f)
        )
    }

    fun translationX(viewportWidth: Float): Float = panXFraction * max(0f, viewportWidth * (zoom - 1f) / 2f)
    fun translationY(viewportHeight: Float): Float = panYFraction * max(0f, viewportHeight * (zoom - 1f) / 2f)

    fun fit(): PdfPageTransform = PdfPageTransform()
}

data class PdfPagePoint(val x: Float, val y: Float)
fun PdfPagePoint.asOffset(): Offset = Offset(x, y)

fun panFractionToRevealPageY(pageYFraction: Float, zoom: Float): Float =
    if (zoom <= 1f) 0f else (((.5f - pageYFraction.coerceIn(0f, 1f)) * zoom) / ((zoom - 1f) / 2f)).coerceIn(-1f, 1f)

/** Inverts a centered graphics-layer zoom/pan into normalized page coordinates. */
fun pagePointFromViewport(point: Offset, width: Float, height: Float, transform: PdfPageTransform): PdfPagePoint {
    if (width <= 0f || height <= 0f) return PdfPagePoint(0f, 0f)
    val x = ((point.x - width / 2f - transform.translationX(width)) / transform.zoom + width / 2f) / width
    val y = ((point.y - height / 2f - transform.translationY(height)) / transform.zoom + height / 2f) / height
    return PdfPagePoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
}

/** Supports TH1 annotations written as page points by earlier builds and normalized annotations. */
fun normalizedTextHighlightBounds(bounds: List<PdfTextHighlightBounds>, pageSize: PdfPageSize): List<PdfTextHighlightBounds> {
    val pointsStored = bounds.any { it.right > 1f || it.bottom > 1f }
    return bounds.mapNotNull { b ->
        val divisorX = if (pointsStored) pageSize.width.toFloat().coerceAtLeast(1f) else 1f
        val divisorY = if (pointsStored) pageSize.height.toFloat().coerceAtLeast(1f) else 1f
        val left = (b.left / divisorX).coerceIn(0f, 1f)
        val top = (b.top / divisorY).coerceIn(0f, 1f)
        val right = (b.right / divisorX).coerceIn(0f, 1f)
        val bottom = (b.bottom / divisorY).coerceIn(0f, 1f)
        PdfTextHighlightBounds(left, top, right, bottom).takeIf(PdfTextHighlightBounds::isValid)
    }
}

internal fun textHighlightBoundsAtRenderSize(bounds: PdfTextHighlightBounds, width: Int, height: Int): PdfTextHighlightBounds =
    PdfTextHighlightBounds(bounds.left * width, bounds.top * height, bounds.right * width, bounds.bottom * height)
