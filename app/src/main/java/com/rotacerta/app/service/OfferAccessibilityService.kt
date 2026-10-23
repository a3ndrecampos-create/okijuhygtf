package com.rotacerta.app.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.rotacerta.app.core.Calculator
import com.rotacerta.app.core.OfferParser
import com.rotacerta.app.data.AppSettings
import com.rotacerta.app.data.DiagnosticsStore
import com.rotacerta.app.data.RideOffer
import com.rotacerta.app.data.HistoryEntry
import com.rotacerta.app.rotaApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OfferAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile private var settings = AppSettings()
    private lateinit var overlay: OverlayController
    private var lastScan = 0L
    private var lastSignature = ""
    private var misses = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = OverlayController(this)
        scope.launch { rotaApp.settings.flow.collect { settings = it } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val s = settings
        if (event == null || !s.monitoring) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastScan < 300) return
        lastScan = now

        var offer: RideOffer? = null
        val found = findTarget()
        if (found != null) {
            val texts = ArrayList<String>()
            collect(found.first, texts, 0)
            offer = OfferParser.parse(texts, found.second)
            if (s.diagnostics) {
                DiagnosticsStore.publish(found.first.packageName?.toString().orEmpty(), texts, offer)
            }
        }
        if (offer == null) {
            if (++misses >= 4) {
                misses = 0
                lastSignature = ""
                overlay.hide()
            }
            return
        }
        misses = 0

        val sig = offer.price.toString() + "|" + offer.totalKm + "|" + offer.totalMin
        if (sig == lastSignature) return
        lastSignature = sig

        val a = Calculator.analyze(offer, s)
        rotaApp.history.add(
            HistoryEntry(System.currentTimeMillis(), offer.price, offer.totalKm, offer.totalMin,
                a.perKm, a.perHour, a.profit, a.verdict, offer.source)
        )
        if (s.overlayEnabled) overlay.show(a, s.overlaySeconds, s.overlayPosition)
    }

    /** Janela ativa ou, se a oferta aparece como balão sobre outro app (ex.: Waze), qualquer janela do app alvo. */
    private fun findTarget(): Pair<AccessibilityNodeInfo, String>? {
        rootInActiveWindow?.let { r ->
            TARGETS[r.packageName?.toString().orEmpty()]?.let { return Pair(r, it) }
        }
        for (w in windows) {
            val r = w.root ?: continue
            TARGETS[r.packageName?.toString().orEmpty()]?.let { return Pair(r, it) }
        }
        return null
    }

    private fun collect(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (node == null || depth > 40 || out.size > 400) return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        for (i in 0 until node.childCount) collect(node.getChild(i), out, depth + 1)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (::overlay.isInitialized) overlay.hide()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Pacotes lidos. Mantenha em sincronia com res/xml/accessibility_service_config.xml. */
        val TARGETS = mapOf(
            "com.ubercab.driver" to "Uber",
            "com.app99.driver" to "99",
            "com.taxis99.driver" to "99",
            "sinet.startup.inDriver" to "inDrive"
        )
    }
}
