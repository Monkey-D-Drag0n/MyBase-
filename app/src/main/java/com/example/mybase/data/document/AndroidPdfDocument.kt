package com.example.mybase.data.document

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Point
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.models.selection.SelectionBoundary
import android.os.Build
import android.os.ext.SdkExtensions
import android.net.Uri
import android.os.ParcelFileDescriptor

interface PdfPageSource {
    fun open(uri: String): PdfSession
}

interface PdfSession : AutoCloseable {
    val pageCount: Int
    val supportsTextSelection: Boolean
    fun pageSize(pageIndex: Int): PdfPageSize
    fun render(pageIndex: Int, targetWidth: Int): Bitmap
    fun selectText(pageIndex: Int, startX: Float, startY: Float, stopX: Float, stopY: Float): PdfTextSelection?
    fun internalDestination(pageIndex: Int, x: Float, y: Float): PdfInternalDestination?
}

data class PdfPageSize(val width: Int, val height: Int)
data class PdfSelectionBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)
data class PdfTextSelection(val page: Int, val text: String, val bounds: List<PdfSelectionBounds>)
data class PdfInternalDestination(val page: Int, val xPoints: Float, val yPoints: Float, val zoom: Float)
internal fun resolvePdfDestinationPage(pageNumber: Int, pageCount: Int): Int? = pageNumber.takeIf { it in 0 until pageCount }

class AndroidPdfPageSource(private val resolver: ContentResolver) : PdfPageSource {
    override fun open(uri: String): PdfSession {
        val descriptor = resolver.openFileDescriptor(Uri.parse(uri), "r") ?: error("Document is unavailable")
        return try { AndroidPdfSession(resolver, Uri.parse(uri), descriptor, PdfRenderer(descriptor)) } catch (failure: Throwable) {
            descriptor.close()
            throw failure
        }
    }
}

private class AndroidPdfSession(
    private val resolver: ContentResolver,
    private val uri: Uri,
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer
) : PdfSession {
    private var preVDescriptor: ParcelFileDescriptor? = null
    private var preVRenderer: android.graphics.pdf.PdfRendererPreV? = openPreVSelectionRenderer()
    override val pageCount: Int get() = renderer.pageCount
    override val supportsTextSelection: Boolean get() = Build.VERSION.SDK_INT >= 35 || preVRenderer != null

    private fun openPreVSelectionRenderer(): android.graphics.pdf.PdfRendererPreV? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || Build.VERSION.SDK_INT >= 35 ||
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) < 13) return null
        return runCatching {
            val extra = resolver.openFileDescriptor(uri, "r") ?: return null
            preVDescriptor = extra
            android.graphics.pdf.PdfRendererPreV(extra)
        }.getOrElse {
            runCatching { preVDescriptor?.close() }
            preVDescriptor = null
            null
        }
    }

    @Synchronized
    override fun pageSize(pageIndex: Int): PdfPageSize {
        require(pageIndex in 0 until pageCount)
        val page = renderer.openPage(pageIndex)
        return try { PdfPageSize(page.width, page.height) } finally { page.close() }
    }

    @Synchronized
    override fun selectText(pageIndex: Int, startX: Float, startY: Float, stopX: Float, stopY: Float): PdfTextSelection? {
        if (!supportsTextSelection || pageIndex !in 0 until pageCount) return null
        val size = pageSize(pageIndex)
        val start = SelectionBoundary(Point((startX.coerceIn(0f, 1f) * size.width).toInt(), (startY.coerceIn(0f, 1f) * size.height).toInt()))
        val stop = SelectionBoundary(Point((stopX.coerceIn(0f, 1f) * size.width).toInt(), (stopY.coerceIn(0f, 1f) * size.height).toInt()))
        val text: String
        val bounds: List<PdfSelectionBounds>
        if (Build.VERSION.SDK_INT >= 35) {
            val page = renderer.openPage(pageIndex)
            try {
                val contents = page.selectContent(start, stop)?.selectedTextContents.orEmpty()
                text = contents.joinToString("") { it.text }.trim()
                bounds = contents.flatMap { it.bounds }.map { PdfSelectionBounds(it.left / size.width, it.top / size.height, it.right / size.width, it.bottom / size.height) }
            } finally { page.close() }
        } else {
            val page = preVRenderer?.openPage(pageIndex) ?: return null
            try {
                val contents = page.selectContent(start, stop)?.selectedTextContents.orEmpty()
                text = contents.joinToString("") { it.text }.trim()
                bounds = contents.flatMap { it.bounds }.map { PdfSelectionBounds(it.left / size.width, it.top / size.height, it.right / size.width, it.bottom / size.height) }
            } finally { page.close() }
        }
        return if (text.isNotBlank() && bounds.isNotEmpty()) PdfTextSelection(pageIndex, text, bounds) else null
    }

    @Synchronized
    override fun internalDestination(pageIndex: Int, x: Float, y: Float): PdfInternalDestination? {
        if (!supportsTextSelection || pageIndex !in 0 until pageCount) return null
        val size = pageSize(pageIndex)
        val px = x.coerceIn(0f, 1f) * size.width
        val py = y.coerceIn(0f, 1f) * size.height
        val target = if (Build.VERSION.SDK_INT >= 35) {
            val page = renderer.openPage(pageIndex)
            try { page.gotoLinks.firstOrNull { link -> link.bounds.any { it.contains(px, py) } }?.destination }
            finally { page.close() }
        } else {
            val page = preVRenderer?.openPage(pageIndex) ?: return null
            try { page.gotoLinks.firstOrNull { link -> link.bounds.any { it.contains(px, py) } }?.destination }
            finally { page.close() }
        }
        val pageNo = target?.pageNumber?.let { resolvePdfDestinationPage(it, pageCount) } ?: return null
        return PdfInternalDestination(pageNo, target.xCoordinate, target.yCoordinate, target.zoom)
    }

    @Synchronized
    override fun render(pageIndex: Int, targetWidth: Int): Bitmap {
        require(pageIndex in 0 until renderer.pageCount)
        val page = renderer.openPage(pageIndex)
        try {
            var width = targetWidth.coerceIn(320, 2400).toFloat()
            var height = width * page.height / page.width.coerceAtLeast(1)
            val pixels = width * height
            val maxPixels = 4_000_000f
            if (pixels > maxPixels) {
                val scale = kotlin.math.sqrt(maxPixels / pixels)
                width *= scale
                height *= scale
            }
            val bitmap = Bitmap.createBitmap(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, Rect(0, 0, bitmap.width, bitmap.height), null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } finally { page.close() }
    }

    @Synchronized
    override fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
        runCatching { preVRenderer?.close() }
        runCatching { preVDescriptor?.close() }
    }
}
