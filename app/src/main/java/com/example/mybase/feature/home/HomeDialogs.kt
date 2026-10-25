package com.example.mybase.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.ui.theme.MyBaseColors

@Composable
fun CaptureDialog(
    state: HomeUiState,
    onTitleChange: (String) -> Unit,
    onKindChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MyBaseColors.White,
        title = { Text("Catch that thought", color = MyBaseColors.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("A note or a next step — you can tidy it up later.", color = MyBaseColors.Muted, fontSize = 13.sp)
                OutlinedTextField(value = state.captureTitle, onValueChange = onTitleChange, label = { Text("What's on your mind?") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Note", "Task").forEach { item ->
                        FilterChip(selected = state.captureKind == item, onClick = { onKindChange(item) }, label = { Text(item) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = state.captureTitle.isNotBlank()) {
                Text("Save", color = MyBaseColors.Ink)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MyBaseColors.Muted) } }
    )
}

@Composable
fun SearchDialog(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find in MyBase", color = MyBaseColors.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.searchQuery, onValueChange = onQueryChange, label = { Text("Search notes and tasks") }, singleLine = true)
                state.searchResults.forEach { entry ->
                    Text("${if (entry.kind == "Task") "✓" else "✎"}  ${entry.title}", color = MyBaseColors.Ink, fontSize = 13.sp)
                }
                if (state.searchQuery.isNotBlank() && state.searchResults.isEmpty()) {
                    Text("No matching notes or tasks yet.", color = MyBaseColors.Muted, fontSize = 12.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text("Done", color = MyBaseColors.Ink) } },
        containerColor = MyBaseColors.White
    )
}
