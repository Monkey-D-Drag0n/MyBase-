package com.example.mybase.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.ui.theme.MyBaseColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeHeader(onSearch: () -> Unit, onSettings: () -> Unit) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello, night owl"
    }
    val date = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()) }
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 26.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("mybase", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MyBaseColors.Ink, letterSpacing = (-0.8).sp)
            Text("$greeting  ·  $date", fontSize = 13.sp, color = MyBaseColors.Muted)
        }
        IconButton(onClick = onSearch) { Text("⌕", fontSize = 28.sp, color = MyBaseColors.Ink) }
        IconButton(onClick = onSettings) { Text("⚙", fontSize = 20.sp, color = MyBaseColors.Ink) }
    }
}
