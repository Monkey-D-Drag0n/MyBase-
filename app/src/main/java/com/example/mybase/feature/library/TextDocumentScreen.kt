package com.example.mybase.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.ui.theme.MyBaseColors

@Composable
fun TextDocumentScreen(state: TextDocumentUiState, onBack: () -> Unit, onRaw: (Boolean) -> Unit, onOpenLink: (String) -> Unit, onOpenExternal: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val chunks = remember(state.text) { state.text.chunked(4_000) }
    val markdownBlocks = state.markdownBlocks
    Column(Modifier.fillMaxSize().background(MyBaseColors.Cream)) {
        Row(Modifier.fillMaxWidth().background(MyBaseColors.Sidebar).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Library", color = MyBaseColors.RoseText) }
            Text(state.title.ifBlank { "Document" }, Modifier.weight(1f), color = MyBaseColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (state.kind == DocumentPreviewKind.Markdown) {
                TextButton(onClick = { onRaw(!state.showRaw) }) { Text(if (state.showRaw) "Preview" else "Raw", color = MyBaseColors.LavenderDark) }
            }
            TextButton(onClick = { onOpenExternal(state.uri) }) { Text("Open file", color = MyBaseColors.Muted) }
        }
        if (state.truncated) Text("Preview limited to the first 300,000 characters.", Modifier.fillMaxWidth().background(MyBaseColors.Blush).padding(9.dp), color = MyBaseColors.RoseText, fontSize = 12.sp)
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MyBaseColors.MintDark) }
            state.error != null -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("This document is unavailable", color = MyBaseColors.Ink, fontSize = 19.sp)
                Text(state.error, Modifier.padding(top = 6.dp), color = MyBaseColors.Muted)
                TextButton(onClick = { onOpenExternal(state.uri) }) { Text("Try opening externally") }
            }
            state.text.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("This document is empty.", color = MyBaseColors.Muted) }
            state.kind == DocumentPreviewKind.PlainText || state.showRaw -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(chunks) { chunk ->
                    SelectionContainer { Text(chunk, color = MyBaseColors.Ink, fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 21.sp) }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(markdownBlocks) { block -> MarkdownBlockView(block, onOpenLink) }
            }
        }
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock, onOpenLink: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        when (block.type) {
            MarkdownBlockType.Heading -> SelectionContainer { MarkdownInlineText(block.text, Modifier.padding(top = if (block.level <= 2) 9.dp else 3.dp), fontSize = when (block.level) { 1 -> 28; 2 -> 24; 3 -> 20; else -> 17 }.sp, color = MyBaseColors.Ink, fontWeight = FontWeight.Bold) }
            MarkdownBlockType.Paragraph -> SelectionContainer { MarkdownInlineText(block.text, fontSize = 15.sp, lineHeight = 23.sp, color = MyBaseColors.Ink) }
            MarkdownBlockType.Quote -> Surface(color = MyBaseColors.Lavender, shape = RoundedCornerShape(4.dp, 14.dp, 14.dp, 4.dp)) {
                SelectionContainer { MarkdownInlineText(block.text, Modifier.padding(12.dp), fontSize = 14.sp, lineHeight = 21.sp, color = MyBaseColors.LavenderDark, fontStyle = FontStyle.Italic) }
            }
            MarkdownBlockType.Code -> Surface(color = MyBaseColors.Sidebar, shape = RoundedCornerShape(12.dp)) {
                SelectionContainer { Text(block.text, Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp), fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp, color = MyBaseColors.Ink) }
            }
            MarkdownBlockType.BulletList, MarkdownBlockType.OrderedList -> block.items.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(if (block.type == MarkdownBlockType.BulletList) "•" else "${index + 1}.", Modifier.width(28.dp), color = MyBaseColors.MintDark)
                    SelectionContainer { MarkdownInlineText(item, Modifier.weight(1f), fontSize = 15.sp, lineHeight = 22.sp, color = MyBaseColors.Ink) }
                }
            }
            MarkdownBlockType.Rule -> HorizontalDivider(color = MyBaseColors.Lavender)
            MarkdownBlockType.Table -> Column(Modifier.horizontalScroll(rememberScrollState()).background(MyBaseColors.White, RoundedCornerShape(11.dp)).padding(8.dp)) {
                block.table.forEachIndexed { rowIndex, row ->
                    Row {
                        row.forEach { cell ->
                            SelectionContainer {
                                MarkdownInlineText(cell, Modifier.widthIn(min = 100.dp, max = 230.dp).padding(8.dp), fontSize = 13.sp, color = MyBaseColors.Ink, fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    if (rowIndex == 0) HorizontalDivider(color = MyBaseColors.Lavender)
                }
            }
        }
        block.links.distinct().filter(LibraryViewModel::isValidWebUrl).forEach { url ->
            TextButton(onClick = { onOpenLink(url) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("Open link · ${url.removePrefix("https://").removePrefix("http://")}", color = MyBaseColors.LavenderDark, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun MarkdownInlineText(
    source: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = 21.sp,
    color: Color = MyBaseColors.Ink,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal
) {
    val regex = remember { Regex("\\*\\*(.+?)\\*\\*|__(.+?)__|\\*(.+?)\\*|_([^_]+)_|`([^`]+)`|\\[([^]]+)]\\((https?://[^)]+)\\)", RegexOption.IGNORE_CASE) }
    val annotated = remember(source, regex) {
        buildAnnotatedString {
            var cursor = 0
            regex.findAll(source).forEach { match ->
                append(source.substring(cursor, match.range.first))
                val (style, visible) = when {
                    match.groups[1] != null -> SpanStyle(fontWeight = FontWeight.Bold) to match.groupValues[1]
                    match.groups[2] != null -> SpanStyle(fontWeight = FontWeight.Bold) to match.groupValues[2]
                    match.groups[3] != null -> SpanStyle(fontStyle = FontStyle.Italic) to match.groupValues[3]
                    match.groups[4] != null -> SpanStyle(fontStyle = FontStyle.Italic) to match.groupValues[4]
                    match.groups[5] != null -> SpanStyle(fontFamily = FontFamily.Monospace, background = MyBaseColors.Sidebar) to match.groupValues[5]
                    else -> SpanStyle(color = MyBaseColors.LavenderDark, textDecoration = TextDecoration.Underline) to match.groupValues[6]
                }
                pushStyle(style); append(visible); pop(); cursor = match.range.last + 1
            }
            append(source.substring(cursor))
        }
    }
    Text(annotated, modifier, color = color, fontSize = fontSize, lineHeight = lineHeight, fontWeight = fontWeight, fontStyle = fontStyle)
}
