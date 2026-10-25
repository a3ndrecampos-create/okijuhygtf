package com.arka.ganhos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arka.ganhos.data.THEME_DARK
import com.arka.ganhos.data.THEME_LIGHT
import com.arka.ganhos.data.Verdict

/** Conjunto de cores de um tema (claro ou escuro). */
@Immutable
class Palette(
    val dark: Boolean,
    val bg: Color, val surface1: Color, val surface2: Color, val stroke: Color,
    val green: Color, val amber: Color, val red: Color, val blue: Color,
    val textHi: Color, val textLo: Color,
    val onGreen: Color, val onAmber: Color,
    val heroActive: List<Color>, val heroIdle: List<Color>, val heroBody: Color,
    val trafficBg: Color,
    val badges: List<Color>
)

private val DarkPalette = Palette(
    dark = true,
    bg = Color(0xFF0A0F1C), surface1 = Color(0xFF111827), surface2 = Color(0xFF182034), stroke = Color(0xFF273049),
    green = Color(0xFF22D38A), amber = Color(0xFFFFC53D), red = Color(0xFFFF5470), blue = Color(0xFF5B8CFF),
    textHi = Color(0xFFF1F5F9), textLo = Color(0xFF94A3B8),
    onGreen = Color(0xFF04130C), onAmber = Color(0xFF1F1500),
    heroActive = listOf(Color(0xFF0F6B4D), Color(0xFF123C73)),
    heroIdle = listOf(Color(0xFF3A2A12), Color(0xFF1A2236)),
    heroBody = Color(0xFFCBD5E1),
    trafficBg = Color(0xCC0A0F1C),
    badges = listOf(Color(0xFF60A5FA), Color(0xFFFBBF24), Color(0xFF2DD4BF), Color(0xFFF472B6), Color(0xFFA78BFA))
)

// No claro, verde/amarelo/vermelho são mais escuros para ter contraste de leitura sobre fundo branco.
private val LightPalette = Palette(
    dark = false,
    bg = Color(0xFFF4F6FB), surface1 = Color(0xFFFFFFFF), surface2 = Color(0xFFEBEFF6), stroke = Color(0xFFD5DCE8),
    green = Color(0xFF0A8250), amber = Color(0xFFB45309), red = Color(0xFFD92D45), blue = Color(0xFF2F5FD0),
    textHi = Color(0xFF0F172A), textLo = Color(0xFF526077),
    onGreen = Color(0xFFFFFFFF), onAmber = Color(0xFFFFFFFF),
    heroActive = listOf(Color(0xFFD3F0E2), Color(0xFFD6E3FA)),
    heroIdle = listOf(Color(0xFFFBEACB), Color(0xFFE6EBF4)),
    heroBody = Color(0xFF3F4B5F),
    trafficBg = Color(0xFF1E293B),
    badges = listOf(Color(0xFF2563EB), Color(0xFFB45309), Color(0xFF0F766E), Color(0xFFBE185D), Color(0xFF6D28D9))
)

/**
 * Paleta em uso. É um estado do Compose: quem lê uma cor (Bg, TextHi...) é redesenhado quando o tema muda.
 * Quem define é o RotaTheme, antes de desenhar o conteúdo.
 */
private var palette by mutableStateOf(DarkPalette)

val Bg: Color get() = palette.bg
val Surface1: Color get() = palette.surface1
val Surface2: Color get() = palette.surface2
val Stroke: Color get() = palette.stroke
val Green: Color get() = palette.green
val Amber: Color get() = palette.amber
val Red: Color get() = palette.red
val Blue: Color get() = palette.blue
val TextHi: Color get() = palette.textHi
val TextLo: Color get() = palette.textLo
val OnGreen: Color get() = palette.onGreen
val OnAmber: Color get() = palette.onAmber
val HeroBody: Color get() = palette.heroBody
val TrafficBg: Color get() = palette.trafficBg
val BadgeColors: List<Color> get() = palette.badges
val HeroActive: Brush get() = Brush.linearGradient(palette.heroActive)
val HeroIdle: Brush get() = Brush.linearGradient(palette.heroIdle)

private fun Palette.colorScheme() =
    if (dark) darkColorScheme(
        primary = green, onPrimary = onGreen, secondary = blue,
        background = bg, onBackground = textHi,
        surface = surface1, onSurface = textHi,
        surfaceVariant = surface2, onSurfaceVariant = textLo,
        outline = stroke, error = red
    ) else lightColorScheme(
        primary = green, onPrimary = onGreen, secondary = blue,
        background = bg, onBackground = textHi,
        surface = surface1, onSurface = textHi,
        surfaceVariant = surface2, onSurfaceVariant = textLo,
        outline = stroke, error = red
    )

/** O app está em tema escuro? Considera a escolha do usuário (Automático / Claro / Escuro). */
@Composable
fun isDarkTheme(mode: Int): Boolean = when (mode) {
    THEME_LIGHT -> false
    THEME_DARK -> true
    else -> isSystemInDarkTheme()
}

private val type = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun RotaTheme(mode: Int, content: @Composable () -> Unit) {
    val p = if (isDarkTheme(mode)) DarkPalette else LightPalette
    palette = p
    MaterialTheme(colorScheme = p.colorScheme(), typography = type, content = content)
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
