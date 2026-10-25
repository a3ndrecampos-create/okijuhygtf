package com.arka.ganhos.data

data class AppSettings(
    val fuelPrice: Double = 6.0,
    val kmPerLiter: Double = 12.0,
    val maintenancePerKm: Double = 0.15,
    val depreciationPerKm: Double = 0.20,
    val otherPerKm: Double = 0.0,
    // Faixas do semáforo (padrão para todos os apps). Verde = melhor; amarelo = intermediário; abaixo do amarelo = vermelho.
    val greenKm: Double = 2.0,
    val yellowKm: Double = 1.5,
    val greenHour: Double = 36.0,
    val yellowHour: Double = 30.0,
    val maxPickupMin: Int = 8,
    val minRating: Double = 4.6,
    val overlayEnabled: Boolean = true,
    val overlaySeconds: Int = 12,
    val overlayPosition: Int = 0,
    val diagnostics: Boolean = false,
    val monitoring: Boolean = true,
    val onboardingDone: Boolean = false,
    /** O usuário aceitou o aviso de Acessibilidade (exigido pela Play Store antes de pedir a permissão). */
    val accessConsent: Boolean = false,
    /** O usuário pediu para não ver mais o convite de compartilhar ao abrir o app. */
    val shareHidden: Boolean = false,
    /** Apps desligados pelo usuário (não são lidos). */
    val disabledApps: Set<String> = emptySet(),
    /** Faixas próprias de cada app; quem não está aqui usa o padrão. */
    val bands: Map<String, Band> = emptyMap()
) {
    val defaultBand: Band get() = Band(greenKm, yellowKm, greenHour, yellowHour)
    fun bandFor(app: String): Band = bands[app] ?: defaultBand
    fun isEnabled(app: String): Boolean = app !in disabledApps

    /** Altera a faixa de [app]; com app nulo altera o padrão de todos. */
    fun withBand(app: String?, f: (Band) -> Band): AppSettings =
        if (app == null) {
            val b = f(defaultBand)
            copy(greenKm = b.greenKm, yellowKm = b.yellowKm, greenHour = b.greenHour, yellowHour = b.yellowHour)
        } else copy(bands = bands + (app to f(bandFor(app))))

    val fuelPerKm: Double get() = if (kmPerLiter > 0) fuelPrice / kmPerLiter else 0.0
    val costPerKm: Double get() = fuelPerKm + maintenancePerKm + depreciationPerKm + otherPerKm
}

/** Faixas de um app: verde a partir de green*, vermelho abaixo de yellow*. */
data class Band(val greenKm: Double, val yellowKm: Double, val greenHour: Double, val yellowHour: Double) {
    fun withGreenKm(v: Double) = copy(greenKm = v, yellowKm = minOf(yellowKm, v))
    fun withYellowKm(v: Double) = copy(yellowKm = v, greenKm = maxOf(greenKm, v))
    fun withGreenHour(v: Double) = copy(greenHour = v, yellowHour = minOf(yellowHour, v))
    fun withYellowHour(v: Double) = copy(yellowHour = v, greenHour = maxOf(greenHour, v))
}

/** Apps com leitura pronta. Para incluir outro (ex.: Keeta, iFood) acrescente aqui e em OfferAccessibilityService.TARGETS. */
val SUPPORTED_APPS = listOf("Uber", "99", "inDrive")

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
    val source: String,
    val rating: Double? = null
)
