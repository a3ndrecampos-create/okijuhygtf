package com.arka.ganhos.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arka.ganhos.billing.AccessState
import com.arka.ganhos.billing.BillingManager

fun openManageSubscription(ctx: Context) {
    val url = BillingManager.manageUrl(ctx.packageName)
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

/** Cartão com a situação do teste grátis ou da assinatura (tela inicial e Ajustes). */
@Composable
fun SubscriptionCard(access: AccessState, onSubscribe: () -> Unit) {
    val ctx = LocalContext.current
    AppCard {
        if (access.subscribed) {
            Text("Assinatura ativa", style = MaterialTheme.typography.titleMedium, color = Green)
            Spacer(Modifier.height(4.dp))
            Text("Obrigado por apoiar o Arka. Você pode cancelar ou trocar a forma de pagamento na Play Store.",
                style = MaterialTheme.typography.bodySmall, color = TextLo)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { openManageSubscription(ctx) },
                shape = RoundedCornerShape(14.dp)
            ) { Text("Gerenciar assinatura", color = TextHi) }
        } else {
            val days = access.trialDaysLeft
            Text(
                if (days == 1) "Teste grátis: resta 1 dia" else "Teste grátis: restam $days dias",
                style = MaterialTheme.typography.titleMedium, color = if (days <= 7) Amber else Green
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Depois do teste, o Arka continua com a assinatura mensal" +
                    (access.price?.let { " de $it" } ?: "") + ". Sem anúncios.",
                style = MaterialTheme.typography.bodySmall, color = TextLo
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSubscribe,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = OnGreen)
            ) { Text("Assinar agora", fontWeight = FontWeight.Bold) }
        }
    }
}

/** Tela cheia mostrada quando o teste acabou e não há assinatura ativa. */
@Composable
fun PaywallScreen(access: AccessState, onSubscribe: () -> Unit, onRestore: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(Bg).systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        ArkaMark(Modifier.size(width = 96.dp, height = 88.dp))
        Spacer(Modifier.height(20.dp))
        Text("Seu teste grátis acabou", style = MaterialTheme.typography.headlineMedium, color = TextHi,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Para continuar vendo o semáforo das corridas, assine o Arka.",
            style = MaterialTheme.typography.bodyMedium, color = TextLo, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        AppCard {
            Text(access.price?.let { "$it por mês" } ?: "Assinatura mensal",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextHi)
            Spacer(Modifier.height(8.dp))
            listOf(
                "Semáforo verde, amarelo e vermelho em cada corrida",
                "Lucro real, R\$/km e R\$/hora",
                "Faixas por aplicativo e histórico completo",
                "Sem anúncios. Cancele quando quiser."
            ).forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, color = TextLo, modifier = Modifier.padding(top = 4.dp)) }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onSubscribe,
            enabled = !access.checking,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = OnGreen)
        ) { Text(if (access.checking) "Verificando assinatura…" else "Assinar agora", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Já assinei: verificar de novo", color = TextHi) }
        access.message?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = Amber, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = { openManageSubscription(ctx) }) { Text("Gerenciar assinatura", color = TextLo) }
        Text(
            "A cobrança é feita pela Google Play e renova todo mês até você cancelar, em Play Store > Pagamentos e assinaturas > Assinaturas.",
            style = MaterialTheme.typography.bodySmall, color = TextLo, textAlign = TextAlign.Center
        )
    }
}

/**
 * Aviso de destaque exigido pela Play Store antes de pedir a permissão de Acessibilidade:
 * diz quais dados são lidos, para quê, e pede aceite explícito.
 */
@Composable
fun AccessDisclosureDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("Permissão de Acessibilidade", color = TextHi, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "O Arka usa o serviço de Acessibilidade do Android para ler, na tela do Uber, da 99 e do inDrive, " +
                        "o valor, o tempo e a distância das ofertas de corrida. Também tira capturas da tela enquanto " +
                        "esses apps estão abertos, só para reconhecer a oferta quando o texto não puder ser lido.",
                    color = TextLo, style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Com esses dados o Arka calcula seu lucro e mostra o semáforo verde, amarelo ou vermelho.",
                    color = TextLo, style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Tudo é processado no seu aparelho. Os dados não são enviados, vendidos nem compartilhados. " +
                        "As capturas de tela não são salvas. No histórico ficam só valor, km e minutos das ofertas analisadas.",
                    color = TextLo, style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "O Arka não aceita, recusa nem toca em nada por você. Você pode desligar quando quiser em Ajustes > Acessibilidade.",
                    color = TextLo, style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = OnGreen)
            ) { Text("Concordo e continuar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Agora não", color = TextLo) } }
    )
}
