package com.rotacerta.app.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

    private val handler = Handler(Looper.getMainLooper())
    private val trailingScan = Runnable { scan() }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !settings.monitoring) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastScan < 300) {
            // Evento caiu na janela de 300 ms: agenda uma leitura final, senão a oferta
            // pode ficar sem leitura quando o app para de emitir eventos (contagem do botão Aceitar).
            handler.removeCallbacks(trailingScan)
            handler.postDelayed(trailingScan, 350)
            return
        }
        scan()
    }

    private fun scan() {
        val s = settings
        if (!s.monitoring) return
        lastScan = SystemClock.elapsedRealtime()

        var offer: RideOffer? = null
        val found = findTargets()
        if (found != null) {
            val texts = ArrayList<String>()
            for (root in found.first) collect(root, texts)
            offer = OfferParser.parse(texts, found.second)
            if (s.diagnostics) {
                DiagnosticsStore.publish(found.first[0].packageName?.toString().orEmpty(), texts, offer)
            }
        } else if (s.diagnostics) {
            val pkgs = windows.mapNotNull { it.root?.packageName?.toString() }.distinct()
            DiagnosticsStore.publish("(nenhum app de corrida lido)", pkgs.map { "janela: $it" }, null)
        }
        if (offer == null) {
            if (!ocrShown && ++misses >= 4) {
                misses = 0
                lastSignature = ""
                overlay.hide()
            }
            // Oferta pode aparecer um instante depois do evento: continua tentando enquanto o app alvo está na tela.
            if (found != null && retries < 6) {
                retries++
                handler.removeCallbacks(trailingScan)
                handler.postDelayed(trailingScan, 400)
            }
            // A Uber pode desenhar o pop-up de um jeito que a acessibilidade não vê: lê a imagem da tela (OCR).
            if (found != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) tryOcr(found.second)
            return
        }
        misses = 0
        retries = 0
        ocrShown = false
        handleOffer(offer, s)
    }

    private fun handleOffer(offer: RideOffer, s: AppSettings) {
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

    // ---------- Plano B: OCR da captura de tela (Android 11+) ----------
    private var ocrBusy = false
    private var lastOcr = 0L
    private var ocrShown = false
    private var ocrMisses = 0
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun tryOcr(source: String) {
        if (ocrBusy || overlay.isShowing) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastOcr < 1500) return
        lastOcr = now
        ocrBusy = true
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    val bmp: Bitmap? = try {
                        val hw = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                        val copy = hw?.copy(Bitmap.Config.ARGB_8888, false)
                        hw?.recycle()
                        copy
                    } catch (e: Exception) { null }
                    try { result.hardwareBuffer.close() } catch (_: Exception) {}
                    if (bmp == null) { ocrBusy = false; return }
                    recognizer.process(InputImage.fromBitmap(bmp, 0))
                        .addOnSuccessListener { t -> bmp.recycle(); onOcrText(t, source) }
                        .addOnFailureListener { bmp.recycle(); ocrBusy = false }
                }

                override fun onFailure(errorCode: Int) { ocrBusy = false }
            })
        } catch (e: Exception) {
            ocrBusy = false
        }
    }

    private fun onOcrText(t: Text, source: String) {
        ocrBusy = false
        val s = settings
        if (!s.monitoring) return
        // Ordem visual (de cima para baixo): retirada vem antes da viagem.
        val lines = t.textBlocks.flatMap { it.lines }
            .sortedWith(compareBy({ (it.boundingBox?.top ?: 0) / 20 }, { it.boundingBox?.left ?: 0 }))
            .map { it.text }
        val offer = OfferParser.parse(lines, source)
        if (s.diagnostics) DiagnosticsStore.publish("com.ubercab.driver (OCR)", lines, offer)
        if (offer != null) {
            ocrShown = true
            ocrMisses = 0
            misses = 0
            handleOffer(offer, s)
        } else if (ocrShown && ++ocrMisses >= 2) {
            // Cartão saiu da tela: libera para a próxima oferta (mesmo valor) ser mostrada de novo.
            ocrShown = false
            ocrMisses = 0
            lastSignature = ""
        }
    }

    private var retries = 0

    /**
     * TODAS as janelas do app alvo (o cartão da oferta costuma ser outra janela, separada do mapa).
     * Antes só a primeira era lida, então o cartão podia nunca entrar na leitura.
     */
    private fun findTargets(): Pair<List<AccessibilityNodeInfo>, String>? {
        val roots = ArrayList<AccessibilityNodeInfo>()
        var source: String? = null
        rootInActiveWindow?.let { r ->
            TARGETS[r.packageName?.toString().orEmpty()]?.let { roots.add(r); source = it }
        }
        for (w in windows) {
            val r = w.root ?: continue
            val name = TARGETS[r.packageName?.toString().orEmpty()] ?: continue
            if (roots.none { it == r }) roots.add(r)
            if (source == null) source = name
        }
        return if (roots.isEmpty()) null else Pair(roots, source ?: "")
    }

    /** Percurso iterativo, sem o corte de 400 textos: o mapa da Uber gera centenas de nós e escondia o cartão. */
    private fun collect(root: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (root == null) return
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(Pair(root, 0))
        var visited = 0
        while (stack.isNotEmpty() && visited < 6000) {
            val (node, depth) = stack.removeLast()
            visited++
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
            if (depth >= 60) continue
            // empilha ao contrário para manter a ordem visual (retirada antes da viagem)
            for (i in node.childCount - 1 downTo 0) {
                node.getChild(i)?.let { stack.addLast(Pair(it, depth + 1)) }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(trailingScan)
        try { recognizer.close() } catch (_: Exception) {}
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
