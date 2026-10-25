package com.arka.ganhos

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.arka.ganhos.data.AppSettings
import com.arka.ganhos.ui.*
import com.arka.ganhos.ui.screens.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { RotaTheme { AppRoot() } }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val app = ctx.rotaApp
    val scope = rememberCoroutineScope()
    val settings by produceState<AppSettings?>(null) { app.settings.flow.collect { value = it } }
    val history by app.history.items.collectAsState()
    var accessOn by remember { mutableStateOf(isAccessibilityEnabled(ctx)) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val billing = app.billing
    val access by billing.state.collectAsState()
    var showDisclosure by remember { mutableStateOf(false) }
    var sharePromptDone by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        accessOn = isAccessibilityEnabled(ctx)
        billing.refresh()
    }
    val subscribe: () -> Unit = { ctx.findActivity()?.let { billing.launchPurchase(it) } }

    if (showSplash) {
        SplashScreen { showSplash = false }
        return
    }

    val s = settings
    if (s == null) {
        Box(Modifier.fillMaxSize().background(Bg))
        return
    }
    val update: ((AppSettings) -> AppSettings) -> Unit = { f -> scope.launch { app.settings.update(f) } }

    // Antes de abrir a tela de Acessibilidade, o Android/Play exigem um aviso claro com aceite do usuário.
    val requestAccessibility: () -> Unit = {
        if (s.accessConsent) openAccessibilitySettings(ctx) else showDisclosure = true
    }
    if (showDisclosure) {
        AccessDisclosureDialog(
            onAccept = {
                showDisclosure = false
                update { it.copy(accessConsent = true) }
                openAccessibilitySettings(ctx)
            },
            onDismiss = { showDisclosure = false }
        )
    }

    if (!s.onboardingDone) {
        OnboardingScreen(accessOn, onOpenAccessibility = requestAccessibility) { update { it.copy(onboardingDone = true) } }
        return
    }

    // Teste de 60 dias acabou e não há assinatura ativa
    if (!access.hasAccess) {
        PaywallScreen(access, onSubscribe = subscribe, onRestore = { billing.refresh() })
        return
    }

    // Convite de compartilhar: aparece ao abrir o app, a menos que o usuário tenha pedido para não ver mais.
    if (!s.shareHidden && !sharePromptDone && !showDisclosure) {
        SharePromptDialog(
            onDismiss = { sharePromptDone = true },
            onNeverAgain = { sharePromptDone = true; update { it.copy(shareHidden = true) } }
        )
    }

    val tabs = listOf(
        "Início" to Icons.Rounded.Home,
        "Calculadora" to Icons.Rounded.Calculate,
        "Histórico" to Icons.Rounded.History,
        "Ajustes" to Icons.Rounded.Tune
    )
    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Surface1) {
                tabs.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OnGreen, indicatorColor = Green, selectedTextColor = Green,
                            unselectedIconColor = TextLo, unselectedTextColor = TextLo
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).statusBarsPadding()) {
            when (tab) {
                0 -> HomeScreen(s, accessOn, history, access,
                    onMonitoring = { c -> update { it.copy(monitoring = c) } },
                    onEnable = requestAccessibility,
                    onSubscribe = subscribe)
                1 -> CalculatorScreen(s)
                2 -> HistoryScreen(history, onClear = { app.history.clear() }, onDelete = { app.history.remove(it.timestamp) })
                else -> SettingsScreen(s, accessOn, access, update, onOpenAccessibility = requestAccessibility, onSubscribe = subscribe)
            }
        }
    }
}
