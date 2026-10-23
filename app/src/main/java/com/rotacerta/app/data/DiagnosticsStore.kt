package com.rotacerta.app.data

import com.rotacerta.app.core.money
import com.rotacerta.app.core.num
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Última leitura de tela dos apps de corrida. Fica só na memória e só é preenchida no modo diagnóstico. */
object DiagnosticsStore {
    data class Capture(val time: Long, val pkg: String, val texts: List<String>, val result: String)

    private val _last = MutableStateFlow<Capture?>(null)
    val last: StateFlow<Capture?> = _last.asStateFlow()

    fun publish(pkg: String, texts: List<String>, offer: RideOffer?) {
        val prev = _last.value
        if (prev?.texts == texts) return
        // A captura útil é a do cartão da oferta (botão Aceitar/Viagem). Telas comuns da Uber
        // (mapa, corrida em andamento) não sobrescrevem uma captura de oferta já guardada.
        fun offerLike(t: List<String>) = t.any {
            val x = it.trim().lowercase()
            x == "aceitar" || x == "viagem" || x == "entrega" || x == "selecionar"
        }
        if (offer == null && !offerLike(texts) && prev != null && offerLike(prev.texts)) return
        val result = if (offer == null) "Oferta não reconhecida (" + texts.size + " textos lidos)" else
            money(offer.price) + " · passageiro " + num(offer.pickupKm, 1) + " km/" + offer.pickupMin +
                " min · viagem " + num(offer.tripKm, 1) + " km/" + offer.tripMin + " min"
        _last.value = Capture(System.currentTimeMillis(), pkg, texts.take(120), result)
    }
}
