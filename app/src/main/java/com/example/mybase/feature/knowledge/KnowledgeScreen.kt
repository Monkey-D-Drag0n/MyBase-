package com.example.mybase.feature.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.core.designsystem.MomoIllustration
import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.ui.theme.MyBaseColors

@Composable
fun KnowledgeScreen(
    state: KnowledgeUiState,
    onSearch: (String) -> Unit,
    onCategory: (String?) -> Unit,
    onTag: (String?) -> Unit,
    onCreate: () -> Unit,
    onEdit: (CapturedEntry) -> Unit,
    onRequestDelete: (CapturedEntry) -> Unit,
    onDismissEditor: () -> Unit,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
    onTitle: (String) -> Unit,
    onBody: (String) -> Unit,
    onCategories: (String) -> Unit,
    onTags: (String) -> Unit,
    onSave: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MyBaseColors.Cream)) {
        val wide = maxWidth >= 700.dp
        Column(
            Modifier.fillMaxSize().padding(horizontal = if (wide) 32.dp else 18.dp)
                .widthIn(max = 1100.dp).align(Alignment.TopCenter)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = if (wide) 28.dp else 18.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Knowledge", color = MyBaseColors.Ink, fontSize = if (wide) 30.sp else 26.sp)
                    Text("A soft landing place for the things you want to remember.", color = MyBaseColors.Muted, fontSize = 13.sp)
                }
                FilledTonalButton(
                    onClick = onCreate,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MyBaseColors.Blush, contentColor = MyBaseColors.RoseText),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("＋  New note") }
            }
            OutlinedTextField(
                value = state.query, onValueChange = onSearch, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search titles and note text…") }, leadingIcon = { Text("⌕", fontSize = 22.sp, color = MyBaseColors.Muted) },
                singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MyBaseColors.White, focusedContainerColor = MyBaseColors.White,
                    unfocusedBorderColor = MyBaseColors.Lavender, focusedBorderColor = MyBaseColors.LavenderDark
                )
            )
            if (state.categories.isNotEmpty()) {
                Text("YOUR SHELVES", Modifier.padding(top = 15.dp, bottom = 4.dp), color = MyBaseColors.Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    state.categories.forEach { category ->
                        FilterChip(selected = state.selectedCategory == category, onClick = { onCategory(category) }, label = { Text(category) })
                    }
                }
            }
            if (state.tags.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    state.tags.forEach { tag ->
                        FilterChip(selected = state.selectedTag == tag, onClick = { onTag(tag) }, label = { Text("# $tag") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MyBaseColors.Mint, selectedLabelColor = MyBaseColors.MintDark))
                    }
                }
            }
            when {
                state.notes.isEmpty() -> EmptyState(
                    title = "A quiet page, waiting for your thoughts",
                    detail = "Write down a little idea. Momo will keep it safe for later.",
                    action = "Write your first note", onAction = onCreate
                )
                state.filteredNotes.isEmpty() -> EmptyState(
                    title = "Nothing on this shelf yet",
                    detail = "Try a different phrase or clear the search to see all your notes.",
                    action = if (state.query.isNotBlank()) "Clear search" else null,
                    onAction = { onSearch(""); onCategory(null); onTag(null) }
                )
                else -> {
                    Text("${state.filteredNotes.size} ${if (state.filteredNotes.size == 1) "note" else "notes"}", Modifier.padding(vertical = 8.dp), color = MyBaseColors.Muted, fontSize = 12.sp)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp), modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.filteredNotes, key = { it.id }) { note ->
                            NoteCard(note, onEdit = { onEdit(note) }, onDelete = { onRequestDelete(note) })
                        }
                    }
                }
            }
        }
    }

    if (state.isEditorVisible) {
        AlertDialog(
            onDismissRequest = onDismissEditor,
            containerColor = MyBaseColors.White,
            title = { Text(if (state.editingId == null) "A new little note" else "Tend to this note", color = MyBaseColors.Ink) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Keep the title short, or leave it blank and let your words lead.", color = MyBaseColors.Muted, fontSize = 12.sp)
                    OutlinedTextField(value = state.title, onValueChange = onTitle, modifier = Modifier.fillMaxWidth(), label = { Text("Title (optional)") }, singleLine = true)
                    OutlinedTextField(value = state.body, onValueChange = onBody, modifier = Modifier.fillMaxWidth(), label = { Text("Your thoughts") }, minLines = 5)
                    OutlinedTextField(value = state.categoriesText, onValueChange = onCategories, modifier = Modifier.fillMaxWidth(), label = { Text("Categories, separated by commas") }, singleLine = true)
                    OutlinedTextField(value = state.tagsText, onValueChange = onTags, modifier = Modifier.fillMaxWidth(), label = { Text("Tags, separated by commas") }, singleLine = true)
                    if (!state.canSave) Text("Add a title or some note text before saving.", color = MyBaseColors.RoseText, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = onSave, enabled = state.canSave) { Text("Save note", color = MyBaseColors.RoseText) } },
            dismissButton = { TextButton(onClick = onDismissEditor) { Text("Cancel", color = MyBaseColors.Muted) } }
        )
    }
    if (state.showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = onKeepEditing,
            containerColor = MyBaseColors.White,
            title = { Text("Leave this note unfinished?", color = MyBaseColors.Ink) },
            text = { Text("Your unsaved changes will be discarded.", color = MyBaseColors.Muted) },
            confirmButton = { TextButton(onClick = onDiscard) { Text("Discard changes", color = MyBaseColors.RoseText) } },
            dismissButton = { TextButton(onClick = onKeepEditing) { Text("Keep editing", color = MyBaseColors.Muted) } }
        )
    }
    if (state.pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            containerColor = MyBaseColors.White,
            title = { Text("Let this note go?", color = MyBaseColors.Ink) },
            text = { Text("This note will be deleted from MyBase.", color = MyBaseColors.Muted) },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Delete note", color = MyBaseColors.RoseText) } },
            dismissButton = { TextButton(onClick = onCancelDelete) { Text("Keep note", color = MyBaseColors.Muted) } }
        )
    }
}

@Composable
private fun EmptyState(title: String, detail: String, action: String?, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        MomoIllustration(Modifier.size(126.dp))
        Text(title, Modifier.padding(top = 10.dp), color = MyBaseColors.Ink, fontSize = 17.sp)
        Text(detail, Modifier.padding(top = 5.dp), color = MyBaseColors.Muted, fontSize = 13.sp)
        if (action != null) TextButton(onClick = onAction) { Text(action, color = MyBaseColors.RoseText) }
    }
}

@Composable
private fun NoteCard(note: CapturedEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        color = MyBaseColors.White, shape = RoundedCornerShape(19.dp), tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.title.ifBlank { "A thought to remember" }, Modifier.weight(1f), color = MyBaseColors.Ink, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("✎", Modifier.clickable(onClick = onEdit).padding(horizontal = 10.dp, vertical = 6.dp), color = MyBaseColors.RoseText)
                Text("Delete", Modifier.clickable(onClick = onDelete).padding(start = 7.dp, end = 2.dp, top = 6.dp, bottom = 6.dp), color = MyBaseColors.RoseText, fontSize = 12.sp)
            }
            if (note.body.isNotBlank()) Text(note.body, Modifier.padding(top = 7.dp), color = MyBaseColors.Muted, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
            if (note.categories.isNotEmpty() || note.tags.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    note.categories.forEach { Badge(it, MyBaseColors.Blush, MyBaseColors.RoseText) }
                    note.tags.forEach { Badge("# $it", MyBaseColors.Lavender, MyBaseColors.LavenderDark) }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, background: androidx.compose.ui.graphics.Color, foreground: androidx.compose.ui.graphics.Color) {
    Text(text, Modifier.background(background, RoundedCornerShape(30.dp)).padding(horizontal = 9.dp, vertical = 4.dp), color = foreground, fontSize = 11.sp, maxLines = 1)
}
