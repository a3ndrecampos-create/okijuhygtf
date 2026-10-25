package com.arka.ganhos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arka.ganhos.ui.*

/** Convite para compartilhar o app, mostrado ao abrir. "Não mostrar novamente" guarda a escolha. */
@Composable
fun SharePromptDialog(onDismiss: () -> Unit, onNeverAgain: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("Indique o Arka a outros motoristas", color = TextHi, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Você usa o Arka grátis por 60 dias. Compartilhe com seus colegas: " +
                        "quem instalar também ganha 60 dias grátis.",
                    color = TextLo, style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { shareApp(ctx); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = OnGreen)
                ) { Text("Compartilhar agora", fontWeight = FontWeight.Bold) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Agora não", color = TextHi) } },
        dismissButton = { TextButton(onClick = onNeverAgain) { Text("Não mostrar novamente", color = TextLo) } }
    )
}
