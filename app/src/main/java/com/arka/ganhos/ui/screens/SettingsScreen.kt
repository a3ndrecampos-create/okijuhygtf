package com.arka.ganhos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.text.style.TextAlign
import com.arka.ganhos.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import com.arka.ganhos.data.SUPPORTED_APPS
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.arka.ganhos.billing.AccessState
import com.arka.ganhos.core.money
import com.arka.ganhos.data.DiagnosticsStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.arka.ganhos.data.AppSettings
import com.arka.ganhos.ui.*

/** Link da Play Store. Deixe vazio até o app ser publicado: enquanto estiver vazio, o texto vai sem link. */
const val PLAY_STORE_URL = ""

/** Endereço da política de privacidade (publique docs/privacidade.html e cole o link aqui). Vazio = botão escondido. */
const val PRIVACY_URL = ""

fun shareApp(ctx: android.content.Context) {
    val name = ctx.getString(R.string.app_name)
    val text = "Conheça o $name: lê as ofertas da Uber, 99 e inDrive e mostra com um semáforo " +
        "(verde, amarelo ou vermelho) se a corrida vale a pena." +
        if (PLAY_STORE_URL.isNotBlank()) "\n" + PLAY_STORE_URL else ""
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(send, "Compartilhar aplicativo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun SettingsScreen(
    s: AppSettings,
    accessOn: Boolean,
    access: AccessState,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onOpenAccessibility: () -> Unit,
    onSubscribe: () -> Unit
) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val versionName = remember { try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" } }
    val gap = Modifier.height(10.dp)
    val cap = MaterialTheme.typography.bodySmall

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineMedium, color = TextHi) }

        if (access.subscribed) item { SubscriptionCard(access, onSubscribe) }

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
                Text("Aplicativos monitorados", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Text("Escolha de quais apps o semáforo analisa as ofertas.", style = cap, color = TextLo)
                SUPPORTED_APPS.forEach { app ->
                    val on = s.isEnabled(app)
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(app, color = TextHi, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(if (on) "Monitorando ofertas" else "Desligado", style = cap,
                                color = if (on) Green else TextLo)
                        }
                        Switch(
                            checked = on,
                            onCheckedChange = { c ->
                                onUpdate { it.copy(disabledApps = if (c) it.disabledApps - app else it.disabledApps + app) }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Green, checkedThumbColor = OnGreen)
                        )
                    }
                }
            }
        }

        item {
            var filter by remember { mutableIntStateOf(0) }
            val app: String? = if (filter == 0) null else SUPPORTED_APPS[filter - 1]
            val band = if (app == null) s.defaultBand else s.bandFor(app)
            val own = app != null && s.bands.containsKey(app)
            val who = app ?: "Todos"
            AppCard {
                Text("Faixas do semáforo", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Text(
                    "Verde é melhor, amarelo é intermediário e abaixo do amarelo fica vermelho. Cada app pode ter suas próprias faixas.",
                    style = cap, color = TextLo
                )
                Spacer(gap)
                Text("Filtro do semáforo", style = cap, color = TextLo)
                Spacer(Modifier.height(6.dp))
                Segmented(listOf("Todos") + SUPPORTED_APPS, filter) { filter = it }
                Spacer(gap)
                Text(if (app == null) "Padrão para todos os apps" else app,
                    color = Green, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        app == null -> "Vale para os apps sem ajuste próprio."
                        own -> "Ajuste próprio do $app."
                        else -> "Usando o padrão. Mexer em qualquer faixa cria um ajuste só para o $app."
                    }, style = cap, color = TextLo
                )
                BandControl("Faixa verde R\$/km ($who)", band.greenKm, 0.5, 6.0, 0.05) { v ->
                    onUpdate { it.withBand(app) { b -> b.withGreenKm(v) } }
                }
                BandControl("Faixa amarela R\$/km ($who)", band.yellowKm, 0.5, 6.0, 0.05) { v ->
                    onUpdate { it.withBand(app) { b -> b.withYellowKm(v) } }
                }
                BandControl("Faixa verde R\$/h ($who)", band.greenHour, 10.0, 120.0, 1.0) { v ->
                    onUpdate { it.withBand(app) { b -> b.withGreenHour(v) } }
                }
                BandControl("Faixa amarela R\$/h ($who)", band.yellowHour, 10.0, 120.0, 1.0) { v ->
                    onUpdate { it.withBand(app) { b -> b.withYellowHour(v) } }
                }
                if (own && app != null) {
                    Spacer(gap)
                    OutlinedButton(
                        onClick = { onUpdate { it.copy(bands = it.bands - app) } },
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Usar o padrão no $app", color = TextHi) }
                }
            }
        }

        item {
            AppCard {
                Text("Outras metas", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Text("Valem para todos os apps. Com passageiro longe ou nota baixa o semáforo não fica verde.", style = cap, color = TextLo)
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
                    onClick = onOpenAccessibility,
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
                Text(
                    "Ajude nosso app a crescer!", style = MaterialTheme.typography.titleLarge, color = TextHi,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Gostou do aplicativo? Compartilhe com seus amigos e ajude mais pessoas a conhecerem e " +
                        "aproveitarem essa experiência. Cada indicação faz a diferença!",
                    style = MaterialTheme.typography.bodyMedium, color = TextLo,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { shareApp(ctx) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = OnGreen)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Compartilhar aplicativo", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            AppCard {
                Text("Privacidade", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Spacer(Modifier.height(4.dp))
                Text(
                    "A leitura da tela só acontece no Uber, na 99 e no inDrive. Todos os cálculos ficam no seu aparelho e o Arka não envia suas corridas para nenhum servidor. Versão " + versionName + ".",
                    style = MaterialTheme.typography.bodyMedium, color = TextLo
                )
                if (PRIVACY_URL.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            try { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(PRIVACY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Política de privacidade", color = TextHi) }
                }
            }
        }
    }
}
