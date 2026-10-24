package com.arka.ganhos.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arka.ganhos.core.money
import com.arka.ganhos.core.num
import com.arka.ganhos.data.HistoryEntry
import com.arka.ganhos.ui.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))

@Composable
fun HistoryRow(e: HistoryEntry) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Stroke, shape).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(e.verdict.color()))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(money(e.price), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextHi)
            Text(
                num(e.totalKm, 1) + " km · " + e.totalMin + " min · " + e.source + " · " + dateFmt.format(Date(e.timestamp)),
                style = MaterialTheme.typography.bodySmall, color = TextLo
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(num(e.perKm) + " R\$/km", color = e.verdict.color(), fontWeight = FontWeight.SemiBold)
            Text("lucro " + money(e.profit), style = MaterialTheme.typography.bodySmall, color = TextLo)
        }
    }
}

private fun shareCsv(ctx: Context, items: List<HistoryEntry>) {
    val f = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val sb = StringBuilder("data;app;valor;km;min;rs_km;rs_hora;lucro;veredito\n")
    items.forEach { e ->
        sb.append(f.format(Date(e.timestamp))).append(';').append(e.source).append(';')
            .append(String.format(Locale.US, "%.2f;%.1f;%d;%.2f;%.2f;%.2f", e.price, e.totalKm, e.totalMin, e.perKm, e.perHour, e.profit))
            .append(';').append(e.verdict.name).append('\n')
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Arka - histórico")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    ctx.startActivity(Intent.createChooser(send, "Exportar histórico").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun HistoryScreen(items: List<HistoryEntry>, onClear: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Histórico", style = MaterialTheme.typography.headlineMedium, color = TextHi,
                    modifier = Modifier.weight(1f))
                if (items.isNotEmpty()) {
                    TextButton(onClick = { shareCsv(ctx, items) }) { Text("Exportar", color = Green) }
                    TextButton(onClick = onClear) { Text("Limpar", color = Red) }
                }
            }
        }
        if (items.isEmpty()) {
            item {
                AppCard {
                    Text("Nenhuma corrida analisada ainda", style = MaterialTheme.typography.titleMedium, color = TextHi)
                    Spacer(Modifier.height(4.dp))
                    Text("As ofertas que o semáforo avaliar aparecem aqui, só neste aparelho.",
                        style = MaterialTheme.typography.bodyMedium, color = TextLo)
                }
            }
        }
        items(items) { HistoryRow(it) }
    }
}
