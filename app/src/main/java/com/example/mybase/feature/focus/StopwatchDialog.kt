package com.example.mybase.feature.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.ui.theme.MyBaseColors
import java.util.Locale

@Composable
fun StopwatchDialog(
    state: StopwatchUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmReset by remember { mutableStateOf(false) }
    val totalSeconds = state.elapsedMillis / 1000L
    val display = if (totalSeconds >= 3600L) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds / 60) % 60, totalSeconds % 60)
    } else String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MyBaseColors.White,
        title = { Text("A moment for focus", color = MyBaseColors.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(display, fontSize = 42.sp, fontWeight = FontWeight.Light, color = MyBaseColors.Ink, modifier = Modifier.padding(vertical = 18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = if (state.isRunning) onPause else onStart, colors = ButtonDefaults.buttonColors(containerColor = MyBaseColors.Rose)) {
                        Text(if (state.isRunning) "Pause" else if (state.elapsedMillis > 0L) "Resume" else "Start")
                    }
                    OutlinedButton(onClick = { if (state.isRunning || state.elapsedMillis > 0L) confirmReset = true else onReset() }) { Text("Reset", color = MyBaseColors.Ink) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = MyBaseColors.Muted) } }
    )
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        containerColor = MyBaseColors.White,
        title = { Text("Reset this focus session?", color = MyBaseColors.Ink) },
        text = { Text("The elapsed time will return to zero.", color = MyBaseColors.Muted) },
        confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text("Reset", color = MyBaseColors.RoseText) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Keep session", color = MyBaseColors.Muted) } }
    )
}
