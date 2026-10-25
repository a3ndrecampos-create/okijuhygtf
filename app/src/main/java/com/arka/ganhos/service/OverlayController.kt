package com.arka.ganhos.service

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.arka.ganhos.core.money
import com.arka.ganhos.core.num
import com.arka.ganhos.data.Analysis
import com.arka.ganhos.data.Verdict

/** Tamanhos de letra do cartão (sp). Para deixar maior ou menor, mexa só aqui. */
private object CardText {
    const val PRICE = 28f        // valor da corrida (topo)
    const val VALUE_MAX = 26f    // números das pastilhas: começam neste tamanho e encolhem se não couberem
    const val VALUE_MIN = 14f
    const val LABEL = 11f        // R$/KM, R$/HORA, LUCRO
    const val PILL = 13f         // ACEITE / AVALIE / RECUSE
    const val REASON = 13f       // linha do motivo e distância
}

/** Cartão flutuante com o semáforo. Usa TYPE_ACCESSIBILITY_OVERLAY: não precisa de permissão extra. */
class OverlayController(private val ctx: Context) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var view: View? = null
    private var hider: Runnable? = null
    private var shownAt = 0L
    private var lifetimeMs = 0L
    private var currentKey: String? = null
    /** Toda view adicionada à janela fica aqui até ser removida: nenhum cartão pode ficar órfão na tela. */
    private val attached = HashSet<View>()

    /** Rede de segurança: se o temporizador do Handler se perder, quem chamar isto fecha o cartão vencido. */
    fun expireIfStale() {
        if (view != null && SystemClock.elapsedRealtime() - shownAt > lifetimeMs + 800) hide()
    }
    val isShowing: Boolean get() = view != null

    /** Esconde/mostra o cartão sem removê-lo (usado para tirar a captura de tela sem ele aparecer). */
    fun setHidden(hidden: Boolean) {
        view?.visibility = if (hidden) View.INVISIBLE else View.VISIBLE
    }

    private fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    private fun argb(v: Verdict): Int = when (v) {
        Verdict.GREEN -> 0xFF22D38A.toInt()
        Verdict.YELLOW -> 0xFFFFC53D.toInt()
        Verdict.RED -> 0xFFFF5470.toInt()
    }

    private fun title(v: Verdict) = when (v) {
        Verdict.GREEN -> "ACEITE"
        Verdict.YELLOW -> "AVALIE"
        Verdict.RED -> "RECUSE"
    }

    /**
     * [key] identifica a oferta. Se o cartão dessa mesma oferta já está na tela, não faz nada:
     * não recria o cartão nem reinicia a contagem. Oferta diferente substitui o cartão anterior
     * (que é removido por completo) e começa uma contagem nova.
     */
    fun show(a: Analysis, seconds: Int, position: Int = 0, key: String = "") {
        if (view != null && key.isNotEmpty() && key == currentKey) return
        removeAll()
        val card = build(a, seconds)
        val lp = WindowManager.LayoutParams(
            ctx.resources.displayMetrics.widthPixels - dp(24),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Sempre na faixa de cima da tela (mapa), longe do cartão da Uber: assim a captura de tela
            // da leitura não pega o semáforo e ele não precisa piscar/sumir. "Centro" e "Base" ficam um pouco mais abaixo.
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = if (position == 0) dp(40) else (ctx.resources.displayMetrics.heightPixels * 0.10f).toInt()
        }
        try {
            wm.addView(card, lp)
        } catch (_: Exception) {
            return
        }
        // Registra o cartão ANTES de qualquer outra coisa: se algo abaixo falhar, ele ainda pode ser removido.
        attached.add(card)
        view = card
        currentKey = key
        shownAt = SystemClock.elapsedRealtime()
        lifetimeMs = seconds * 1000L
        try { animateIn(card) } catch (_: Exception) {}
        // Um temporizador por cartão: um temporizador antigo nunca fecha (nem deixa de fechar) o cartão novo.
        val r = Runnable { if (view === card) hide() else detach(card) }
        hider = r
        handler.postDelayed(r, lifetimeMs)
    }

    fun hide() = removeAll()

    private fun removeAll() {
        hider?.let { handler.removeCallbacks(it) }
        hider = null
        for (v in attached.toList()) detach(v)
        attached.clear()
        view = null
        currentKey = null
    }

    private fun detach(v: View) {
        try {
            v.animate().cancel()
            ((v.tag as? Pair<*, *>)?.first as? View)?.animate()?.cancel()
        } catch (_: Exception) {}
        try {
            wm.removeViewImmediate(v)
        } catch (_: Exception) {
            try { wm.removeView(v) } catch (_: Exception) {}
        }
        attached.remove(v)
    }

    private fun tv(text: String, sp: Float, color: Int, bold: Boolean = false) = TextView(ctx).apply {
        this.text = text
        textSize = sp
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    /** Número grande: usa o maior tamanho que cabe em uma linha, entre [minSp] e [maxSp]. */
    private fun fitTv(text: String, maxSp: Float, minSp: Float, color: Int) = tv(text, maxSp, color, true).apply {
        maxLines = 1
        setAutoSizeTextTypeUniformWithConfiguration(minSp.toInt(), maxSp.toInt(), 1, TypedValue.COMPLEX_UNIT_SP)
    }

    private fun pill(text: String, bg: Int, fg: Int) = tv(text, CardText.PILL, fg, true).apply {
        letterSpacing = 0.12f
        setPadding(dp(12), dp(5), dp(12), dp(5))
        background = GradientDrawable().apply { setColor(bg); cornerRadius = dp(20).toFloat() }
    }

    private fun build(a: Analysis, seconds: Int): View {
        val c = argb(a.verdict)
        val white = 0xFFF8FAFC.toInt()
        val gray = 0xFF94A3B8.toInt()
        val dark = 0xFF04130C.toInt()
        val profitColor = if (a.profit > 0) argb(Verdict.GREEN) else argb(Verdict.RED)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF5121B30.toInt(), 0xF51B2745.toInt())
            ).apply {
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1) + 1, (c and 0x00FFFFFF) or (0xB0 shl 24))
            }
            elevation = dp(12).toFloat()
            setOnClickListener { hide() }
        }

        // Linha 1: selo do veredito + valor da corrida
        val header = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(pill(title(a.verdict), c, dark))
        header.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(tv(money(a.offer.price), CardText.PRICE, white, true))
        root.addView(header)

        // Linha 2: três indicadores em "pastilhas"
        val row = LinearLayout(ctx)
        fun tile(label: String, value: String, valueColor: Int, last: Boolean, weight: Float = 1f) {
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(7), dp(10), dp(8))
                background = GradientDrawable().apply {
                    setColor(0x1AFFFFFF); cornerRadius = dp(14).toFloat()
                }
            }
            col.addView(tv(label, CardText.LABEL, gray, true).apply { letterSpacing = 0.06f })
            col.addView(fitTv(value, CardText.VALUE_MAX, CardText.VALUE_MIN, valueColor), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            if (!last) lp.marginEnd = dp(6)
            row.addView(col, lp)
        }
        if (a.offer.tripKnown) {
            tile("R$/KM", num(a.perKm), white, false)
            tile("R$/HORA", if (a.offer.timeKnown) num(a.perHour, 0) else "—", white, false)
            tile("LUCRO", money(a.profit), profitColor, true, weight = 1.3f)
        } else {
            // inDrive: só a retirada é conhecida
            tile("RETIRADA", num(a.offer.pickupKm, 1) + " km", white, false)
            tile("ATÉ O PASSAGEIRO", a.offer.pickupMin.toString() + " min", white, false)
            tile("NOTA", a.offer.rating?.let { num(it, 1) } ?: "—", white, true)
        }
        root.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })

        // Linha 3: motivo + distância total
        root.addView(tv(
            if (a.offer.tripKnown) a.reasons.first() + "  ·  " + num(a.offer.totalKm, 1) + " km" + (if (a.offer.timeKnown) " · " + a.offer.totalMin + " min" else "")
            else a.reasons.joinToString("  ·  "),
            CardText.REASON, gray
        ), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) })

        // Barrinha que encolhe até o cartão sumir
        val bar = View(ctx).apply {
            background = GradientDrawable().apply { setColor(c); cornerRadius = dp(2).toFloat() }
            pivotX = 0f
        }
        root.addView(bar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(3)
        ).apply { topMargin = dp(10) })
        root.tag = Pair(bar, seconds)
        return root
    }

    /** Entrada suave + contagem regressiva da barrinha. */
    @Suppress("UNCHECKED_CAST")
    private fun animateIn(card: View) {
        card.alpha = 0f
        card.translationY = -dp(14).toFloat()
        card.animate().alpha(1f).translationY(0f).setDuration(180).start()
        val (bar, seconds) = card.tag as Pair<View, Int>
        bar.animate().scaleX(0f).setDuration(seconds * 1000L)
            .setInterpolator(android.view.animation.LinearInterpolator()).start()
    }
}
