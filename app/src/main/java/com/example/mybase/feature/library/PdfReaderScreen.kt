package com.example.mybase.feature.library

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.core.model.PdfHighlight
import com.example.mybase.core.model.PdfTextHighlight
import com.example.mybase.data.document.PdfPageSize
import com.example.mybase.data.document.PdfInternalDestination
import com.example.mybase.ui.theme.MyBaseColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun PdfReaderScreen(
    state: PdfReaderUiState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBookmark: () -> Unit,
    onShowBookmarks: (Boolean) -> Unit,
    onJumpTo: (Int) -> Unit,
    onRemoveBookmark: (Int) -> Unit,
    onAddHighlight: (Int, Float, Float, Float, Float) -> Unit,
    onRemoveHighlight: (String) -> Unit,
    onShowHighlights: (Boolean) -> Unit,
    onOpenExternal: (String) -> Unit,
    onSelectText: (Int, Float, Float, Float, Float) -> Unit,
    onSaveSelectedText: () -> Unit,
    onClearTextSelection: () -> Unit,
    onRemoveTextHighlight: (String) -> Unit,
    onNavigateInternal: (Int, Float, Float, (PdfInternalDestination?) -> Unit) -> Unit,
    onEnsurePage: (Int, Int) -> Unit,
    onEnsureThumbnail: (Int) -> Unit,
    onVisiblePage: (Int) -> Unit,
    onToggleThumbnails: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pageTransforms = remember(state.documentId) { mutableStateMapOf<Int, PdfPageTransform>() }
    var textSelectionMode by remember(state.documentId) { mutableStateOf(false) }
    var regionMode by remember(state.documentId) { mutableStateOf(false) }
    var jumpPageText by remember(state.documentId) { mutableStateOf("") }
    var jumpError by remember(state.documentId) { mutableStateOf(false) }
    var dragStart by remember(state.documentId) { mutableStateOf<Offset?>(null) }
    var dragEnd by remember(state.documentId) { mutableStateOf<Offset?>(null) }
    var readerWidth by remember(state.documentId) { mutableStateOf(0.dp) }
    var viewportReady by remember(state.documentId) { mutableStateOf(false) }
    val currentTransform = pageTransforms[state.page] ?: PdfPageTransform()

    fun jumpToPageAt(page: Int, pageTopFraction: Float = 0f) {
        if (page !in 0 until state.pageCount) return
        onJumpTo(page)
        val size = state.pageSizes.getOrNull(page)
        val transform = pageTransforms[page] ?: PdfPageTransform()
        val width = with(density) { (readerWidth - 24.dp).coerceAtLeast(1.dp).toPx() }
        val height = if (size == null) 0f else width * size.height / size.width.coerceAtLeast(1)
        if (transform.zoom > 1f) {
            pageTransforms[page] = transform.copy(panYFraction = panFractionToRevealPageY(pageTopFraction, transform.zoom))
            scope.launch { listState.animateScrollToItem(page) }
        } else {
            val targetY = pageTopFraction.coerceIn(0f, 1f) * height
            scope.launch { listState.animateScrollToItem(page, targetY.toInt().coerceAtLeast(0)) }
        }
    }

    fun jumpToPage(page: Int) = jumpToPageAt(page)

    fun jumpToDestination(destination: PdfInternalDestination) {
        if (destination.page !in 0 until state.pageCount) return
        onJumpTo(destination.page)
        if (destination.zoom > 0f) pageTransforms[destination.page] = (pageTransforms[destination.page] ?: PdfPageTransform()).copy(zoom = destination.zoom.coerceIn(1f, 4f))
        val pageSize = state.pageSizes.getOrNull(destination.page)
        val yFraction = pageSize?.height?.takeIf { it > 0 }?.let { destination.yPoints / it } ?: 0f
        jumpToPageAt(destination.page, yFraction)
    }

    fun changeZoom(factor: Float) {
        val transform = pageTransforms[state.page] ?: PdfPageTransform()
        pageTransforms[state.page] = transform.zoomBy(factor)
    }

    LaunchedEffect(state.documentId, state.pageCount) {
        if (state.pageCount > 0) {
            listState.scrollToItem(state.page.coerceIn(0, state.pageCount - 1))
            viewportReady = true
        }
    }
    LaunchedEffect(listState, state.documentId) {
        snapshotFlow {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
        }.distinctUntilChanged().collect { if (viewportReady) it?.let(onVisiblePage) }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MyBaseColors.Cream)) {
        val wide = maxWidth >= 850.dp
        val availableReaderWidth = maxWidth - if (wide && state.showThumbnails) 132.dp else 0.dp
        SideEffect { readerWidth = availableReaderWidth }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(MyBaseColors.Sidebar).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹ Library", color = MyBaseColors.RoseText) }
                Text("PDF reader", Modifier.weight(1f), color = MyBaseColors.Ink, fontSize = 17.sp)
                IconButton(onClick = onToggleThumbnails) {
                    Text("|||", Modifier.semantics { contentDescription = if (state.showThumbnails) "Hide page thumbnails" else "Show page thumbnails" }, color = MyBaseColors.LavenderDark)
                }
                Text(if (state.showThumbnails) "Pages shown" else "Pages hidden", color = MyBaseColors.Muted, fontSize = 11.sp)
                TextButton(onClick = { onOpenExternal(state.uri) }) { Text("Open externally", color = MyBaseColors.LavenderDark) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { jumpToPage(state.page - 1) }, enabled = state.page > 0) { Text("‹ Previous") }
                Text("${if (state.pageCount == 0) 0 else state.page + 1} / ${state.pageCount}", Modifier.padding(horizontal = 8.dp), color = MyBaseColors.Ink)
                OutlinedTextField(
                    value = jumpPageText, onValueChange = { jumpPageText = it.filter(Char::isDigit); jumpError = false },
                    modifier = Modifier.width(92.dp), singleLine = true, label = { Text("Page #") }, isError = jumpError
                )
                TextButton(onClick = {
                    val entered = jumpPageText.toIntOrNull()
                    if (entered == null || entered !in 1..state.pageCount) jumpError = true
                    else { jumpToPage(entered - 1); jumpPageText = "" }
                }, enabled = state.pageCount > 0) { Text("Go") }
                TextButton(onClick = onBookmark, enabled = state.pageCount > 0) { Text("☆ Bookmark") }
                TextButton(onClick = { onShowBookmarks(true) }) { Text("Saved (${state.bookmarks.size})") }
                IconButton(onClick = { changeZoom(.8f) }, enabled = currentTransform.zoom > 1f) { Text("−", Modifier.semantics { contentDescription = "Zoom out" }) }
                Text("${(currentTransform.zoom * 100).toInt()}%", color = MyBaseColors.Ink, fontSize = 12.sp)
                IconButton(onClick = { changeZoom(1.25f) }) { Text("+", Modifier.semantics { contentDescription = "Zoom in" }) }
                TextButton(onClick = { pageTransforms.clear() }) { Text("Fit") }
                FilterChip(selected = regionMode, onClick = { regionMode = !regionMode; if (regionMode) textSelectionMode = false }, label = { Text("Region") })
                if (state.supportsTextSelection) FilterChip(selected = textSelectionMode, onClick = { textSelectionMode = !textSelectionMode; if (textSelectionMode) regionMode = false }, label = { Text("Select text") })
                else Text("Text selection and PDF links need Android 15 or Android 12+ PDF extension 13", Modifier.padding(horizontal = 7.dp), color = MyBaseColors.Muted, fontSize = 10.sp)
                TextButton(onClick = { onShowHighlights(true) }) { Text("Annotations (${state.highlights.size + state.textHighlights.size})") }
                TextButton(onClick = { jumpToPage(state.page + 1) }, enabled = state.page < state.pageCount - 1) { Text("Next ›") }
            }
            if (regionMode) Text("Region mark · drag an area; this legacy annotation is not selected text.", Modifier.padding(horizontal = 16.dp, vertical = 2.dp), color = MyBaseColors.RoseText, fontSize = 12.sp)
            if (textSelectionMode) Text("Text selection · drag from the start to end of the text you want.", Modifier.padding(horizontal = 16.dp, vertical = 2.dp), color = MyBaseColors.LavenderDark, fontSize = 12.sp)
            state.selectionMessage?.let { Text(it, Modifier.padding(horizontal = 16.dp, vertical = 2.dp), color = MyBaseColors.Muted, fontSize = 12.sp) }
            state.textSelection?.let { selected ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(selected.text, Modifier.weight(1f), maxLines = 2, color = MyBaseColors.Ink, fontSize = 13.sp)
                    TextButton(onClick = {
                        val text = textForPdfClipboard(selected)
                        if (text != null) context.getSystemService(ClipboardManager::class.java)?.let { manager ->
                            manager.setPrimaryClip(ClipData.newPlainText("PDF text", text))
                            Toast.makeText(context, "Selected PDF text copied", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Copy") }
                    TextButton(onClick = { onSaveSelectedText(); textSelectionMode = false }) { Text("Highlight") }
                    TextButton(onClick = onClearTextSelection) { Text("Clear") }
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (wide && state.showThumbnails) PageThumbnailRail(state, onEnsureThumbnail, ::jumpToPage, Modifier.width(132.dp).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    if (!wide && state.showThumbnails) PageThumbnailStrip(state, onEnsureThumbnail, ::jumpToPage)
                    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MyBaseColors.MintDark)
                    if (state.error != null && state.pageCount == 0) {
                        ReaderMessage("This PDF could not be opened", state.error, onOpenExternal, state.uri)
                    } else if (state.pageCount == 0) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MyBaseColors.MintDark) }
                    } else {
                        LazyColumn(
                            state = listState, modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed((0 until state.pageCount).toList(), key = { index, _ -> "${state.documentId}:$index" }) { index, _ ->
                                val pageInfo = state.pageSizes.getOrNull(index) ?: PdfPageSize(612, 792)
                                val pageWidth = (availableReaderWidth - 24.dp).coerceAtLeast(1.dp)
                                val pageHeight = pageWidth * pageInfo.height.toFloat() / pageInfo.width.coerceAtLeast(1)
                                val pxWidth = with(density) { pageWidth.roundToPx() }.coerceIn(320, 2400)
                                val pageWidthPx = with(density) { pageWidth.toPx() }
                                val pageHeightPx = with(density) { pageHeight.toPx() }
                                val transform = pageTransforms[index] ?: PdfPageTransform()
                                val latestTransform by rememberUpdatedState(transform)
                                val pageTransformState = rememberTransformableState { zoomChange, panChange, _ ->
                                    val current = pageTransforms[index] ?: PdfPageTransform()
                                    val scaled = current.zoomBy(zoomChange)
                                    pageTransforms[index] = scaled.panBy(panChange, pageWidthPx, pageHeightPx)
                                }
                                LaunchedEffect(state.documentId, index, pxWidth) { onEnsurePage(index, pxWidth) }
                                val bitmap = state.pageBitmaps[index]
                                Box(Modifier.fillMaxWidth().height(pageHeight).clip(RoundedCornerShape(10.dp)).background(MyBaseColors.White), contentAlignment = Alignment.TopCenter) {
                                    Box(Modifier.width(pageWidth).height(pageHeight)
                                        .transformable(pageTransformState, enabled = !textSelectionMode && !regionMode, canPan = { transform.zoom > 1.001f })
                                        .pointerInput(state.documentId, index, textSelectionMode, regionMode) {
                                            if (textSelectionMode) detectDragGestures(
                                                onDragStart = { point -> dragStart = pagePointFromViewport(point, size.width.toFloat(), size.height.toFloat(), latestTransform).asOffset(); dragEnd = dragStart },
                                                onDrag = { change, _ -> dragEnd = pagePointFromViewport(change.position, size.width.toFloat(), size.height.toFloat(), latestTransform).asOffset(); change.consume() },
                                                onDragEnd = {
                                                    val start = dragStart; val end = dragEnd
                                                    if (start != null && end != null && size.width > 0 && size.height > 0) onSelectText(index,
                                                        start.x, start.y, end.x, end.y)
                                                    dragStart = null; dragEnd = null
                                                }, onDragCancel = { dragStart = null; dragEnd = null }
                                            ) else if (regionMode) detectDragGestures(
                                                onDragStart = { point -> dragStart = pagePointFromViewport(point, size.width.toFloat(), size.height.toFloat(), latestTransform).asOffset(); dragEnd = dragStart },
                                                onDrag = { change, _ -> dragEnd = pagePointFromViewport(change.position, size.width.toFloat(), size.height.toFloat(), latestTransform).asOffset(); change.consume() },
                                                onDragEnd = {
                                                    val a = dragStart; val b = dragEnd
                                                    if (a != null && b != null) onAddHighlight(index,
                                                        minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y))
                                                    dragStart = null; dragEnd = null
                                                }, onDragCancel = { dragStart = null; dragEnd = null }
                                            ) else awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                val up = waitForUpOrCancellation()
                                                if (up != null && abs(up.position.x - down.position.x) <= viewConfiguration.touchSlop && abs(up.position.y - down.position.y) <= viewConfiguration.touchSlop) {
                                                    val target = pagePointFromViewport(up.position, size.width.toFloat(), size.height.toFloat(), latestTransform)
                                                    onNavigateInternal(index, target.x, target.y) { destination -> destination?.let(::jumpToDestination) }
                                                }
                                            }
                                        }
                                    ) {
                                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.White).graphicsLayer {
                                            scaleX = transform.zoom; scaleY = transform.zoom
                                            translationX = transform.translationX(size.width); translationY = transform.translationY(size.height)
                                        }) {
                                            if (bitmap != null && !bitmap.isRecycled) Image(bitmap.asImageBitmap(), "PDF page ${index + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                                            else state.pageErrors[index]?.let { Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Page ${index + 1} unavailable", color = MyBaseColors.Muted)
                                                TextButton(onClick = { onOpenExternal(state.uri) }) { Text("Open externally") }
                                            } } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 2.dp, color = MyBaseColors.MintDark) }
                                            Canvas(Modifier.fillMaxSize()) {
                                                drawPageMarks(state.highlights.filter { it.page == index }, state.textHighlights.filter { it.page == index }, index, state.textSelection, pageInfo)
                                                if ((regionMode || textSelectionMode) && dragStart != null && dragEnd != null) {
                                                    val a = dragStart!!; val b = dragEnd!!
                                                    drawRect(Color(0x667B61B8), Offset(minOf(a.x, b.x) * this.size.width, minOf(a.y, b.y) * this.size.height), androidx.compose.ui.geometry.Size(abs(b.x - a.x) * this.size.width, abs(b.y - a.y) * this.size.height))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showBookmarks) AlertDialog(onDismissRequest = { onShowBookmarks(false) }, containerColor = MyBaseColors.White,
        title = { Text("Saved pages", color = MyBaseColors.Ink) },
        text = { if (state.bookmarks.isEmpty()) Text("Bookmark a page to find it here.", color = MyBaseColors.Muted) else LazyColumn(Modifier.heightIn(max = 360.dp)) {
            itemsIndexed(state.bookmarks.sorted()) { _, page -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { jumpToPage(page); onShowBookmarks(false) }, modifier = Modifier.weight(1f)) { Text("Page ${page + 1}") }
                TextButton(onClick = { onRemoveBookmark(page) }) { Text("Remove", color = MyBaseColors.RoseText) }
            } }
        } }, confirmButton = { TextButton(onClick = { onShowBookmarks(false) }) { Text("Done") } })

    if (state.showHighlights) AlertDialog(onDismissRequest = { onShowHighlights(false) }, containerColor = MyBaseColors.White,
        title = { Text("Annotations", color = MyBaseColors.Ink) },
        text = { if (state.highlights.isEmpty() && state.textHighlights.isEmpty()) Text("Select PDF text or mark a region.", color = MyBaseColors.Muted) else LazyColumn(Modifier.heightIn(max = 420.dp)) {
            item { Text("Text Highlights", Modifier.padding(vertical = 8.dp), color = MyBaseColors.LavenderDark, fontSize = 14.sp) }
            if (state.textHighlights.isEmpty()) item { Text("No text highlights yet.", Modifier.padding(bottom = 6.dp), color = MyBaseColors.Muted, fontSize = 12.sp) }
            itemsIndexed(state.textHighlights.sortedWith(compareBy({ it.page }, { it.id })), key = { _, h -> "text:${h.id}" }) { _, mark -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val bounds = normalizedTextHighlightBounds(mark.bounds, state.pageSizes.getOrNull(mark.page) ?: PdfPageSize(612, 792))
                TextButton(onClick = { jumpToPageAt(mark.page, bounds.firstOrNull()?.top ?: 0f); onShowHighlights(false) }, modifier = Modifier.weight(1f)) { Text("Page ${mark.page + 1} · ‘${mark.text}’", maxLines = 2) }
                TextButton(onClick = { onRemoveTextHighlight(mark.id) }) { Text("Remove", color = MyBaseColors.RoseText) }
            } }
            item { Text("Region Annotations", Modifier.padding(vertical = 8.dp), color = MyBaseColors.RoseText, fontSize = 14.sp) }
            if (state.highlights.isEmpty()) item { Text("No region annotations yet.", Modifier.padding(bottom = 6.dp), color = MyBaseColors.Muted, fontSize = 12.sp) }
            itemsIndexed(state.highlights.sortedWith(compareBy({ it.page }, { it.top })), key = { _, h -> "region:${h.id}" }) { _, mark -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { jumpToPageAt(mark.page, mark.top); onShowHighlights(false) }, modifier = Modifier.weight(1f)) { Text("Page ${mark.page + 1} · region") }
                TextButton(onClick = { onRemoveHighlight(mark.id) }) { Text("Remove", color = MyBaseColors.RoseText) }
            } }
        } }, confirmButton = { TextButton(onClick = { onShowHighlights(false) }) { Text("Done") } })
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPageMarks(
    regions: List<PdfHighlight>, texts: List<PdfTextHighlight>, page: Int,
    selection: com.example.mybase.data.document.PdfTextSelection?, pageInfo: PdfPageSize
) {
    regions.forEach { h ->
        val regionColor = if (h.colorArgb == 0x66FFE45C) 0x557B61B8 else h.colorArgb
        drawRect(Color(regionColor), Offset(size.width * h.left, size.height * h.top), androidx.compose.ui.geometry.Size(size.width * (h.right - h.left), size.height * (h.bottom - h.top)))
    }
    texts.forEach { h -> normalizedTextHighlightBounds(h.bounds, pageInfo).forEach { b -> drawRect(Color(h.colorArgb), Offset(size.width * b.left, size.height * b.top), androidx.compose.ui.geometry.Size(size.width * (b.right - b.left), size.height * (b.bottom - b.top))) } }
    selection?.takeIf { it.page == page }?.bounds?.forEach { b -> drawRect(Color(0x557B61B8), Offset(size.width * b.left, size.height * b.top), androidx.compose.ui.geometry.Size(size.width * (b.right - b.left), size.height * (b.bottom - b.top))) }
}

@Composable
private fun PageThumbnailRail(state: PdfReaderUiState, onEnsure: (Int) -> Unit, onJump: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.background(MyBaseColors.Sidebar).padding(7.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed((0 until state.pageCount).toList(), key = { index, _ -> "thumb:$index" }) { index, _ ->
            ThumbnailItem(index, state, onEnsure, onJump, Modifier.fillMaxWidth().height(116.dp))
        }
    }
}

@Composable
private fun PageThumbnailStrip(state: PdfReaderUiState, onEnsure: (Int) -> Unit, onJump: (Int) -> Unit) {
    LazyRow(Modifier.fillMaxWidth().height(118.dp).background(MyBaseColors.Sidebar).padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed((0 until state.pageCount).toList(), key = { index, _ -> "strip:$index" }) { index, _ -> ThumbnailItem(index, state, onEnsure, onJump, Modifier.width(74.dp).fillMaxHeight()) }
    }
}

@Composable
private fun ThumbnailItem(index: Int, state: PdfReaderUiState, onEnsure: (Int) -> Unit, onJump: (Int) -> Unit, modifier: Modifier) {
    LaunchedEffect(index, state.documentId) { onEnsure(index) }
    val bitmap = state.thumbnailBitmaps[index]
    Column(modifier.clickable(onClick = { onJump(index) }).background(if (state.page == index) MyBaseColors.Blush else MyBaseColors.Sidebar, RoundedCornerShape(8.dp)).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (bitmap != null && !bitmap.isRecycled) Image(bitmap.asImageBitmap(), "Thumbnail page ${index + 1}", Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Fit)
        else Box(Modifier.weight(1f).fillMaxWidth().background(MyBaseColors.White, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text("▤", color = MyBaseColors.Muted) }
        Text("${index + 1}", color = if (state.page == index) MyBaseColors.RoseText else MyBaseColors.Ink, fontSize = 11.sp)
    }
}

@Composable
private fun ReaderMessage(title: String, details: String, onOpen: (String) -> Unit, uri: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, color = MyBaseColors.Ink, fontSize = 18.sp)
        Text(details, Modifier.padding(top = 6.dp), color = MyBaseColors.Muted)
        TextButton(onClick = { onOpen(uri) }) { Text("Try opening externally") }
    }
}

