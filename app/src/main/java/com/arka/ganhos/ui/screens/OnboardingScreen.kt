package com.arka.ganhos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arka.ganhos.ui.*
import kotlinx.coroutines.launch

private data class Page(val icon: ImageVector, val title: String, val body: String, val tint: Color)

@Composable
fun OnboardingScreen(accessOn: Boolean, onFinish: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pages = listOf(
        Page(Icons.Rounded.Traffic, "Decida em 1 segundo",
            "Quando uma corrida aparecer, o semáforo mostra se ela vale a pena antes de você aceitar.", Green),
        Page(Icons.Rounded.Payments, "Seu custo, sua meta",
            "Informe combustível, consumo e manutenção. O app calcula o lucro real e compara com as metas que você definir.", Blue),
        Page(Icons.Rounded.Lock, "Privacidade primeiro",
            "O app não tem acesso à internet. Ele lê só valor, tempo e distância da oferta e tudo fica no seu aparelho.", Amber)
    )
    val total = pages.size + 1
    val pager = rememberPagerState(pageCount = { total })

    Column(Modifier.fillMaxSize().background(Bg).systemBarsPadding().padding(24.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
            if (i < pages.size) InfoPage(pages[i]) else PermissionPage(accessOn, ctx)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(total) { i ->
                Box(
                    Modifier.padding(4.dp)
                        .size(width = if (i == pager.currentPage) 22.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (i == pager.currentPage) Green else Stroke)
                )
            }
        }
        Button(
            onClick = {
                if (pager.currentPage < total - 1) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                else onFinish()
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = OnGreen)
        ) { Text(if (pager.currentPage < total - 1) "Continuar" else "Começar", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun InfoPage(p: Page) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(p.tint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(p.icon, null, tint = p.tint, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(p.title, style = MaterialTheme.typography.headlineMedium, color = TextHi, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(p.body, style = MaterialTheme.typography.bodyMedium, color = TextLo, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PermissionPage(accessOn: Boolean, ctx: android.content.Context) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(Green.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Accessibility, null, tint = Green, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Ative a leitura de tela", style = MaterialTheme.typography.headlineMedium, color = TextHi, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "1. Abra Acessibilidade e escolha Arka.\n2. Ligue a chave e confirme.\nSe estiver cinza (Android 13+), use o botão de configurações restritas.",
            style = MaterialTheme.typography.bodyMedium, color = TextLo, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        if (accessOn) Text("Acessibilidade ativada", color = Green, fontWeight = FontWeight.Bold)
        else {
            Button(
                onClick = { openAccessibilitySettings(ctx) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color(0xFF1F1500))
            ) { Text("Abrir Acessibilidade") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { openAppInfo(ctx) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Liberar configurações restritas", color = TextHi) }
        }
    }
}
