package com.arka.ganhos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arka.ganhos.core.Calculator
import com.arka.ganhos.data.AppSettings
import com.arka.ganhos.data.RideOffer
import com.arka.ganhos.ui.*

private fun d(t: String): Double = t.replace(',', '.').toDoubleOrNull() ?: 0.0

@Composable
fun CalculatorScreen(s: AppSettings) {
    var price by rememberSaveable { mutableStateOf("") }
    var pk by rememberSaveable { mutableStateOf("") }
    var pm by rememberSaveable { mutableStateOf("") }
    var tk by rememberSaveable { mutableStateOf("") }
    var tm by rememberSaveable { mutableStateOf("") }

    val offer = RideOffer(d(price), d(pk), d(pm).toInt(), d(tk), d(tm).toInt())
    val analysis = if (offer.price > 0 && offer.totalKm > 0 && offer.totalMin > 0) Calculator.analyze(offer, s) else null

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Text("Calculadora", style = MaterialTheme.typography.headlineMedium, color = TextHi)
                Text("Teste uma corrida na mão, sem precisar do semáforo.",
                    style = MaterialTheme.typography.bodyMedium, color = TextLo)
            }
        }
        item { DecimalField("Valor da corrida (R$)", price, { price = it }) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DecimalField("Até o passageiro (km)", pk, { pk = it }, Modifier.weight(1f))
                DecimalField("Até o passageiro (min)", pm, { pm = it }, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DecimalField("Viagem (km)", tk, { tk = it }, Modifier.weight(1f))
                DecimalField("Viagem (min)", tm, { tm = it }, Modifier.weight(1f))
            }
        }
        item {
            if (analysis != null) AnalysisCard(analysis)
            else AppCard {
                Text("Preencha valor, km e minutos para ver o veredito.",
                    style = MaterialTheme.typography.bodyMedium, color = TextLo)
            }
        }
    }
}
