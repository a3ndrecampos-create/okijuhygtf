package com.rotacerta.app.data

data class AppSettings(
    val fuelPrice: Double = 6.0,
    val kmPerLiter: Double = 12.0,
    val maintenancePerKm: Double = 0.15,
    val depreciationPerKm: Double = 0.20,
    val otherPerKm: Double = 0.0,
    val minPerKm: Double = 1.8,
    val minPerHour: Double = 30.0,
    val maxPickupMin: Int = 8,
    val minRating: Double = 4.6,
    val overlayEnabled: Boolean = true,
    val overlaySeconds: Int = 12,
    val overlayPosition: Int = 0,
    val diagnostics: Boolean = false,
    val monitoring: Boolean = true,
    val onboardingDone: Boolean = false
) {
    val fuelPerKm: Double get() = if (kmPerLiter > 0) fuelPrice / kmPerLiter else 0.0
    val costPerKm: Double get() = fuelPerKm + maintenancePerKm + depreciationPerKm + otherPerKm
}

enum class Verdict { GREEN, YELLOW, RED }

data class RideOffer(
    val price: Double,
    val pickupKm: Double,
    val pickupMin: Int,
    val tripKm: Double,
    val tripMin: Int,
    val rating: Double? = null,
    val source: String = ""
) {
    val totalKm: Double get() = pickupKm + tripKm
    val totalMin: Int get() = pickupMin + tripMin
}

data class Analysis(
    val offer: RideOffer,
    val perKm: Double,
    val perHour: Double,
    val cost: Double,
    val profit: Double,
    val minFare: Double,
    val verdict: Verdict,
    val reasons: List<String>
)

data class HistoryEntry(
    val timestamp: Long,
    val price: Double,
    val totalKm: Double,
    val totalMin: Int,
    val perKm: Double,
    val perHour: Double,
    val profit: Double,
    val verdict: Verdict,
    val source: String
)
