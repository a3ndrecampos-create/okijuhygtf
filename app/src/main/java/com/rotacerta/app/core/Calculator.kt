package com.rotacerta.app.core

import com.rotacerta.app.data.*
import kotlin.math.max

object Calculator {
    fun analyze(offer: RideOffer, s: AppSettings): Analysis {
        val km = offer.totalKm.coerceAtLeast(0.1)
        val hours = offer.totalMin.coerceAtLeast(1) / 60.0
        val cost = km * s.costPerKm
        val profit = offer.price - cost
        val perKm = offer.price / km
        val perHour = offer.price / hours
        val minFare = max(km * s.minPerKm, hours * s.minPerHour)

        val okKm = perKm >= s.minPerKm
        val okHour = perHour >= s.minPerHour
        val okPickup = offer.pickupMin <= s.maxPickupMin
        val rating = offer.rating
        val okRating = rating == null || rating >= s.minRating

        val reasons = mutableListOf<String>()
        if (profit <= 0) reasons += "Não cobre o custo do veículo"
        if (!okKm) reasons += "R$/km abaixo da meta (${num(s.minPerKm)})"
        if (!okHour) reasons += "R$/hora abaixo da meta (${num(s.minPerHour, 0)})"
        if (!okPickup) reasons += "Passageiro longe: ${offer.pickupMin} min (máx. ${s.maxPickupMin})"
        if (!okRating) reasons += "Nota do passageiro baixa (${num(rating ?: 0.0)})"
        if (reasons.isEmpty()) reasons += "Dentro das suas metas"

        val verdict = when {
            profit <= 0 || perKm < s.minPerKm * 0.75 || perHour < s.minPerHour * 0.75 -> Verdict.RED
            okKm && okHour && okPickup && okRating -> Verdict.GREEN
            else -> Verdict.YELLOW
        }
        return Analysis(offer, perKm, perHour, cost, profit, minFare, verdict, reasons)
    }
}
