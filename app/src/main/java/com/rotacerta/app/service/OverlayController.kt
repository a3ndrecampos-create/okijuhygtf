package com.rotacerta.app.service

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.rotacerta.app.core.money
import com.rotacerta.app.core.num
import com.rotacerta.app.data.Analysis
import com.rotacerta.app.data.Verdict

/** Cartão flutuante com o semáforo. Usa TYPE_ACCESSIBILITY_OVERLAY: não precisa de permissão extra. */
class OverlayController(private val ctx: Context) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var view: View? = null
    private val hider = Runnable { hide() }
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

    fun show(a: Analysis, seconds: Int, position: Int = 0) {
        hide()
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
            animateIn(card)
            view = card
            handler.postDelayed(hider, seconds * 1000L)
        } catch (_: Exception) {
        }
    }

    fun hide() {
        handler.removeCallbacks(hider)
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
    }

    private fun tv(text: String, sp: Float, color: Int, bold: Boolean = false) = TextView(ctx).apply {
        this.text = text
        textSize = sp
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun pill(text: String, bg: Int, fg: Int) = tv(text, 12f, fg, true).apply {
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
        header.addView(tv(money(a.offer.price), 26f, white, true))
        root.addView(header)

        // Linha 2: três indicadores em "pastilhas"
        val row = LinearLayout(ctx)
        fun tile(label: String, value: String, valueColor: Int, last: Boolean) {
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = GradientDrawable().apply {
                    setColor(0x1AFFFFFF); cornerRadius = dp(14).toFloat()
                }
            }
            col.addView(tv(label, 10f, gray, true).apply { letterSpacing = 0.1f })
            col.addView(tv(value, 18f, valueColor, true))
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            if (!last) lp.marginEnd = dp(8)
            row.addView(col, lp)
        }
        tile("R$/KM", num(a.perKm), white, false)
        tile("R$/HORA", num(a.perHour, 0), white, false)
        tile("LUCRO", money(a.profit), profitColor, true)
        root.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })

        // Linha 3: motivo + distância total
        root.addView(tv(
            a.reasons.first() + "  ·  " + num(a.offer.totalKm, 1) + " km · " + a.offer.totalMin + " min",
            12f, gray
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
