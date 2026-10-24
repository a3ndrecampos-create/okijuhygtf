package com.arka.ganhos.core

import com.arka.ganhos.data.*
import kotlin.math.max

object Calculator {
    /** As faixas usadas são as do app da oferta (offer.source); sem faixa própria vale o padrão. */
    fun analyze(offer: RideOffer, s: AppSettings): Analysis {
        val band = s.bandFor(offer.source)
        val km = offer.totalKm.coerceAtLeast(0.1)
        val hours = offer.totalMin.coerceAtLeast(1) / 60.0
        val cost = km * s.costPerKm
        val profit = offer.price - cost
        val perKm = offer.price / km
        val perHour = offer.price / hours
        val minFare = max(km * band.greenKm, hours * band.greenHour)

        val greenKmOk = perKm >= band.greenKm
        val greenHourOk = perHour >= band.greenHour
        val redKm = perKm < band.yellowKm
        val redHour = perHour < band.yellowHour
        val okPickup = offer.pickupMin <= s.maxPickupMin
        val rating = offer.rating
        val okRating = rating == null || rating >= s.minRating

        val reasons = mutableListOf<String>()
        if (profit <= 0) reasons += "Não cobre o custo do veículo"
        if (redKm) reasons += "R$/km abaixo do mínimo (${num(band.yellowKm)})"
        else if (!greenKmOk) reasons += "R$/km abaixo da faixa verde (${num(band.greenKm)})"
        if (redHour) reasons += "R$/hora abaixo do mínimo (${num(band.yellowHour, 0)})"
        else if (!greenHourOk) reasons += "R$/hora abaixo da faixa verde (${num(band.greenHour, 0)})"
        if (!okPickup) reasons += "Passageiro longe: ${offer.pickupMin} min (máx. ${s.maxPickupMin})"
        if (!okRating) reasons += "Nota do passageiro baixa (${num(rating ?: 0.0)})"
        if (reasons.isEmpty()) reasons += "Dentro das suas metas"

        val verdict = when {
            profit <= 0 || redKm || redHour -> Verdict.RED
            greenKmOk && greenHourOk && okPickup && okRating -> Verdict.GREEN
            else -> Verdict.YELLOW
        }
        return Analysis(offer, perKm, perHour, cost, profit, minFare, verdict, reasons)
    }
}
