package com.example.mybase.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.core.designsystem.MomoIllustration
import com.example.mybase.core.model.CapturedEntry
import com.example.mybase.ui.theme.MyBaseColors

@Composable
fun WelcomeCard(modifier: Modifier, onCapture: () -> Unit) {
    Row(
        modifier.heightIn(min = 176.dp).shadow(5.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(MyBaseColors.Blush).padding(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("A fresh little start", fontSize = 12.sp, color = MyBaseColors.RoseText, fontWeight = FontWeight.SemiBold)
            Text("Hi, friend!", fontSize = 26.sp, color = MyBaseColors.Ink, fontWeight = FontWeight.Bold)
            Text("One step at a time. You've got this!", fontSize = 14.sp, color = MyBaseColors.Ink, lineHeight = 20.sp)
            Button(onClick = onCapture, colors = ButtonDefaults.buttonColors(containerColor = MyBaseColors.ButtonRose, contentColor = MyBaseColors.White), shape = RoundedCornerShape(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                Text("＋  Quick Capture", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("✿", fontSize = 36.sp, color = androidx.compose.ui.graphics.Color(0xFFCE8C8B), modifier = Modifier.padding(start = 8.dp).align(Alignment.Top))
    }
}

@Composable
fun TodayCard(modifier: Modifier, entries: List<CapturedEntry>) {
    val tasks = entries.filter { it.kind == "Task" }
    val done = tasks.count { it.completed }
    Column(modifier.heightIn(min = 176.dp).shadow(4.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(MyBaseColors.White).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("TODAY AT A GLANCE", fontSize = 11.sp, color = MyBaseColors.Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$done", fontSize = 28.sp, color = MyBaseColors.Ink, fontWeight = FontWeight.Bold)
            Text(" / ${tasks.size} tasks", fontSize = 13.sp, color = MyBaseColors.Muted, modifier = Modifier.padding(start = 4.dp))
        }
        LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = MyBaseColors.Rose, trackColor = MyBaseColors.Blush)
        InfoLine("◷", "Upcoming", "Nothing scheduled yet", MyBaseColors.Lavender)
        InfoLine("◇", "Focus time", if (tasks.isNotEmpty()) "Ready when you are" else "Start with one small step", MyBaseColors.Mint)
    }
}

@Composable
fun MomoCard(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "momo")
    val bob by transition.animateFloat(0f, 4f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Column(modifier.heightIn(min = 176.dp).shadow(4.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(MyBaseColors.Lavender).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        MomoIllustration(Modifier.size(104.dp).offset(y = (-bob).dp))
        Text("Momo", fontSize = 16.sp, color = MyBaseColors.Ink, fontWeight = FontWeight.Bold)
        Text("your cozy companion", fontSize = 11.sp, color = MyBaseColors.Muted)
    }
}

@Composable
fun InfoLine(glyph: String, title: String, detail: String, tint: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(29.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) { Text(glyph, fontSize = 14.sp, color = MyBaseColors.Ink) }
        Column(Modifier.padding(start = 9.dp)) {
            Text(title, fontSize = 12.sp, color = MyBaseColors.Ink, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 11.sp, color = MyBaseColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(title, fontSize = 19.sp, color = MyBaseColors.Ink, fontWeight = FontWeight.Bold)
        Text(subtitle, fontSize = 12.sp, color = MyBaseColors.Muted, modifier = Modifier.padding(start = 10.dp, bottom = 2.dp))
    }
}

@Composable
fun ActionTile(label: String, glyph: String, tint: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(88.dp).shadow(2.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(MyBaseColors.White).clickable(onClick = onClick).padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(tint), contentAlignment = Alignment.Center) { Text(glyph, fontSize = 18.sp, color = MyBaseColors.Ink) }
        Text(label, fontSize = 12.sp, color = MyBaseColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun EmptyActivity(onCapture: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MyBaseColors.White).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("☁", fontSize = 30.sp, color = MyBaseColors.Rose, modifier = Modifier.padding(end = 14.dp))
        Column(Modifier.weight(1f)) {
            Text("A little space for your ideas", color = MyBaseColors.Ink, fontWeight = FontWeight.SemiBold)
            Text("Your recent notes and projects will show up here.", color = MyBaseColors.Muted, fontSize = 12.sp)
        }
        TextButton(onClick = onCapture) { Text("Add one", color = MyBaseColors.ButtonRose) }
    }
}

@Composable
fun ActivityRow(entry: CapturedEntry, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MyBaseColors.White).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(if (entry.kind == "Task") MyBaseColors.Mint else MyBaseColors.Blush).then(if (entry.kind == "Task") Modifier.clickable(onClick = onToggle) else Modifier), contentAlignment = Alignment.Center) {
            Text(if (entry.kind == "Task" && entry.completed) "✓" else if (entry.kind == "Task") "○" else "✎", color = MyBaseColors.Ink)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp).then(if (entry.kind == "Task") Modifier.clickable(onClick = onToggle) else Modifier)) {
            Text(entry.title, color = if (entry.completed) MyBaseColors.Muted else MyBaseColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${entry.kind} · just now", color = MyBaseColors.Muted, fontSize = 11.sp)
        }
    }
}
