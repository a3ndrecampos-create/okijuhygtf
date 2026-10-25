package com.arka.ganhos.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
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
import com.arka.ganhos.data.Verdict
import com.arka.ganhos.ui.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BR: Locale = Locale.forLanguageTag("pt-BR")
private val timeFmt = SimpleDateFormat("HH:mm", BR)
private val dayFmt = SimpleDateFormat("dd/MM", BR)

private fun qualityLabel(v: Verdict) = when (v) {
    Verdict.GREEN -> "Ótima"
    Verdict.YELLOW -> "Regular"
    Verdict.RED -> "Fraca"
}

@Composable
private fun MiniTile(value: String, label: String, modifier: Modifier = Modifier, star: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Surface2).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (star) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = Amber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextHi)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextLo)
    }
}

@Composable
fun HistoryRow(e: HistoryEntry, onDelete: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(20.dp)
    val c = e.verdict.color()
    val pct = if (e.price > 0) Math.round(e.profit / e.price * 100).toInt() else 0
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Stroke, shape).padding(14.dp)
    ) {
        // Linha 1: app, valor, hora e qualidade da corrida
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppBadge(e.source)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(money(e.price), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextHi)
                    Spacer(Modifier.width(8.dp))
                    Text(timeFmt.format(Date(e.timestamp)), style = MaterialTheme.typography.bodySmall,
                        color = TextLo, modifier = Modifier.padding(bottom = 3.dp))
                }
                Text(
                    num(e.totalKm, 1) + " km · " + (if (e.totalMin > 0) e.totalMin.toString() + " min · " else "") + num(e.perKm) + " R\$/km",
                    style = MaterialTheme.typography.bodySmall, color = TextLo
                )
            }
            Text(
                qualityLabel(e.verdict), color = c, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Linha 2: por hora e nota
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniTile(money(e.perHour), "por hora", Modifier.weight(1f))
            MiniTile(if (e.rating != null) num(e.rating) else "—", "nota", Modifier.weight(1f), star = e.rating != null)
        }

        // Linha 3: lucro
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.16f))
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Lucro " + money(e.profit) + " (" + pct + "%)", color = c, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // Rodapé: data e ações
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dayFmt.format(Date(e.timestamp)), style = MaterialTheme.typography.bodySmall, color = TextLo,
                modifier = Modifier.weight(1f))
            Icon(
                Icons.Rounded.Share, contentDescription = "Compartilhar", tint = TextLo,
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable { shareEntry(ctx, e) }.padding(9.dp)
            )
            if (onDelete != null) {
                Icon(
                    Icons.Rounded.DeleteOutline, contentDescription = "Excluir", tint = Red,
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onDelete() }.padding(9.dp)
                )
            }
        }
    }
}

private fun shareEntry(ctx: Context, e: HistoryEntry) {
    val pct = if (e.price > 0) Math.round(e.profit / e.price * 100).toInt() else 0
    val text = "Arka · " + e.source + " · " + money(e.price) + " · " + num(e.totalKm, 1) + " km · " + (if (e.totalMin > 0) e.totalMin.toString() + " min · " + money(e.perHour) + "/h · " else "") +
        "lucro " + money(e.profit) + " (" + pct + "%)"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(send, "Compartilhar corrida").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
fun HistoryScreen(items: List<HistoryEntry>, onClear: () -> Unit, onDelete: (HistoryEntry) -> Unit) {
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
        items(items, key = { it.timestamp }) { e -> HistoryRow(e) { onDelete(e) } }
    }
}
