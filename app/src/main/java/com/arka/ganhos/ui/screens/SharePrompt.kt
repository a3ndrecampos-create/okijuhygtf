package com.arka.ganhos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arka.ganhos.ui.*

/** Convite para compartilhar o app, mostrado ao abrir. "Não mostrar mais" guarda a escolha. */
@Composable
fun SharePromptDialog(onDismiss: () -> Unit, onNeverAgain: () -> Unit) {
    val ctx = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Surface1,
            border = BorderStroke(1.dp, Stroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícone em destaque
                Box(
                    modifier = Modifier.size(64.dp).background(Green.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = Green, modifier = Modifier.size(32.dp))
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "Indique o Arka a outros motoristas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextHi,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    buildAnnotatedString {
                        append("Você usa o Arka grátis por 60 dias. Compartilhe com seus colegas: quem instalar também ganha ")
                        withStyle(SpanStyle(color = Green, fontWeight = FontWeight.Bold)) { append("60 dias grátis") }
                        append(".")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLo,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = { shareApp(ctx); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = OnGreen)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartilhar agora", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Agora não", color = TextHi, maxLines = 1)
                    }
                    TextButton(onClick = onNeverAgain, modifier = Modifier.weight(1f)) {
                        Text("Não mostrar mais", color = TextLo, maxLines = 1)
                    }
                }
            }
        }
    }
}
