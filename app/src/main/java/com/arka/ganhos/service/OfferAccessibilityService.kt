package com.arka.ganhos.service

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
import com.arka.ganhos.core.Calculator
import com.arka.ganhos.core.OfferParser
import com.arka.ganhos.data.AppSettings
import com.arka.ganhos.data.DiagnosticsStore
import com.arka.ganhos.data.RideOffer
import com.arka.ganhos.data.HistoryEntry
import com.arka.ganhos.rotaApp
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
            if (found != null) ensureOcrLoop()
            return
        }
        misses = 0
        retries = 0
        ocrShown = false
        lastTreeOfferAt = SystemClock.elapsedRealtime()
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
    @Volatile private var ocrBusy = false
    private var lastShotAt = 0L
    private var lastTargetCheck = 0L
    private var cachedSrc: String? = null
    private val shotExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var ocrShown = false
    private var ocrMisses = 0
    private var ocrLoopOn = false
    private var ocrSource = "Uber"
    private var lastTreeOfferAt = 0L
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /**
     * Laço próprio de OCR: roda a cada ~0,4 s enquanto a Uber estiver na tela, sem depender de eventos de
     * acessibilidade (o pop-up quase não gera eventos, por isso a leitura demorava até o fim da contagem).
     */
    private val ocrTick = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            // Qual app está na tela só é conferido 1x por segundo (conferir janelas é caro).
            if (now - lastTargetCheck > 1000 || cachedSrc == null) {
                cachedSrc = if (settings.monitoring && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) targetOnScreen() else null
                lastTargetCheck = now
            }
            val src = cachedSrc
            if (src == null) {
                ocrLoopOn = false
                if (ocrShown) { overlay.hide(); ocrShown = false }
                lastSignature = ""
                return
            }
            ocrSource = src
            // Nova captura assim que a anterior termina (mínimo de ~0,34 s: limite do Android entre capturas).
            // Se a leitura por texto acabou de achar a oferta, não precisa gastar bateria com imagem.
            val gap = if (overlay.isShowing) 600L else 340L
            if (!ocrBusy && now - lastShotAt >= gap && now - lastTreeOfferAt > 4000) runOcr()
            handler.postDelayed(this, 80)
        }
    }

    private fun ensureOcrLoop() {
        if (ocrLoopOn || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        ocrLoopOn = true
        handler.postDelayed(ocrTick, 100)
    }

    /** Nome do app de corrida (ligado) que está na tela agora, ou null. */
    private fun targetOnScreen(): String? {
        val st = settings
        fun name(pkg: String?): String? = TARGETS[pkg.orEmpty()]?.takeIf { st.isEnabled(it) }
        name(rootInActiveWindow?.packageName?.toString())?.let { return it }
        for (w in windows) name(w.root?.packageName?.toString())?.let { return it }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun runOcr() {
        if (ocrBusy) return
        ocrBusy = true
        lastShotAt = SystemClock.elapsedRealtime()
        shoot()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun shoot() {
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, shotExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    val bmp: Bitmap? = try {
                        val hw = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                        val copy = hw?.copy(Bitmap.Config.ARGB_8888, false)
                        hw?.recycle()
                        // O cartão da oferta fica na parte de baixo: descarta os 30% de cima (onde fica o semáforo do Arka)
                        // e a barra de navegação. Depois reduz para ~720 px de largura: o OCR fica bem mais rápido.
                        copy?.let {
                            val top = (it.height * 0.30f).toInt()
                            val h = (it.height * 0.64f).toInt()
                            val cut = Bitmap.createBitmap(it, 0, top, it.width, h)
                            if (cut !== it) it.recycle()
                            val maxW = 720
                            if (cut.width > maxW) {
                                val scaled = Bitmap.createScaledBitmap(cut, maxW, cut.height * maxW / cut.width, true)
                                if (scaled !== cut) cut.recycle()
                                scaled
                            } else cut
                        }
                    } catch (e: Exception) { null }
                    try { result.hardwareBuffer.close() } catch (_: Exception) {}
                    if (bmp == null) { ocrBusy = false; return }
                    recognizer.process(InputImage.fromBitmap(bmp, 0))
                        .addOnSuccessListener { t -> bmp.recycle(); onOcrText(t) }
                        .addOnFailureListener { bmp.recycle(); ocrBusy = false }
                }

                override fun onFailure(errorCode: Int) {
                    ocrBusy = false
                }
            })
        } catch (e: Exception) {
            ocrBusy = false
        }
    }

    private fun onOcrText(t: Text) {
        ocrBusy = false
        val s = settings
        if (!s.monitoring) return
        // Ordem visual (de cima para baixo): retirada vem antes da viagem.
        val lines = t.textBlocks.flatMap { it.lines }
            .sortedWith(compareBy({ (it.boundingBox?.top ?: 0) / 20 }, { it.boundingBox?.left ?: 0 }))
            .map { it.text }
            .filter { it.trim().lowercase() !in OWN_LINES }
        val offer = OfferParser.parse(lines, ocrSource, strict = true)
        if (s.diagnostics) DiagnosticsStore.publish("$ocrSource (OCR)", lines, offer)
        if (offer != null) {
            ocrShown = true
            ocrMisses = 0
            misses = 0
            handleOffer(offer, s)   // oferta nova (assinatura diferente) substitui o cartão anterior
        } else if (++ocrMisses >= 3) {
            // Cartão da Uber saiu da tela (aceita, recusada ou expirada): some o semáforo e libera a próxima.
            ocrMisses = 0
            if (ocrShown) { overlay.hide(); ocrShown = false }
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
        val st = settings
        fun name(r: AccessibilityNodeInfo): String? =
            TARGETS[r.packageName?.toString().orEmpty()]?.takeIf { st.isEnabled(it) }
        rootInActiveWindow?.let { r -> name(r)?.let { roots.add(r); source = it } }
        for (w in windows) {
            val r = w.root ?: continue
            val n = name(r) ?: continue
            if (roots.none { it == r }) roots.add(r)
            if (source == null) source = n
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
        handler.removeCallbacks(ocrTick)
        shotExecutor.shutdown()
        try { recognizer.close() } catch (_: Exception) {}
        if (::overlay.isInitialized) overlay.hide()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Linhas do cartão do próprio Arka, caso apareçam na captura. */
        private val OWN_LINES = setOf("r\$/km", "r\$/hora", "lucro", "aceite", "avalie", "recuse")

        /** Pacotes lidos. Mantenha em sincronia com res/xml/accessibility_service_config.xml. */
        val TARGETS = mapOf(
            "com.ubercab.driver" to "Uber",
            "com.app99.driver" to "99",
            "com.taxis99.driver" to "99",
            "sinet.startup.inDriver" to "inDrive"
        )
    }
}
