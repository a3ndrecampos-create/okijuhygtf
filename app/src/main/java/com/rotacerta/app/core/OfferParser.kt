package com.rotacerta.app.core

import com.rotacerta.app.data.RideOffer

/**
 * Extrai valor, tempo/distância e nota a partir dos textos da tela do app de corrida.
 * Os formatos mudam a cada versão dos apps: ajuste as regex abaixo se algum campo não for lido.
 */
object OfferParser {
    private val money = Regex("""R\$\s*(\d{1,3}(?:\.\d{3})*,\d{2}|\d+[.,]\d{2})""")
    private val pair = Regex(
        """(?:(\d+)\s*h(?:oras?)?\s*(?:e\s*)?)?(\d+)\s*min(?:utos?)?[\s(•·|,\-–]*(\d+(?:[.,]\d+)?)\s*(km|quil[oô]metros?|metros?|m)\b""",
        RegexOption.IGNORE_CASE
    )
    private val rating = Regex("""^\s*([3-5][.,]\d{1,2})(?:\s*\(\d+\))?\s*$""")
    private val cues = listOf("aceitar", "viagem", "accept", "selecionar", "confirmar", "oferecer")

    /**
     * Os apps usam espaço não separável (U+00A0, U+202F), marcas invisíveis e bullets variados.
     * O \s do Java NÃO casa com eles, então "R$\u00A010,32" e "3\u00A0min" não eram lidos.
     */
    fun normalize(raw: String): String = raw
        .replace(Regex("[\\u00A0\\u1680\\u2000-\\u200A\\u202F\\u205F\\u3000]"), " ")
        .replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2069\\uFEFF]"), "")
        .replace(Regex("[\\u2022\\u2027\\u00B7\\u25CF\\u2219]"), "•")

    private fun toDouble(raw: String): Double? {
        val s = if (raw.contains(',')) raw.replace(".", "").replace(',', '.') else raw
        return s.toDoubleOrNull()
    }

    fun parse(rawTexts: List<String>, source: String): RideOffer? {
        val texts = rawTexts.map { normalize(it) }
        val joined = texts.joinToString("  ")
        val low = joined.lowercase()
        val hasCue = cues.any { low.contains(it) }

        val price = money.findAll(joined)
            .mapNotNull { toDouble(it.groupValues[1]) }
            .filter { it in 2.0..2000.0 }
            .maxOrNull() ?: return null

        val pairs = pair.findAll(joined).map {
            val h = it.groupValues[1].toIntOrNull() ?: 0
            val m = it.groupValues[2].toIntOrNull() ?: 0
            var km = toDouble(it.groupValues[3]) ?: 0.0
            if (!it.groupValues[4].startsWith("k", true) && !it.groupValues[4].startsWith("q", true)) km /= 1000.0
            Pair(h * 60 + m, km)
        }.toList()

        val pickup: Pair<Int, Double>
        val trip: Pair<Int, Double>
        when {
            pairs.size >= 2 -> { pickup = pairs[0]; trip = pairs[1] }
            pairs.size == 1 -> { pickup = Pair(0, 0.0); trip = pairs[0] }
            else -> return null
        }
        // Sem botão "Aceitar" visível na árvore, só aceita se tiver preço e as duas rotas (retirada + viagem).
        if (!hasCue && pairs.size < 2) return null
        if (trip.second <= 0.0 && trip.first <= 0) return null

        val r = texts.firstNotNullOfOrNull { t ->
            rating.find(t)?.groupValues?.get(1)?.let { toDouble(it) }
        }?.takeIf { it in 3.0..5.0 }

        return RideOffer(price, pickup.second, pickup.first, trip.second, trip.first, r, source)
    }
}
