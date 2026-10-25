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
    // Botão "Aceitar por R$17,76" (99 Negocia): é o valor da oferta; os outros R$ do card são contrapropostas
    private val acceptPrice = Regex("""aceitar\s+por\s+R(?:\$|S)\s*(\d{1,3}(?:\.\d{3})*,\d{2}|\d+[.,]\d{2}|\d++(?![.,]\d))""", RegexOption.IGNORE_CASE)
    // "4,91 (944)", "4,90" ou "4,98 • 124 corridas" (99)
    private val rating = Regex("""^\s*([3-5][.,]\d{1,2})(?:\s*\(\d+\))?(?:\s*[•·|\-–]\s*\+?\d+\s*corridas?.*)?\s*$""")
    // inDrive: "5 (5)" (nota inteira e número de corridas entre parênteses)
    private val ratingInt = Regex("""^\s*[^\d\s]{0,2}\s*([3-5])\s*\(\d+\)\s*$""")
    /** Botões que só valem se o texto do nó for exatamente isso ("Registro de viagens" não conta). */
    private val exactCues = setOf("viagem", "entrega", "aceitar", "accept")
    private val cues = listOf("aceitar", "accept", "selecionar", "confirmar", "oferecer")
    /** Textos de telas/rodapés de dinheiro do motorista (saldo, ganhos...): o R$ vizinho NUNCA é o valor da corrida. */
    private val balanceWords = Regex(
        """saldo|ganhos|carteira|extrato|repasse|resgat|saque|sacar|balance|earnings|wallet""",
        RegexOption.IGNORE_CASE
    )
    /** R$ que aparece no card mas não é o preço da corrida (bônus, pedágio, "R$/km"). */
    private val notPriceWords = Regex("""inclu[ií]d|b[oô]nus|bonus|ped[aá]gio""", RegexOption.IGNORE_CASE)
    private val perKmAfter = Regex("""^\s*/\s*km""", RegexOption.IGNORE_CASE)
    private class Money(val node: Int, val value: Double, val soft: Boolean)
    /** Distância máxima (em textos) entre o preço e a primeira rota ("3 min • 1 km") dentro do card da oferta. */
    private const val NEAR_NODES = 8

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

    private val kmValue = Regex("""(\d+(?:[.,]\d+)?)\s*km\b""", RegexOption.IGNORE_CASE)
    private val timeValue = Regex("""(?:(\d+)\s*h\s*)?(\d+)\s*min""", RegexOption.IGNORE_CASE)

    /**
     * iFood entregador: o card traz "R$ 13,83", "Distância total 7,08 km" (rota inteira) e
     * "Tempo aproximado de rota 40 min". Não há retirada separada: a distância total já vale a rota toda.
     */
    private fun parseIfood(texts: List<String>): RideOffer? {
        val joined = texts.joinToString("  ")
        val low = joined.lowercase()
        if (!low.contains("aceitar")) return null
        val price = money.findAll(joined).mapNotNull { toDouble(it.groupValues[1]) }
            .firstOrNull { it in 2.0..2000.0 } ?: return null
        val dIdx = Regex("dist[âa]ncia\\s+total", RegexOption.IGNORE_CASE).find(joined)?.range?.last ?: -1
        val km = (kmValue.find(joined, maxOf(dIdx, 0)) ?: kmValue.find(joined))
            ?.groupValues?.get(1)?.let { toDouble(it) } ?: return null
        val tIdx = Regex("tempo\\s+aproximado", RegexOption.IGNORE_CASE).find(joined)?.range?.last ?: -1
        val t = (timeValue.find(joined, maxOf(tIdx, 0)) ?: return null)
        val min = (t.groupValues[1].toIntOrNull() ?: 0) * 60 + (t.groupValues[2].toIntOrNull() ?: return null)
        if (km <= 0.0 || min <= 0) return null
        return RideOffer(price, 0.0, 0, km, min, null, "iFood")
    }

    /**
     * Keeta Rider: "R$ 18,48", "5,4 km total" e o botão "Pegar(40s)". O card não mostra o tempo da rota:
     * sem ele o semáforo julga só pelo R$/km. Se algum dia o tempo aparecer ("12 min"), ele é usado.
     */
    private fun parseKeeta(texts: List<String>): RideOffer? {
        val joined = texts.joinToString("  ")
        if (!joined.lowercase().contains("pegar")) return null
        val price = money.findAll(joined).mapNotNull { toDouble(it.groupValues[1]) }
            .firstOrNull { it in 2.0..2000.0 } ?: return null
        val km = kmValue.find(joined)?.groupValues?.get(1)?.let { toDouble(it) }?.takeIf { it > 0.0 } ?: return null
        val t = timeValue.find(joined)
        val min = t?.let { (it.groupValues[1].toIntOrNull() ?: 0) * 60 + (it.groupValues[2].toIntOrNull() ?: 0) } ?: 0
        return RideOffer(price, 0.0, 0, km, min, null, "Keeta", true, min > 0)
    }

    /**
     * @param strict leitura por imagem (OCR): em vez de aceitar uma leitura incompleta/errada (ex.: só uma das
     * duas rotas), recupera os km pelo "R$/km" do próprio card ou espera a próxima leitura.
     */
    fun parse(rawTexts: List<String>, source: String, strict: Boolean = false): RideOffer? {
        val texts = rawTexts.map { normalize(it) }
        if (source == "iFood") return parseIfood(texts)
        if (source == "Keeta") return parseKeeta(texts)
        val joined = texts.joinToString("  ")
        val low = joined.lowercase()
        val hasCue = cues.any { low.contains(it) } || texts.any { it.trim().lowercase() in exactCues }

        // Posição de cada texto dentro de "joined": serve para saber a qual texto (nó) cada R$ / rota pertence.
        val offsets = IntArray(texts.size)
        var pos = 0
        for (i in texts.indices) { offsets[i] = pos; pos += texts[i].length + 2 }
        fun nodeAt(p: Int): Int { var n = 0; for (i in offsets.indices) { if (offsets[i] <= p) n = i else break }; return n }

        // Valores em R$ que podem ser o preço da corrida. Fora: saldo/ganhos do app, bônus incluídos, R$/km.
        val items = money.findAll(joined).mapNotNull { m ->
            val v = toDouble(m.groupValues[1]) ?: return@mapNotNull null
            if (v !in 2.0..2000.0) return@mapNotNull null
            val node = nodeAt(m.range.first)
            val after = joined.substring(m.range.last + 1, minOf(joined.length, m.range.last + 12))
            if (perKmAfter.containsMatchIn(after)) return@mapNotNull null
            if (notPriceWords.containsMatchIn(texts[node])) return@mapNotNull null
            // rótulo no próprio texto ("Saldo: R$ 845,20"): com certeza não é a corrida
            if (balanceWords.containsMatchIn(texts[node])) return@mapNotNull null
            // rótulo no texto vizinho ("Saldo total" / "R$ 845,20"): provável saldo, mas pode ser o rótulo do outro valor
            val soft = listOf(node - 1, node + 1).filter { it in texts.indices }
                .any { balanceWords.containsMatchIn(texts[it]) }
            Money(node, v, soft)
        }.toList()

        val pairMatches = pair.findAll(joined).toList()
        val firstPairNode = pairMatches.firstOrNull()?.let { nodeAt(it.range.first) }
        val pairs = pairMatches.map {
            val h = it.groupValues[1].toIntOrNull() ?: 0
            val m = it.groupValues[2].toIntOrNull() ?: 0
            var km = toDouble(it.groupValues[3]) ?: 0.0
            val u = it.groupValues[4].lowercase()
            if (!u.startsWith("k") && !u.startsWith("q")) km /= 1000.0
            Pair(h * 60 + m, km)
        }

        val shown = shownPerKm.find(joined)?.groupValues?.get(1)?.let { toDouble(it) }?.takeIf { it > 0.0 }
        val pairKm = if (pairs.size >= 2) pairs[0].second + pairs[1].second else null

        // Qual R$ é o valor da corrida? 1) o do botão "Aceitar por R$"; 2) o que fecha com o R$/km e os km do card
        // (no "Negocia" da 99 há contrapropostas maiores); 3) o maior valor PERTO das rotas do card
        // (um R$ longe delas costuma ser o saldo/ganhos do app, não a corrida).
        val candidates = items.map { it.value }
        val price = acceptPrice.find(joined)?.groupValues?.get(1)?.let { toDouble(it) }
            ?: (if (shown != null && pairKm != null) {
                candidates.minByOrNull { kotlin.math.abs(it / shown - pairKm) }
                    ?.takeIf { kotlin.math.abs(it / shown - pairKm) <= maxOf(0.5, pairKm * 0.12) }
            } else null)
            ?: run {
                var pool = items
                if (firstPairNode != null) {
                    val near = pool.filter { kotlin.math.abs(it.node - firstPairNode) <= NEAR_NODES }
                    if (near.isNotEmpty()) pool = near
                }
                // valores com rótulo de saldo/ganhos ao lado só valem se não houver outro; nesse caso o mais perto das rotas
                if (pool.any { !it.soft }) pool = pool.filter { !it.soft }
                else if (firstPairNode != null) pool = listOfNotNull(pool.minByOrNull { kotlin.math.abs(it.node - firstPairNode) })
                if (pairKm != null && pairKm > 0.0) {
                    val plausible = pool.filter { it.value / pairKm in 0.3..15.0 }
                    if (plausible.isNotEmpty()) pool = plausible
                }
                pool.maxOfOrNull { it.value }
            }
            ?: return null
        val expectedKm = shown?.let { price / it }

        val pickup: Pair<Int, Double>
        val trip: Pair<Int, Double>
        var tripKnown = true
        when {
            // inDrive: a rota até o destino (verde) fica no alto do mapa e a retirada (azul) embaixo, perto do ponto A.
            // Na ordem de leitura (de cima para baixo) a viagem vem primeiro. Os totais (km e min) não dependem da ordem.
            pairs.size >= 2 && source == "inDrive" -> { trip = pairs[0]; pickup = pairs[1] }
            pairs.size >= 2 -> { pickup = pairs[0]; trip = pairs[1] }
            // inDrive mostra só a retirada (ex.: "7 min. 3,3 km" sobre a rota até o passageiro): viagem desconhecida.
            pairs.size == 1 && source == "inDrive" -> { pickup = pairs[0]; trip = Pair(0, 0.0); tripKnown = false }
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
        // Tela de saldo/ganhos sem botão "Aceitar" de verdade: não é uma oferta.
        val strongCue = low.contains("aceitar") || low.contains("accept") || texts.any { it.trim().lowercase() in exactCues }
        if (!strongCue && balanceWords.containsMatchIn(low)) return null
        // Sem botão "Aceitar" visível na árvore, só aceita se tiver preço e as duas rotas (retirada + viagem).
        if (!hasCue && pairs.size < 2) return null
        if (tripKnown && trip.second <= 0.0 && trip.first <= 0) return null

        val r = texts.firstNotNullOfOrNull { t ->
            (rating.find(t) ?: ratingInt.find(t))?.groupValues?.get(1)?.let { toDouble(it) }
        }?.takeIf { it in 3.0..5.0 }

        return RideOffer(price, pickup.second, pickup.first, trip.second, trip.first, r, source, tripKnown)
    }
}
