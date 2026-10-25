package com.example.mybase.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.ui.theme.MyBaseColors

@Composable
fun PlaceholderSectionScreen(title: String, detail: String, glyph: String) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(108.dp).clip(CircleShape).background(MyBaseColors.Blush), contentAlignment = Alignment.Center) {
            Text(glyph, color = MyBaseColors.Rose, fontSize = 42.sp)
        }
        Text(title, fontSize = 26.sp, color = MyBaseColors.Ink, modifier = Modifier.padding(top = 20.dp))
        Text(detail, fontSize = 15.sp, color = MyBaseColors.Muted, modifier = Modifier.widthIn(max = 420.dp).padding(top = 8.dp), lineHeight = 22.sp)
    }
}
