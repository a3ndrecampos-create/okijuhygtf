package com.rotacerta.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rotacerta.app.data.Verdict

val Bg = Color(0xFF0A0F1C)
val Surface1 = Color(0xFF111827)
val Surface2 = Color(0xFF182034)
val Stroke = Color(0xFF273049)
val Green = Color(0xFF22D38A)
val Amber = Color(0xFFFFC53D)
val Red = Color(0xFFFF5470)
val Blue = Color(0xFF5B8CFF)
val TextHi = Color(0xFFF1F5F9)
val TextLo = Color(0xFF94A3B8)
val OnGreen = Color(0xFF04130C)

private val scheme = darkColorScheme(
    primary = Green, onPrimary = OnGreen, secondary = Blue,
    background = Bg, onBackground = TextHi,
    surface = Surface1, onSurface = TextHi,
    surfaceVariant = Surface2, onSurfaceVariant = TextLo,
    outline = Stroke, error = Red
)

private val type = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun RotaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}

fun Verdict.color(): Color = when (this) {
    Verdict.GREEN -> Green
    Verdict.YELLOW -> Amber
    Verdict.RED -> Red
}

fun Verdict.label(): String = when (this) {
    Verdict.GREEN -> "ACEITE"
    Verdict.YELLOW -> "AVALIE"
    Verdict.RED -> "RECUSE"
}
