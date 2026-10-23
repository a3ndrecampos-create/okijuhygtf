package com.rotacerta.app.core

import java.util.Locale

private val BR: Locale = Locale.forLanguageTag("pt-BR")

fun money(v: Double): String = "R$ " + String.format(BR, "%,.2f", v)

fun num(v: Double, d: Int = 2): String = String.format(BR, "%." + d + "f", v)
