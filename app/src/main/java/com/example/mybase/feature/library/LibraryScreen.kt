package com.example.mybase.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.core.designsystem.MomoIllustration
import com.example.mybase.core.model.LibraryResource
import com.example.mybase.core.model.ResourceType
import com.example.mybase.data.document.DocumentLocation
import com.example.mybase.data.document.PdfThumbnailStore
import com.example.mybase.ui.theme.MyBaseColors

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    thumbnailStore: PdfThumbnailStore,
    onSearch: (String) -> Unit,
    onType: (ResourceType?) -> Unit,
    onCategory: (String?) -> Unit,
    onTag: (String?) -> Unit,
    onCreate: () -> Unit,
    onInfo: (LibraryResource) -> Unit,
    onDismissInfo: () -> Unit,
    onEdit: (LibraryResource) -> Unit,
    onRequestDelete: (LibraryResource) -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenExternal: (String) -> Unit,
    onPreview: (LibraryResource) -> Unit,
    onAddFile: () -> Unit,
    onBrowseFiles: () -> Unit,
    onChooseFolder: () -> Unit,
    onCloseBrowser: () -> Unit,
    onBrowseFolder: (DocumentLocation) -> Unit,
    onEnterFolder: (DocumentLocation) -> Unit,
    onBackFolder: () -> Unit,
    onAddFolderItem: (DocumentLocation) -> Unit,
    onDismissEditor: () -> Unit,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
    onTitle: (String) -> Unit,
    onResourceType: (ResourceType) -> Unit,
    onReference: (String) -> Unit,
    onDescription: (String) -> Unit,
    onCategoryDraft: (String) -> Unit,
    onTagsDraft: (String) -> Unit,
    onSave: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onClearFilters: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MyBaseColors.Cream)) {
        val wide = maxWidth >= 720.dp
        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 32.dp else 18.dp).widthIn(max = 1120.dp).align(Alignment.TopCenter)) {
            Row(Modifier.fillMaxWidth().padding(top = if (wide) 28.dp else 18.dp, bottom = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Library", color = MyBaseColors.Ink, fontSize = if (wide) 30.sp else 26.sp)
                    Text("A small, thoughtful shelf for what you want to revisit.", color = MyBaseColors.Muted, fontSize = 13.sp)
                }
                FilledTonalButton(onClick = onCreate, shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = MyBaseColors.Mint, contentColor = MyBaseColors.MintDark)) {
                    Text("＋  Add resource")
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddFile, shape = RoundedCornerShape(14.dp)) { Text("＋ Add file") }
                OutlinedButton(onClick = onBrowseFiles, shape = RoundedCornerShape(14.dp)) { Text("Browse files") }
            }
            OutlinedTextField(
                value = state.query, onValueChange = onSearch, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search title, notes, links, or tags…") }, leadingIcon = { Text("⌕", fontSize = 22.sp, color = MyBaseColors.Muted) },
                singleLine = true, shape = RoundedCornerShape(17.dp), colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MyBaseColors.White, focusedContainerColor = MyBaseColors.White,
                    unfocusedBorderColor = MyBaseColors.Lavender, focusedBorderColor = MyBaseColors.LavenderDark
                )
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ResourceType.entries.forEach { type ->
                    FilterChip(selected = state.selectedType == type, onClick = { onType(type) }, label = { Text(type.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (type == ResourceType.Pdf) MyBaseColors.Blush else MyBaseColors.Lavender))
                }
            }
            if (state.categories.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.categories.forEach { FilterChip(selected = state.selectedCategory == it, onClick = { onCategory(it) }, label = { Text(it) }) }
                    state.tags.forEach { FilterChip(selected = state.selectedTag == it, onClick = { onTag(it) }, label = { Text("# $it") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MyBaseColors.Mint)) }
                }
            } else if (state.tags.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.tags.forEach { FilterChip(selected = state.selectedTag == it, onClick = { onTag(it) }, label = { Text("# $it") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MyBaseColors.Mint)) }
                }
            }

            when {
                state.resources.isEmpty() -> EmptyLibrary(onCreate)
                state.filteredResources.isEmpty() -> NoResults(onClearFilters)
                else -> {
                    Text("${state.filteredResources.size} ${if (state.filteredResources.size == 1) "resource" else "resources"}", Modifier.padding(vertical = 7.dp), color = MyBaseColors.Muted, fontSize = 12.sp)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 320.dp), modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.filteredResources, key = { it.id }) { resource ->
                            ResourceTile(
                                resource = resource,
                                displayTitle = state.documentNames[resource.id]?.takeIf { resource.title.startsWith("content://") || resource.title.startsWith("file://") } ?: readableResourceTitle(resource),
                                unavailable = resource.id in state.unavailableDocumentIds,
                                thumbnailStore = thumbnailStore,
                                onEdit = { onEdit(resource) }, onDelete = { onRequestDelete(resource) }, onInfo = { onInfo(resource) },
                                onOpen = { if (resource.type == ResourceType.Link) onOpenLink(resource.reference) else when (previewKind(resource)) {
                                    DocumentPreviewKind.Pdf, DocumentPreviewKind.PlainText, DocumentPreviewKind.Markdown -> onPreview(resource)
                                    DocumentPreviewKind.External -> onOpenExternal(resource.reference)
                                } }
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.isEditorVisible) ResourceEditor(state, onDismissEditor, onTitle, onResourceType, onReference, onDescription, onCategoryDraft, onTagsDraft, onSave)
    if (state.infoResource != null) ResourceInfoDialog(state, onDismissInfo)
    if (state.isBrowserVisible) FolderBrowser(state, onChooseFolder, onCloseBrowser, onBrowseFolder, onEnterFolder, onBackFolder, onAddFolderItem)
    if (state.showDiscardConfirmation) AlertDialog(
        onDismissRequest = onKeepEditing, containerColor = MyBaseColors.White,
        title = { Text("Leave this resource unfinished?", color = MyBaseColors.Ink) },
        text = { Text("Your unsaved changes will be discarded.", color = MyBaseColors.Muted) },
        confirmButton = { TextButton(onClick = onDiscard) { Text("Discard changes", color = MyBaseColors.RoseText) } },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text("Keep editing", color = MyBaseColors.Muted) }
        }
    )
    if (state.pendingDeleteId != null) AlertDialog(
        onDismissRequest = onCancelDelete, containerColor = MyBaseColors.White,
        title = { Text("Remove this resource?", color = MyBaseColors.Ink) },
        text = { Text("Only its Library record will be removed. Any referenced file will stay where it is.", color = MyBaseColors.Muted) },
        confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Delete resource", color = MyBaseColors.RoseText) } },
        dismissButton = { TextButton(onClick = onCancelDelete) { Text("Keep it", color = MyBaseColors.Muted) } }
    )
}

@Composable
private fun ResourceEditor(
    state: LibraryUiState, onDismiss: () -> Unit, onTitle: (String) -> Unit,
    onType: (ResourceType) -> Unit, onReference: (String) -> Unit, onDescription: (String) -> Unit,
    onCategory: (String) -> Unit, onTags: (String) -> Unit, onSave: () -> Unit
) {
    var typeMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MyBaseColors.White,
        title = { Text(if (state.editingId == null) "Add to your Library" else "Edit resource", color = MyBaseColors.Ink) },
        text = {
            Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(value = state.title, onValueChange = onTitle, modifier = Modifier.fillMaxWidth(), label = { Text("Resource title") }, singleLine = true)
                Box {
                    OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("${state.type.label}   ▾") }
                    DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        ResourceType.entries.forEach { type -> DropdownMenuItem(text = { Text(type.label) }, onClick = { onType(type); typeMenu = false }) }
                    }
                }
                OutlinedTextField(
                    value = state.reference, onValueChange = onReference, modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (state.type == ResourceType.Link) "Web address" else "Document reference or path") },
                    placeholder = { Text(if (state.type == ResourceType.Link) "https://example.com" else "File URI, path, or reference") },
                    isError = state.urlError != null || (state.reference.isBlank() && state.title.isNotBlank()), singleLine = true,
                    supportingText = { Text(state.urlError ?: "Required. References are saved without copying files.") }
                )
                OutlinedTextField(value = state.description, onValueChange = onDescription, modifier = Modifier.fillMaxWidth(), label = { Text("Short description (optional)") }, minLines = 2)
                OutlinedTextField(value = state.category, onValueChange = onCategory, modifier = Modifier.fillMaxWidth(), label = { Text("Category (optional)") }, singleLine = true)
                OutlinedTextField(value = state.tagsText, onValueChange = onTags, modifier = Modifier.fillMaxWidth(), label = { Text("Tags, comma separated") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = state.canSave) { Text("Save resource", color = MyBaseColors.MintDark) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MyBaseColors.Muted) } }
    )
}

@Composable
private fun ResourceTile(resource: LibraryResource, displayTitle: String, unavailable: Boolean, thumbnailStore: PdfThumbnailStore, onEdit: () -> Unit, onDelete: () -> Unit, onInfo: () -> Unit, onOpen: () -> Unit) {
    var menuExpanded by remember(resource.id) { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), color = MyBaseColors.White, shape = RoundedCornerShape(18.dp), tonalElevation = 1.dp) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (previewKind(resource) == DocumentPreviewKind.Pdf) PdfThumbnail(resource, thumbnailStore)
                else Text(previewGlyph(resource), Modifier.background(typeTint(resource.type), RoundedCornerShape(12.dp)).padding(horizontal = 11.dp, vertical = 9.dp), fontSize = 14.sp)
                Column(Modifier.weight(1f).padding(start = 11.dp)) {
                    Text(displayTitle, color = MyBaseColors.Ink, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${resource.type.label}${resource.category.takeIf(String::isNotBlank)?.let { "  ·  $it" }.orEmpty()}", color = MyBaseColors.Muted, fontSize = 11.sp)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) { Text("⋮", color = MyBaseColors.Muted, fontSize = 25.sp) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Info") }, onClick = { menuExpanded = false; onInfo() })
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menuExpanded = false; onEdit() })
                        DropdownMenuItem(text = { Text("Delete", color = MyBaseColors.RoseText) }, onClick = { menuExpanded = false; onDelete() })
                    }
                }
            }
            if (resource.description.isNotBlank()) Text(resource.description, Modifier.padding(top = 9.dp), color = MyBaseColors.Muted, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (resource.type == ResourceType.Link) Text(readableReference(resource), Modifier.padding(top = 8.dp), color = MyBaseColors.LavenderDark, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (resource.type != ResourceType.Link) Text(if (unavailable) "File unavailable · access may have changed" else "Reference saved · contents remain in your chosen location", Modifier.padding(top = 4.dp), color = if (unavailable) MyBaseColors.RoseText else MyBaseColors.Muted, fontSize = 10.sp, maxLines = 2)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (resource.tags.isNotEmpty()) Text(resource.tags.joinToString("  ") { "# $it" }, Modifier.weight(1f), color = MyBaseColors.MintDark, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) else Spacer(Modifier.weight(1f))
                Text("Tap to ${if (resource.type == ResourceType.Link) "open link" else "open"}", color = MyBaseColors.Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ResourceInfoDialog(state: LibraryUiState, onDismiss: () -> Unit) {
    val resource = state.infoResource ?: return
    val location = state.infoLocation
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MyBaseColors.White,
        title = { Text(readableResourceTitle(resource).let { state.documentNames[resource.id] ?: it }, color = MyBaseColors.Ink) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                InfoLine("Type", resource.mimeType.ifBlank { resource.type.label })
                if (resource.category.isNotBlank()) InfoLine("Category", resource.category)
                if (resource.tags.isNotEmpty()) InfoLine("Tags", resource.tags.joinToString(", "))
                if (resource.description.isNotBlank()) InfoLine("Description", resource.description)
                if (state.infoLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MyBaseColors.MintDark)
                else if (resource.type != ResourceType.Link) {
                    InfoLine("Availability", if (state.infoAvailable == true) "Readable now" else "Unavailable or permission revoked")
                    location?.sizeBytes?.let { InfoLine("File size", readableFileSize(it)) }
                    location?.modifiedAtMillis?.let { InfoLine("Modified", java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))) }
                    location?.mimeType?.takeIf(String::isNotBlank)?.let { InfoLine("MIME type", it) }
                }
                Text("Reference", color = MyBaseColors.Muted, fontSize = 12.sp)
                Surface(color = MyBaseColors.Cream, shape = RoundedCornerShape(12.dp)) {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(resource.reference, Modifier.fillMaxWidth().padding(11.dp), color = MyBaseColors.Ink, fontSize = 12.sp)
                    }
                }
                Text("Long press the reference to select and copy it.", color = MyBaseColors.Muted, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = MyBaseColors.MintDark) } }
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = MyBaseColors.Muted, fontSize = 11.sp)
        Text(value, color = MyBaseColors.Ink, fontSize = 13.sp)
    }
}

private fun readableFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}

@Composable
private fun PdfThumbnail(resource: LibraryResource, store: PdfThumbnailStore) {
    val bitmap = produceState<android.graphics.Bitmap?>(null, resource.reference, resource.mimeType, store) {
        value = store.load(resource.reference, resource.mimeType)
    }.value
    if (bitmap != null && !bitmap.isRecycled) {
        Image(bitmap.asImageBitmap(), "PDF cover: ${resource.title}", Modifier.size(width = 60.dp, height = 82.dp), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.size(width = 60.dp, height = 82.dp).background(MyBaseColors.Blush, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text("▤\nPDF", color = MyBaseColors.RoseText, fontSize = 13.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun FolderBrowser(
    state: LibraryUiState,
    onChooseFolder: () -> Unit,
    onClose: () -> Unit,
    onBrowseFolder: (DocumentLocation) -> Unit,
    onEnterFolder: (DocumentLocation) -> Unit,
    onBackFolder: () -> Unit,
    onAddFile: (DocumentLocation) -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = MyBaseColors.White,
        title = { Text("Browse authorized files", color = MyBaseColors.Ink) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Only folders you choose are shown. Downloads and Documents appear when your file provider exposes them.", color = MyBaseColors.Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick = onChooseFolder) { Text("Choose folder") }
                    if (state.folderStack.isNotEmpty()) OutlinedButton(onClick = onBackFolder) { Text("‹ Back") }
                }
                if (state.folderLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MyBaseColors.MintDark)
                if (state.folderError != null) Text(state.folderError, color = MyBaseColors.Muted, fontSize = 12.sp)
                val entries = if (state.folderStack.isEmpty()) state.authorizedFolders else state.folderItems
                entries.forEach { item ->
                    Row(Modifier.fillMaxWidth().background(MyBaseColors.Cream, RoundedCornerShape(13.dp)).padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (item.isDirectory) "▰" else "▤", Modifier.padding(horizontal = 8.dp), color = MyBaseColors.LavenderDark)
                        Text(item.name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, color = MyBaseColors.Ink)
                        if (item.isDirectory) TextButton(onClick = { if (state.folderStack.isEmpty()) onBrowseFolder(item) else onEnterFolder(item) }) { Text("Open") }
                        else TextButton(onClick = { onAddFile(item) }) { Text("Add") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done", color = MyBaseColors.MintDark) } }
    )
}

@Composable
private fun EmptyLibrary(onCreate: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        MomoIllustration(Modifier.size(126.dp))
        Text("Your shelf is waiting", Modifier.padding(top = 10.dp), color = MyBaseColors.Ink, fontSize = 18.sp)
        Text("Save a link or a document reference to keep useful things close.", Modifier.padding(top = 5.dp), color = MyBaseColors.Muted, fontSize = 13.sp)
        TextButton(onClick = onCreate) { Text("Add your first resource", color = MyBaseColors.MintDark) }
    }
}

@Composable
private fun NoResults(onClear: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("No resources on this shelf", color = MyBaseColors.Ink, fontSize = 18.sp)
        Text("Try another search or clear the filters.", Modifier.padding(top = 5.dp), color = MyBaseColors.Muted, fontSize = 13.sp)
        TextButton(onClick = onClear) { Text("Clear search and filters", color = MyBaseColors.LavenderDark) }
    }
}

private fun typeGlyph(type: ResourceType) = when (type) {
    ResourceType.Link -> "↗"
    ResourceType.Pdf -> "▤"
    ResourceType.Document -> "▧"
    ResourceType.Other -> "✧"
}

private fun previewGlyph(resource: LibraryResource) = when (previewKind(resource)) {
    DocumentPreviewKind.Pdf -> "PDF"
    DocumentPreviewKind.PlainText -> "TXT"
    DocumentPreviewKind.Markdown -> "MD"
    DocumentPreviewKind.External -> typeGlyph(resource.type)
}

private fun typeTint(type: ResourceType) = when (type) {
    ResourceType.Link -> MyBaseColors.Mint
    ResourceType.Pdf -> MyBaseColors.Blush
    ResourceType.Document -> MyBaseColors.Lavender
    ResourceType.Other -> MyBaseColors.Sidebar
}
