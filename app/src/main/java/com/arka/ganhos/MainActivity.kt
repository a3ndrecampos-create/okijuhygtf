package com.arka.ganhos

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
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { accessOn = isAccessibilityEnabled(ctx) }

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

    if (!s.onboardingDone) {
        OnboardingScreen(accessOn) { update { it.copy(onboardingDone = true) } }
        return
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
                0 -> HomeScreen(s, accessOn, history,
                    onMonitoring = { c -> update { it.copy(monitoring = c) } },
                    onEnable = { openAccessibilitySettings(ctx) })
                1 -> CalculatorScreen(s)
                2 -> HistoryScreen(history) { app.history.clear() }
                else -> SettingsScreen(s, accessOn, update)
            }
        }
    }
}
