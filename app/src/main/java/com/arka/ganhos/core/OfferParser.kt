package com.arka.ganhos.core

import com.arka.ganhos.data.RideOffer

/**
 * Extrai valor, tempo/distância e nota a partir dos textos da tela do app de corrida.
 * Os formatos mudam a cada versão dos apps: ajuste as regex abaixo se algum campo não for lido.
 */
object OfferParser {
    private val money = Regex("""R(?:\$|S)\s*(\d{1,3}(?:\.\d{3})*,\d{2}|\d+[.,]\d{2})""")
    private val pair = Regex(
        """(?:(\d+)\s*h(?:oras?)?\s*(?:e\s*)?)?(\d+)\s*(?:m[ií1]n|rn[ií]n)(?:utos?)?[\s(•·|,.:*~\-–]*(\d+(?:[.,]\d+)?)\s*(km|k\s?m|kn|krn|quil[oô]metros?|metros?|m)\b""",
        RegexOption.IGNORE_CASE
    )
    // "R$1,88/km" (99) ou "Est. de R$3,44/km" (Uber): serve para conferir/recuperar os km totais (preço ÷ R$/km)
    private val shownPerKm = Regex("""R(?:\$|S)\s*(\d+[.,]\d{1,2})\s*/\s*km""", RegexOption.IGNORE_CASE)
    // "4,91 (944)", "4,90" ou "4,98 • 124 corridas" (99)
    private val rating = Regex("""^\s*([3-5][.,]\d{1,2})(?:\s*\(\d+\))?(?:\s*[•·|\-–]\s*\d+\s*corridas?.*)?\s*$""")
    /** Botões que só valem se o texto do nó for exatamente isso ("Registro de viagens" não conta). */
    private val exactCues = setOf("viagem", "entrega", "aceitar", "accept")
    private val cues = listOf("aceitar", "accept", "selecionar", "confirmar", "oferecer")

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

    /**
     * @param strict leitura por imagem (OCR): em vez de aceitar uma leitura incompleta/errada (ex.: só uma das
     * duas rotas), recupera os km pelo "R$/km" do próprio card ou espera a próxima leitura.
     */
    fun parse(rawTexts: List<String>, source: String, strict: Boolean = false): RideOffer? {
        val texts = rawTexts.map { normalize(it) }
        val joined = texts.joinToString("  ")
        val low = joined.lowercase()
        val hasCue = cues.any { low.contains(it) } || texts.any { it.trim().lowercase() in exactCues }

        val price = money.findAll(joined)
            .mapNotNull { toDouble(it.groupValues[1]) }
            .filter { it in 2.0..2000.0 }
            .maxOrNull() ?: return null

        val pairs = pair.findAll(joined).map {
            val h = it.groupValues[1].toIntOrNull() ?: 0
            val m = it.groupValues[2].toIntOrNull() ?: 0
            var km = toDouble(it.groupValues[3]) ?: 0.0
            val u = it.groupValues[4].lowercase()
            if (!u.startsWith("k") && !u.startsWith("q")) km /= 1000.0
            Pair(h * 60 + m, km)
        }.toList()

        val shown = shownPerKm.find(joined)?.groupValues?.get(1)?.let { toDouble(it) }?.takeIf { it > 0.0 }
        val expectedKm = shown?.let { price / it }

        val pickup: Pair<Int, Double>
        val trip: Pair<Int, Double>
        when {
            pairs.size >= 2 -> { pickup = pairs[0]; trip = pairs[1] }
            pairs.size == 1 -> {
                val p = pairs[0]
                if (expectedKm != null && expectedKm > p.second * 1.05 + 0.15) {
                    // Só uma rota foi lida: a retirada é o que falta para fechar o total do card.
                    val pk = expectedKm - p.second
                    val pm = if (p.second > 0) Math.round(p.first / p.second * pk).toInt() else 0
                    pickup = Pair(pm, pk); trip = p
                } else {
                    if (strict) return null
                    pickup = Pair(0, 0.0); trip = p
                }
            }
            else -> return null
        }
        // Confere com o R$/km do card: leitura que não fecha (dígito errado, texto cortado) espera a próxima.
        if (strict && expectedKm != null) {
            val total = pickup.second + trip.second
            if (kotlin.math.abs(total - expectedKm) > maxOf(0.5, expectedKm * 0.12)) return null
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
