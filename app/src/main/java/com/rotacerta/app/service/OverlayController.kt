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

    private fun build(a: Analysis): View {
        val c = argb(a.verdict)
        val white = 0xFFF1F5F9.toInt()
        val gray = 0xFF94A3B8.toInt()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(0xF2101828.toInt())
                cornerRadius = dp(20).toFloat()
                setStroke(dp(2), c)
            }
            elevation = dp(8).toFloat()
            setOnClickListener { hide() }
        }

        val header = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        val dot = View(ctx).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c) }
        }
        header.addView(dot, LinearLayout.LayoutParams(dp(14), dp(14)))
        header.addView(tv(title(a.verdict), 16f, c, true).apply { setPadding(dp(10), 0, 0, 0) },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(tv(money(a.offer.price), 22f, white, true))
        root.addView(header)

        val row = LinearLayout(ctx).apply { setPadding(0, dp(12), 0, dp(8)) }
        fun metric(label: String, value: String) {
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            col.addView(tv(label, 11f, gray))
            col.addView(tv(value, 17f, white, true))
            row.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        metric("R$/km", num(a.perKm))
        metric("R$/hora", num(a.perHour, 0))
        metric("Lucro", money(a.profit))
        root.addView(row)

        root.addView(tv(
            a.reasons.first() + "  ·  " + num(a.offer.totalKm, 1) + " km · " + a.offer.totalMin + " min",
            12f, gray
        ))
        return root
    }
}
