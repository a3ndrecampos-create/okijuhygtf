package com.arka.ganhos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val kFuel = doublePreferencesKey("fuel")
    private val kKmL = doublePreferencesKey("km_l")
    private val kMaint = doublePreferencesKey("maint")
    private val kDepr = doublePreferencesKey("depr")
    private val kOther = doublePreferencesKey("other")
    private val kGreenKm = doublePreferencesKey("green_km")
    private val kYellowKm = doublePreferencesKey("yellow_km")
    private val kGreenHour = doublePreferencesKey("green_hour")
    private val kYellowHour = doublePreferencesKey("yellow_hour")
    private val kDisabled = stringSetPreferencesKey("disabled_apps")
    private fun kBand(app: String) = stringPreferencesKey("band_$app")
    private val kPickup = intPreferencesKey("max_pickup")
    private val kRating = doublePreferencesKey("min_rating")
    private val kOverlay = booleanPreferencesKey("overlay")
    private val kSeconds = intPreferencesKey("overlay_s")
    private val kPosition = intPreferencesKey("overlay_pos")
    private val kDiag = booleanPreferencesKey("diagnostics")
    private val kMonitoring = booleanPreferencesKey("monitoring")
    private val kOnb = booleanPreferencesKey("onboarding")
    private val kConsent = booleanPreferencesKey("access_consent")
    private val kShareHidden = booleanPreferencesKey("share_hidden")

    val flow: Flow<AppSettings> = context.store.data.map { read(it) }

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            fuelPrice = p[kFuel] ?: d.fuelPrice,
            kmPerLiter = p[kKmL] ?: d.kmPerLiter,
            maintenancePerKm = p[kMaint] ?: d.maintenancePerKm,
            depreciationPerKm = p[kDepr] ?: d.depreciationPerKm,
            otherPerKm = p[kOther] ?: d.otherPerKm,
            greenKm = p[kGreenKm] ?: d.greenKm,
            yellowKm = p[kYellowKm] ?: d.yellowKm,
            greenHour = p[kGreenHour] ?: d.greenHour,
            yellowHour = p[kYellowHour] ?: d.yellowHour,
            maxPickupMin = p[kPickup] ?: d.maxPickupMin,
            minRating = p[kRating] ?: d.minRating,
            overlayEnabled = p[kOverlay] ?: d.overlayEnabled,
            overlaySeconds = p[kSeconds] ?: d.overlaySeconds,
            overlayPosition = p[kPosition] ?: d.overlayPosition,
            diagnostics = p[kDiag] ?: d.diagnostics,
            monitoring = p[kMonitoring] ?: d.monitoring,
            onboardingDone = p[kOnb] ?: d.onboardingDone,
            accessConsent = p[kConsent] ?: d.accessConsent,
            shareHidden = p[kShareHidden] ?: d.shareHidden,
            disabledApps = p[kDisabled] ?: d.disabledApps,
            bands = SUPPORTED_APPS.mapNotNull { app -> p[kBand(app)]?.let { parseBand(it) }?.let { app to it } }.toMap()
        )
    }

    private fun parseBand(raw: String): Band? {
        val v = raw.split(';').mapNotNull { it.toDoubleOrNull() }
        return if (v.size == 4) Band(v[0], v[1], v[2], v[3]) else null
    }

    suspend fun update(f: (AppSettings) -> AppSettings) {
        context.store.edit { p ->
            val n = f(read(p))
            p[kFuel] = n.fuelPrice
            p[kKmL] = n.kmPerLiter
            p[kMaint] = n.maintenancePerKm
            p[kDepr] = n.depreciationPerKm
            p[kOther] = n.otherPerKm
            p[kGreenKm] = n.greenKm
            p[kYellowKm] = n.yellowKm
            p[kGreenHour] = n.greenHour
            p[kYellowHour] = n.yellowHour
            p[kDisabled] = n.disabledApps
            SUPPORTED_APPS.forEach { app ->
                val b = n.bands[app]
                if (b == null) p.remove(kBand(app))
                else p[kBand(app)] = listOf(b.greenKm, b.yellowKm, b.greenHour, b.yellowHour).joinToString(";")
            }
            p[kPickup] = n.maxPickupMin
            p[kRating] = n.minRating
            p[kOverlay] = n.overlayEnabled
            p[kSeconds] = n.overlaySeconds
            p[kPosition] = n.overlayPosition
            p[kDiag] = n.diagnostics
            p[kMonitoring] = n.monitoring
            p[kOnb] = n.onboardingDone
            p[kConsent] = n.accessConsent
            p[kShareHidden] = n.shareHidden
        }
    }
}
