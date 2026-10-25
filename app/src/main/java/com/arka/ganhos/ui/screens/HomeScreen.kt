package com.arka.ganhos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arka.ganhos.billing.AccessState
import com.arka.ganhos.core.money
import com.arka.ganhos.core.num
import com.arka.ganhos.data.AppSettings
import com.arka.ganhos.data.HistoryEntry
import com.arka.ganhos.data.Verdict
import com.arka.ganhos.ui.*
import java.util.Calendar
import kotlin.math.max

private fun startOfDay(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun HomeScreen(
    s: AppSettings,
    accessOn: Boolean,
    history: List<HistoryEntry>,
    access: AccessState,
    onMonitoring: (Boolean) -> Unit,
    onEnable: () -> Unit,
    onSubscribe: () -> Unit
) {
    val today = remember(history) { val t0 = startOfDay(); history.filter { it.timestamp >= t0 } }
    val greens = today.count { it.verdict == Verdict.GREEN }
    val avgKm = if (today.isEmpty()) 0.0 else today.map { it.perKm }.average()
    val avgProfit = if (today.isEmpty()) 0.0 else today.map { it.profit }.average()

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column {
                Text("Arka", style = MaterialTheme.typography.headlineMedium, color = TextHi)
                Text("Seu copiloto de lucro", style = MaterialTheme.typography.bodyMedium, color = TextLo)
            }
        }
        item { Hero(accessOn, s.monitoring, onMonitoring, onEnable) }
        item { SubscriptionCard(access, onSubscribe) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Analisadas hoje", today.size.toString(), TextHi, Modifier.weight(1f))
                Stat("Corridas boas", greens.toString(), Green, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Média R\$/km", num(avgKm), TextHi, Modifier.weight(1f))
                Stat("Lucro médio", money(avgProfit), TextHi, Modifier.weight(1f))
            }
        }
        item { CostCard(s) }
        history.firstOrNull()?.let { e ->
            item {
                Column {
                    SectionTitle("Última corrida analisada")
                    Spacer(Modifier.height(6.dp))
                    HistoryRow(e)
                }
            }
        }
    }
}

@Composable
private fun Hero(accessOn: Boolean, monitoring: Boolean, onMonitoring: (Boolean) -> Unit, onEnable: () -> Unit) {
    val active = accessOn && monitoring
    val brush = if (active) Brush.linearGradient(listOf(Color(0xFF0F6B4D), Color(0xFF123C73)))
    else Brush.linearGradient(listOf(Color(0xFF3A2A12), Color(0xFF1A2236)))
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().clip(shape).background(brush).border(1.dp, Stroke, shape).padding(20.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrafficLight(if (active) Verdict.GREEN else null)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            !accessOn -> "Leitura desativada"
                            !monitoring -> "Monitoramento pausado"
                            else -> "Monitorando corridas"
                        },
                        style = MaterialTheme.typography.titleLarge, color = TextHi
                    )
                    Text(
                        when {
                            !accessOn -> "Ative a acessibilidade para o semáforo aparecer sobre o Uber, 99 e inDrive."
                            !monitoring -> "Ligue o botão para voltar a analisar as ofertas."
                            else -> "Abra seu app de corridas e fique online. O semáforo aparece sozinho."
                        },
                        style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD5E1)
                    )
                }
                if (accessOn) {
                    Switch(
                        checked = monitoring, onCheckedChange = onMonitoring,
                        colors = SwitchDefaults.colors(checkedTrackColor = Green, checkedThumbColor = OnGreen)
                    )
                }
            }
            if (!accessOn) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onEnable,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color(0xFF1F1500))
                ) { Text("Ativar leitura de tela") }
            }
        }
    }
}

@Composable
private fun CostCard(s: AppSettings) {
    val parts = listOf(
        Triple("Combustível", s.fuelPerKm, Green),
        Triple("Manutenção", s.maintenancePerKm, Blue),
        Triple("Depreciação", s.depreciationPerKm, Amber),
        Triple("Outros", s.otherPerKm, Red)
    )
    AppCard {
        Text("Seu custo por km", style = MaterialTheme.typography.titleMedium, color = TextHi)
        Text(money(s.costPerKm), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Green)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50))) {
            parts.forEach { (_, v, c) ->
                Box(Modifier.weight(max(v, 0.001).toFloat()).fillMaxHeight().background(c))
            }
        }
        Spacer(Modifier.height(12.dp))
        parts.forEach { (n, v, c) ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(c))
                Spacer(Modifier.width(8.dp))
                Text(n, color = TextLo, modifier = Modifier.weight(1f))
                Text(money(v), color = TextHi)
            }
        }
    }
}
