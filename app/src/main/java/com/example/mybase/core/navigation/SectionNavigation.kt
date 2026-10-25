package com.example.mybase.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybase.ui.theme.MyBaseColors

@Composable
fun MyBaseNavigationRail(selected: Destination, onSelect: (Destination) -> Unit) {
    Column(
        Modifier.fillMaxHeight().width(100.dp).background(MyBaseColors.Sidebar).padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("m", fontSize = 27.sp, color = MyBaseColors.Rose, modifier = Modifier.padding(bottom = 30.dp))
        Destination.entries.forEach { destination ->
            val active = selected == destination
            Column(
                Modifier.padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) MyBaseColors.Blush else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(destination) }
                    .padding(vertical = 10.dp)
                    .width(74.dp)
                    .heightIn(min = 56.dp)
                    .semantics(mergeDescendants = true) { contentDescription = destination.label },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val inactiveColor = androidx.compose.ui.graphics.Color(0xFF6F625A)
                Text(destination.glyph, fontSize = 21.sp, color = if (active) MyBaseColors.Ink else inactiveColor)
                Text(destination.label, fontSize = 11.sp, color = if (active) MyBaseColors.Ink else inactiveColor, maxLines = 1)
            }
        }
    }
}

@Composable
fun MyBaseBottomNavigation(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar(containerColor = androidx.compose.ui.graphics.Color(0xFFFFFCF8), tonalElevation = 0.dp) {
        Destination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { Text(destination.glyph, fontSize = 19.sp) },
                label = { Text(destination.label, fontSize = 9.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MyBaseColors.Ink,
                    selectedTextColor = MyBaseColors.Ink,
                    indicatorColor = MyBaseColors.Blush,
                    unselectedIconColor = MyBaseColors.Muted,
                    unselectedTextColor = MyBaseColors.Muted
                )
            )
        }
    }
}
