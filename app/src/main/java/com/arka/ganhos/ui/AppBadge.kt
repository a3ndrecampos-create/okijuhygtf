package com.arka.ganhos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arka.ganhos.data.SUPPORTED_APPS

/**
 * Pastilha com a inicial do app de corrida. Usa cores próprias do Arka (não as das marcas):
 * identifica o app sem usar logotipo de terceiros.
 */
@Composable
fun AppBadge(app: String, size: Dp = 44.dp) {
    val palette = BadgeColors
    val i = SUPPORTED_APPS.indexOf(app)
    val tint = if (i >= 0) palette[i % palette.size] else TextLo
    val label = when (app) {
        "Uber" -> "U"
        "99" -> "99"
        "inDrive" -> "iD"
        "iFood" -> "iF"
        "Keeta" -> "K"
        else -> app.take(2).ifBlank { "?" }
    }
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(size).clip(shape).background(tint.copy(alpha = 0.16f)).border(1.dp, tint.copy(alpha = 0.45f), shape),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = if (label.length > 1) 15.sp else 18.sp, maxLines = 1)
    }
}
