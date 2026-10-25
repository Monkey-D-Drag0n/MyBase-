package com.example.mybase.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun MomoIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(Color(0xFFFFF9F4), radius = w * .46f, center = Offset(w * .5f, h * .52f))
        drawRoundRect(Color(0xFFB9CDBD), Offset(w * .19f, h * .59f), Size(w * .62f, h * .34f), CornerRadius(w * .18f))
        drawCircle(Color(0xFF755347), radius = w * .31f, center = Offset(w * .5f, h * .36f))
        drawOval(Color(0xFFFFDFC7), Offset(w * .27f, h * .27f), Size(w * .46f, h * .44f))
        drawOval(Color(0xFF755347), Offset(w * .22f, h * .19f), Size(w * .56f, h * .31f))
        drawCircle(Color(0xFF755347), w * .12f, Offset(w * .27f, h * .37f))
        drawCircle(Color(0xFF755347), w * .12f, Offset(w * .73f, h * .37f))
        drawLine(Color(0xFF755347), Offset(w * .39f, h * .36f), Offset(w * .46f, h * .39f), w * .025f, cap = StrokeCap.Round)
        drawLine(Color(0xFF755347), Offset(w * .61f, h * .39f), Offset(w * .68f, h * .36f), w * .025f, cap = StrokeCap.Round)
        drawCircle(Color(0xFFE9A2A0), w * .035f, Offset(w * .34f, h * .45f))
        drawCircle(Color(0xFFE9A2A0), w * .035f, Offset(w * .66f, h * .45f))
        drawArc(Color(0xFF9B655C), 20f, 140f, false, Offset(w * .44f, h * .46f), Size(w * .12f, h * .08f), style = Stroke(width = w * .018f, cap = StrokeCap.Round))
        drawLine(Color(0xFFEDF2E9), Offset(w * .5f, h * .64f), Offset(w * .5f, h * .84f), w * .018f, cap = StrokeCap.Round)
    }
}
