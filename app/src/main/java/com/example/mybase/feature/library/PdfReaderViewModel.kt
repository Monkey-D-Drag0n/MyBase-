package com.example.mybase.feature.library

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybase.data.document.PdfPageSource
import com.example.mybase.data.document.PdfPageSize
import com.example.mybase.data.document.PdfInternalDestination
import com.example.mybase.data.document.PdfSession
import com.example.mybase.data.document.PdfTextSelection
import com.example.mybase.data.repository.DocumentReadingStateRepository
import com.example.mybase.core.model.PdfHighlight
import com.example.mybase.core.model.PdfTextHighlight
import com.example.mybase.data.repository.PdfHighlightRepository
import com.example.mybase.data.repository.PdfTextHighlightRepository
import com.example.mybase.core.model.PdfTextHighlightBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class PdfReaderUiState(
    val documentId: String = "",
    val uri: String = "",
    val pageCount: Int = 0,
    val page: Int = 0,
    val pageSizes: List<PdfPageSize> = emptyList(),
    val pageBitmaps: Map<Int, Bitmap> = emptyMap(),
    val thumbnailBitmaps: Map<Int, Bitmap> = emptyMap(),
    val pageErrors: Map<Int, String> = emptyMap(),
    val bookmarks: List<Int> = emptyList(),
    val highlights: List<PdfHighlight> = emptyList(),
    val textHighlights: List<PdfTextHighlight> = emptyList(),
    val textSelection: PdfTextSelection? = null,
    val supportsTextSelection: Boolean = false,
    val selectionMessage: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val showBookmarks: Boolean = false,
    val showHighlights: Boolean = false,
    val showThumbnails: Boolean = true
)

internal fun PdfReaderUiState.toggleThumbnails(): PdfReaderUiState = copy(showThumbnails = !showThumbnails)

internal fun PdfReaderUiState.withTextSelection(selection: PdfTextSelection?): PdfReaderUiState = copy(
    textSelection = selection,
    selectionMessage = if (selection == null) "No selectable text was found in that selection." else null
)

class PdfReaderViewModel(
    private val pageSource: PdfPageSource,
    private val readingStateRepository: DocumentReadingStateRepository,
    private val highlightRepository: PdfHighlightRepository,
    private val textHighlightRepository: PdfTextHighlightRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(PdfReaderUiState())
    val uiState = mutableState.asStateFlow()
    private var session: PdfSession? = null
    private var openToken = 0
    private val pageCache = LinkedHashMap<Int, Bitmap>(8, .75f, true)
    private val thumbnailCache = LinkedHashMap<Int, Bitmap>(16, .75f, true)
    private val inFlight = mutableSetOf<String>()

    fun open(documentId: String, uri: String) {
        releaseBitmaps()
        closeSession()
        val token = ++openToken
        mutableState.value = PdfReaderUiState(documentId = documentId, uri = uri, loading = true)
        viewModelScope.launch {
            val opened = runCatching { withContext(Dispatchers.IO) { pageSource.open(uri) } }.getOrElse { failure ->
                if (token == openToken) mutableState.update { it.copy(loading = false, error = failure.message ?: "This PDF could not be opened.") }
                return@launch
            }
            if (token != openToken) { withContext(Dispatchers.IO) { opened.close() }; return@launch }
            session = opened
            if (opened.pageCount <= 0) {
                closeSession()
                mutableState.update { it.copy(loading = false, error = "This PDF has no readable pages.") }
                return@launch
            }
            val sizes = runCatching { withContext(Dispatchers.IO) { (0 until opened.pageCount).map(opened::pageSize) } }.getOrElse { failure ->
                withContext(Dispatchers.IO) { opened.close() }
                if (session === opened) session = null
                if (token == openToken) mutableState.update { it.copy(loading = false, error = failure.message ?: "PDF page information could not be read.") }
                return@launch
            }
            val saved = readingStateRepository.stateFor(documentId)
            val page = saved.lastPage.coerceIn(0, opened.pageCount - 1)
            mutableState.update { it.copy(pageCount = opened.pageCount, page = page, pageSizes = sizes, bookmarks = saved.bookmarks.filter { bookmark -> bookmark in 0 until opened.pageCount }, highlights = highlightRepository.forDocument(documentId), textHighlights = textHighlightRepository.forDocument(documentId), supportsTextSelection = opened.supportsTextSelection, loading = false) }
        }
    }

    fun goToPage(page: Int) {
        val state = mutableState.value
        if (state.pageCount <= 0) return
        val target = page.coerceIn(0, state.pageCount - 1)
        readingStateRepository.savePage(state.documentId, target)
        mutableState.update { it.copy(page = target, textSelection = null, selectionMessage = null, highlights = highlightRepository.forDocument(state.documentId), textHighlights = textHighlightRepository.forDocument(state.documentId), error = null) }
    }
    /** Persists the page most visible in the continuous reading viewport. */
    fun onViewportPageChanged(page: Int) {
        val state = mutableState.value
        if (state.documentId.isBlank() || state.pageCount <= 0) return
        val target = page.coerceIn(0, state.pageCount - 1)
        if (target == state.page) return
        readingStateRepository.savePage(state.documentId, target)
        mutableState.update { it.copy(page = target, textSelection = null, selectionMessage = null, highlights = highlightRepository.forDocument(state.documentId), textHighlights = textHighlightRepository.forDocument(state.documentId)) }
    }
    fun ensurePageRendered(page: Int, widthPx: Int) = requestBitmap(page, widthPx, thumbnail = false)
    fun ensureThumbnailRendered(page: Int) = requestBitmap(page, 260, thumbnail = true)
    fun nextPage() = goToPage(mutableState.value.page + 1)
    fun previousPage() = goToPage(mutableState.value.page - 1)
    fun addBookmark() {
        val state = mutableState.value
        if (state.pageCount > 0) {
            readingStateRepository.addBookmark(state.documentId, state.page)
            mutableState.update { it.copy(bookmarks = readingStateRepository.stateFor(state.documentId).bookmarks) }
        }
    }
    fun removeBookmark(page: Int) {
        val id = mutableState.value.documentId
        readingStateRepository.removeBookmark(id, page)
        mutableState.update { it.copy(bookmarks = readingStateRepository.stateFor(id).bookmarks) }
    }
    fun showBookmarks(show: Boolean) = mutableState.update { it.copy(showBookmarks = show) }
    fun addHighlight(page: Int, left: Float, top: Float, right: Float, bottom: Float) {
        val state = mutableState.value
        if (page !in 0 until state.pageCount) return
        val highlight = PdfHighlight(UUID.randomUUID().toString(), state.documentId, page, left.coerceIn(0f, 1f), top.coerceIn(0f, 1f), right.coerceIn(0f, 1f), bottom.coerceIn(0f, 1f))
        if (!highlight.isValid()) return
        highlightRepository.add(highlight)
        mutableState.update { it.copy(highlights = highlightRepository.forDocument(state.documentId)) }
    }
    fun removeHighlight(id: String) {
        highlightRepository.remove(id)
        val state = mutableState.value
        mutableState.update { it.copy(highlights = highlightRepository.forDocument(state.documentId)) }
    }
    fun addTextHighlight(page: Int, text: String, bounds: List<PdfTextHighlightBounds>) {
        val state = mutableState.value
        val highlight = PdfTextHighlight(UUID.randomUUID().toString(), state.documentId, page, text, bounds)
        if (!highlight.isValid()) return
        textHighlightRepository.add(highlight)
        mutableState.update { it.copy(textHighlights = textHighlightRepository.forDocument(state.documentId)) }
    }
    fun selectText(page: Int, startX: Float, startY: Float, stopX: Float, stopY: Float) {
        val snapshot = mutableState.value
        if (!snapshot.supportsTextSelection || page !in 0 until snapshot.pageCount) return
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { session?.selectText(page, startX, startY, stopX, stopY) } }.getOrNull()
            if (mutableState.value.documentId == snapshot.documentId && mutableState.value.page == page) mutableState.update { it.withTextSelection(result) }
        }
    }
    fun clearTextSelection() = mutableState.update { it.copy(textSelection = null) }
    fun saveSelectedText() {
        val selection = mutableState.value.textSelection ?: return
        textHighlightRepository.add(selection.toHighlight(mutableState.value.documentId))
        val documentId = mutableState.value.documentId
        mutableState.update { it.copy(textHighlights = textHighlightRepository.forDocument(documentId)) }
        clearTextSelection()
    }
    fun removeTextHighlight(id: String) {
        textHighlightRepository.remove(id)
        val documentId = mutableState.value.documentId
        mutableState.update { it.copy(textHighlights = textHighlightRepository.forDocument(documentId)) }
    }
    fun showHighlights(show: Boolean) = mutableState.update { it.copy(showHighlights = show) }
    fun toggleThumbnails() = mutableState.update(PdfReaderUiState::toggleThumbnails)

    fun resolveInternalDestination(page: Int, x: Float, y: Float, onResolved: (PdfInternalDestination?) -> Unit) {
        val documentId = mutableState.value.documentId
        viewModelScope.launch {
            val destination = runCatching { withContext(Dispatchers.IO) { session?.internalDestination(page, x, y) } }.getOrNull()
            if (mutableState.value.documentId == documentId && mutableState.value.page == page) onResolved(destination)
        }
    }

    private fun requestBitmap(page: Int, width: Int, thumbnail: Boolean) {
        val state = mutableState.value
        if (page !in 0 until state.pageCount || width <= 0) return
        val cache = if (thumbnail) thumbnailCache else pageCache
        val map = if (thumbnail) state.thumbnailBitmaps else state.pageBitmaps
        if (cache[page]?.isRecycled == false || page in map) return
        val key = "${if (thumbnail) "t" else "p"}:$page"
        if (!inFlight.add(key)) return
        val token = openToken
        viewModelScope.launch {
            val bitmap = runCatching { withContext(Dispatchers.IO) { session?.render(page, width) ?: error("Reader is closed") } }.getOrElse { failure ->
                inFlight.remove(key)
                if (token == openToken) mutableState.update { current -> current.copy(pageErrors = current.pageErrors + (page to (failure.message ?: "This PDF page could not be rendered."))) }
                return@launch
            }
            inFlight.remove(key)
            if (token != openToken) { bitmap.recycle(); return@launch }
            cache[page]?.takeIf { it !== bitmap }?.let { if (!it.isRecycled) it.recycle() }
            cache[page] = bitmap
            val maxEntries = if (thumbnail) 12 else 4
            while (cache.size > maxEntries) {
                val eldest = cache.entries.iterator().next()
                cache.remove(eldest.key)
                if (!eldest.value.isRecycled) eldest.value.recycle()
            }
            mutableState.update { current -> current.copy(
                pageBitmaps = if (thumbnail) current.pageBitmaps else pageCache.toMap(),
                thumbnailBitmaps = if (thumbnail) thumbnailCache.toMap() else current.thumbnailBitmaps,
                pageErrors = current.pageErrors - page
            ) }
        }
    }

    fun closeReader() {
        openToken++
        mutableState.value = PdfReaderUiState()
        releaseBitmaps()
        closeSession()
    }

    private fun closeSession() {
        val old = session
        session = null
        if (old != null) runCatching { old.close() }
    }

    private fun releaseBitmaps() {
        (pageCache.values + thumbnailCache.values).distinct().forEach { if (!it.isRecycled) it.recycle() }
        pageCache.clear(); thumbnailCache.clear(); inFlight.clear()
    }

    override fun onCleared() { closeReader() }
}

class PdfReaderViewModelFactory(private val pageSource: PdfPageSource, private val stateRepository: DocumentReadingStateRepository, private val highlightRepository: PdfHighlightRepository, private val textHighlightRepository: PdfTextHighlightRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PdfReaderViewModel(pageSource, stateRepository, highlightRepository, textHighlightRepository) as T
}

internal fun PdfTextSelection.toHighlight(documentId: String, id: String = UUID.randomUUID().toString()): PdfTextHighlight =
    PdfTextHighlight(id, documentId, page, text, bounds.map { PdfTextHighlightBounds(it.left, it.top, it.right, it.bottom) })

internal fun textForPdfClipboard(selection: PdfTextSelection?): String? = selection?.text?.takeIf(String::isNotBlank)
