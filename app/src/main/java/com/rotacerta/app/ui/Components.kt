package com.rotacerta.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotacerta.app.core.money
import com.rotacerta.app.core.num
import com.rotacerta.app.data.Analysis
import com.rotacerta.app.data.Verdict
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Stroke, shape).padding(16.dp),
        content = content
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = TextHi,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
}

@Composable
fun Stat(label: String, value: String, color: Color = TextHi, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Surface2).padding(14.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextLo)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun VerdictPill(v: Verdict) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(v.color().copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(v.color()))
        Spacer(Modifier.width(8.dp))
        Text(v.label(), color = v.color(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun TrafficLight(active: Verdict?) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xCC0A0F1C)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(Verdict.RED, Verdict.YELLOW, Verdict.GREEN).forEach { v ->
            Box(
                Modifier.size(18.dp).clip(CircleShape)
                    .background(v.color().copy(alpha = if (active == v) 1f else 0.18f))
            )
        }
    }
}

@Composable
fun DecimalField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { t -> if (t.all { it.isDigit() || it == ',' || it == '.' }) onChange(t) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Green, unfocusedBorderColor = Stroke,
            focusedLabelColor = Green, cursorColor = Green
        )
    )
}

private fun fmt(v: Double): String =
    BigDecimal(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

@Composable
fun NumberField(label: String, value: Double, modifier: Modifier = Modifier, onValue: (Double) -> Unit) {
    var text by remember { mutableStateOf(fmt(value)) }
    DecimalField(label, text, { t ->
        text = t
        t.replace(',', '.').toDoubleOrNull()?.let(onValue)
    }, modifier)
}

@Composable
fun AnalysisCard(a: Analysis) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VerdictPill(a.verdict)
            Spacer(Modifier.weight(1f))
            Text(money(a.offer.price), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextHi)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("R\$/km", num(a.perKm), a.verdict.color(), Modifier.weight(1f))
            Stat("R\$/hora", num(a.perHour, 0), TextHi, Modifier.weight(1f))
            Stat("Lucro", money(a.profit), if (a.profit >= 0) Green else Red, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        a.reasons.forEach {
            Text("• " + it, color = TextLo, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Custo estimado " + money(a.cost) + " · tarifa mínima para suas metas " + money(a.minFare),
            color = TextLo, style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface2).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                    .background(if (on) Green else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(o, color = if (on) OnGreen else TextLo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}
