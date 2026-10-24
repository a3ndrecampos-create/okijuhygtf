package com.arka.ganhos.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Marca do Arka: três barras crescentes (vermelho, amarelo, verde) = ganhos subindo. */
@Composable
fun ArkaMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val h = size.height
        val barW = size.width * 0.22f
        val gap = size.width * 0.07f
        val total = barW * 3 + gap * 2
        val x0 = (size.width - total) / 2f
        val radius = CornerRadius(barW * 0.33f)
        val heights = listOf(0.38f, 0.68f, 1.0f)
        val colors = listOf(Red, Amber, Green)
        for (i in 0..2) {
            val bh = h * heights[i]
            drawRoundRect(
                color = colors[i],
                topLeft = Offset(x0 + i * (barW + gap), h - bh),
                size = Size(barW, bh),
                cornerRadius = radius
            )
        }
    }
}

/** Tela de abertura no estilo "from" do Facebook: marca no centro, assinatura embaixo. */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1800)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            ArkaMark(Modifier.size(width = 120.dp, height = 110.dp))
            Spacer(Modifier.height(14.dp))
            Text(
                "Arka", color = TextHi, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("from", color = TextLo, fontSize = 13.sp)
            Text(
                "André Campos", color = TextHi, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp
            )
        }
    }
}
