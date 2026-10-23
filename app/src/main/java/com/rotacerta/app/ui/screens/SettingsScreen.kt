package com.rotacerta.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.rotacerta.app.core.money
import com.rotacerta.app.data.DiagnosticsStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.rotacerta.app.data.AppSettings
import com.rotacerta.app.ui.*

@Composable
fun SettingsScreen(s: AppSettings, accessOn: Boolean, onUpdate: ((AppSettings) -> AppSettings) -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val gap = Modifier.height(10.dp)
    val cap = MaterialTheme.typography.bodySmall

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineMedium, color = TextHi) }

        item {
            AppCard {
                Text("Custos do veículo", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Text("Usados para calcular seu lucro real em cada corrida.", style = cap, color = TextLo)
                Spacer(gap)
                NumberField("Preço do combustível (R\$/L)", s.fuelPrice) { v -> onUpdate { it.copy(fuelPrice = v) } }
                Spacer(gap)
                NumberField("Consumo médio (km/L)", s.kmPerLiter) { v -> onUpdate { it.copy(kmPerLiter = v) } }
                Spacer(gap)
                NumberField("Manutenção (R\$/km)", s.maintenancePerKm) { v -> onUpdate { it.copy(maintenancePerKm = v) } }
                Spacer(gap)
                NumberField("Depreciação (R\$/km)", s.depreciationPerKm) { v -> onUpdate { it.copy(depreciationPerKm = v) } }
                Spacer(gap)
                NumberField("Outros custos (R\$/km)", s.otherPerKm) { v -> onUpdate { it.copy(otherPerKm = v) } }
                Spacer(gap)
                Text("Custo total: " + money(s.costPerKm) + " por km", color = Green, fontWeight = FontWeight.Bold)
            }
        }

        item {
            AppCard {
                Text("Suas metas", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Text("Verde quando bate todas as metas; vermelho quando fica bem abaixo.", style = cap, color = TextLo)
                Spacer(gap)
                NumberField("Mínimo R\$/km", s.minPerKm) { v -> onUpdate { it.copy(minPerKm = v) } }
                Spacer(gap)
                NumberField("Mínimo R\$/hora", s.minPerHour) { v -> onUpdate { it.copy(minPerHour = v) } }
                Spacer(gap)
                NumberField("Máx. minutos até o passageiro", s.maxPickupMin.toDouble()) { v ->
                    onUpdate { it.copy(maxPickupMin = v.toInt()) }
                }
                Spacer(gap)
                NumberField("Nota mínima do passageiro", s.minRating) { v -> onUpdate { it.copy(minRating = v) } }
            }
        }

        item {
            AppCard {
                Text("Semáforo na tela", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostrar cartão sobre o app", color = TextLo, modifier = Modifier.weight(1f))
                    Switch(
                        checked = s.overlayEnabled,
                        onCheckedChange = { c -> onUpdate { it.copy(overlayEnabled = c) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = Green, checkedThumbColor = OnGreen)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Posição na tela", style = cap, color = TextLo)
                Spacer(Modifier.height(6.dp))
                Segmented(listOf("Topo", "Centro", "Base"), s.overlayPosition) { i -> onUpdate { it.copy(overlayPosition = i) } }
                Spacer(Modifier.height(10.dp))
                Text("Some após " + s.overlaySeconds + " segundos", style = cap, color = TextLo)
                Slider(
                    value = s.overlaySeconds.toFloat(),
                    onValueChange = { v -> onUpdate { it.copy(overlaySeconds = v.toInt()) } },
                    valueRange = 5f..30f,
                    colors = SliderDefaults.colors(thumbColor = Green, activeTrackColor = Green)
                )
            }
        }

        item {
            AppCard {
                Text("Permissões", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (accessOn) "Acessibilidade: ativada" else "Acessibilidade: desativada",
                    color = if (accessOn) Green else Amber, fontWeight = FontWeight.SemiBold
                )
                Spacer(gap)
                Button(
                    onClick = { openAccessibilitySettings(ctx) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = OnGreen)
                ) { Text("Abrir Acessibilidade") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { openAppInfo(ctx) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Liberar configurações restritas", color = TextHi) }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Se a chave de Acessibilidade estiver cinza (Android 13+): toque em Informações do app, abra o menu ⋮ e escolha Permitir configurações restritas.",
                    style = cap, color = TextLo
                )
            }
        }

        item {
            val last by DiagnosticsStore.last.collectAsState()
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Modo diagnóstico", style = MaterialTheme.typography.titleMedium, color = TextHi)
                        Text("Mostra o que o app consegue ler na tela das corridas. Use para ajustar o leitor.",
                            style = cap, color = TextLo)
                    }
                    Switch(
                        checked = s.diagnostics,
                        onCheckedChange = { c -> onUpdate { it.copy(diagnostics = c) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = Green, checkedThumbColor = OnGreen)
                    )
                }
                if (s.diagnostics) {
                    val c = last
                    Spacer(gap)
                    if (c == null) {
                        Text("Abra o app de corrida e espere uma oferta aparecer.", style = cap, color = TextLo)
                    } else {
                        Text(c.pkg + " · " + SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(c.time)), style = cap, color = TextLo)
                        Text(c.result, fontWeight = FontWeight.SemiBold,
                            color = if (c.result.startsWith("Oferta não")) Amber else Green)
                        Spacer(Modifier.height(8.dp))
                        Text(c.texts.take(40).joinToString("\n"), style = cap, color = TextLo)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { clip.setText(AnnotatedString(c.pkg + "\n" + c.result + "\n\n" + c.texts.joinToString("\n"))) },
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Copiar leitura", color = TextHi) }
                    }
                }
            }
        }

        item {
            AppCard {
                Text("Privacidade", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(Modifier.height(4.dp))
                Text(
                    "O Rota Certa não tem permissão de internet. A leitura da tela só acontece no Uber, 99 e inDrive, e todos os cálculos ficam no seu aparelho. Versão 1.1.0.",
                    style = MaterialTheme.typography.bodyMedium, color = TextLo
                )
            }
        }
    }
}
