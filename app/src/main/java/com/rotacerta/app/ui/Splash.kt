package com.rotacerta.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Tela de abertura no estilo "from" do Facebook: logo no centro, assinatura embaixo. */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1800)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(Bg)) {
        Canvas(Modifier.align(Alignment.Center).size(width = 72.dp, height = 132.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = Surface2, size = Size(w, h), cornerRadius = CornerRadius(w * 0.25f)
            )
            drawRoundRect(
                color = Color(0xFF3A4663), size = Size(w, h), cornerRadius = CornerRadius(w * 0.25f),
                style = Stroke(width = 3f)
            )
            val r = w * 0.24f
            drawCircle(Red, r, Offset(w / 2, h * 0.19f))
            drawCircle(Amber, r, Offset(w / 2, h * 0.50f))
            drawCircle(Green, r, Offset(w / 2, h * 0.81f))
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
