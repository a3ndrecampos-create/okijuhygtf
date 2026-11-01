package com.arka.ganhos.service

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    const val PRICE = 24f        // valor da corrida (topo)
    const val VALUE_MAX = 20f    // números das pastilhas: começam neste tamanho e encolhem se não couberem
    const val VALUE_MIN = 12f
    const val LABEL = 10f        // R$/KM, R$/HORA, LUCRO
    const val PILL = 12f         // ACEITE / AVALIE / RECUSE
    const val REASON = 12f       // linha do motivo e distância
}

/**
 * Cartão flutuante com o semáforo. Usa TYPE_ACCESSIBILITY_OVERLAY: não precisa de permissão extra.
 * O cartão fica na tela até a pessoa tocar no X ou até chegar uma nova oferta (que o substitui).
 */
class OverlayController(private val ctx: Context) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null
    private var currentKey: String? = null
    /** Toda view adicionada à janela fica aqui até ser removida: nenhum cartão pode ficar órfão na tela. */
    private val attached = HashSet<View>()

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
     * [key] identifica a oferta. Se o cartão dessa mesma oferta já está na tela, não faz nada.
     * Oferta diferente substitui o cartão anterior (que é removido por completo).
     * [seconds] não é mais usado: o cartão não some sozinho, só pelo X ou por uma nova oferta.
     */
    @Suppress("UNUSED_PARAMETER")
    fun show(a: Analysis, seconds: Int, position: Int = 0, key: String = "") {
        if (view != null && key.isNotEmpty() && key == currentKey) return
        removeAll()
        val card = build(a)
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
        try { animateIn(card) } catch (_: Exception) {}
    }

    fun hide() = removeAll()

    private fun removeAll() {
        for (v in attached.toList()) detach(v)
        attached.clear()
        view = null
        currentKey = null
    }

    private fun detach(v: View) {
        try { v.animate().cancel() } catch (_: Exception) {}
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
        setPadding(dp(10), dp(3), dp(10), dp(3))
        background = GradientDrawable().apply { setColor(bg); cornerRadius = dp(20).toFloat() }
    }

    private fun build(a: Analysis): View {
        val c = argb(a.verdict)
        val white = 0xFFF8FAFC.toInt()
        val gray = 0xFF94A3B8.toInt()
        val dark = 0xFF04130C.toInt()
        val profitColor = if (a.profit > 0) argb(Verdict.GREEN) else argb(Verdict.RED)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(8), dp(8))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF5121B30.toInt(), 0xF51B2745.toInt())
            ).apply {
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1) + 1, (c and 0x00FFFFFF) or (0xB0 shl 24))
            }
            elevation = dp(12).toFloat()
        }

        // Linha 1: selo do veredito + valor da corrida + X para fechar
        val header = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(pill(title(a.verdict), c, dark))
        header.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(tv(money(a.offer.price), CardText.PRICE, white, true))
        val close = tv("✕", 15f, gray, true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(0x1AFFFFFF); cornerRadius = dp(16).toFloat() }
            contentDescription = "Fechar"
            setOnClickListener { hide() }
        }
        header.addView(close, LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(10) })
        root.addView(header)

        // Linha 2: três indicadores em "pastilhas" (com folga à direita para alinhar com o X)
        val row = LinearLayout(ctx)
        fun tile(label: String, value: String, valueColor: Int, last: Boolean, weight: Float = 1f) {
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(4), dp(8), dp(5))
                background = GradientDrawable().apply {
                    setColor(0x1AFFFFFF); cornerRadius = dp(12).toFloat()
                }
            }
            col.addView(tv(label, CardText.LABEL, gray, true).apply { letterSpacing = 0.06f })
            col.addView(fitTv(value, CardText.VALUE_MAX, CardText.VALUE_MIN, valueColor), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            if (!last) lp.marginEnd = dp(5)
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
        ).apply { topMargin = dp(6); marginEnd = dp(4) })

        // Linha 3: motivo + distância total
        root.addView(tv(
            if (a.offer.tripKnown) a.reasons.first() + "  ·  " + num(a.offer.totalKm, 1) + " km" + (if (a.offer.timeKnown) " · " + a.offer.totalMin + " min" else "")
            else a.reasons.joinToString("  ·  "),
            CardText.REASON, gray
        ), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(5) })

        return root
    }

    /** Entrada suave. */
    private fun animateIn(card: View) {
        card.alpha = 0f
        card.translationY = -dp(14).toFloat()
        card.animate().alpha(1f).translationY(0f).setDuration(150).start()
    }
}
