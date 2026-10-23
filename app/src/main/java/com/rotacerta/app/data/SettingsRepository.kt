package com.rotacerta.app.data

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
    private val kMinKm = doublePreferencesKey("min_km")
    private val kMinHour = doublePreferencesKey("min_hour")
    private val kPickup = intPreferencesKey("max_pickup")
    private val kRating = doublePreferencesKey("min_rating")
    private val kOverlay = booleanPreferencesKey("overlay")
    private val kSeconds = intPreferencesKey("overlay_s")
    private val kPosition = intPreferencesKey("overlay_pos")
    private val kDiag = booleanPreferencesKey("diagnostics")
    private val kMonitoring = booleanPreferencesKey("monitoring")
    private val kOnb = booleanPreferencesKey("onboarding")

    val flow: Flow<AppSettings> = context.store.data.map { read(it) }

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            fuelPrice = p[kFuel] ?: d.fuelPrice,
            kmPerLiter = p[kKmL] ?: d.kmPerLiter,
            maintenancePerKm = p[kMaint] ?: d.maintenancePerKm,
            depreciationPerKm = p[kDepr] ?: d.depreciationPerKm,
            otherPerKm = p[kOther] ?: d.otherPerKm,
            minPerKm = p[kMinKm] ?: d.minPerKm,
            minPerHour = p[kMinHour] ?: d.minPerHour,
            maxPickupMin = p[kPickup] ?: d.maxPickupMin,
            minRating = p[kRating] ?: d.minRating,
            overlayEnabled = p[kOverlay] ?: d.overlayEnabled,
            overlaySeconds = p[kSeconds] ?: d.overlaySeconds,
            overlayPosition = p[kPosition] ?: d.overlayPosition,
            diagnostics = p[kDiag] ?: d.diagnostics,
            monitoring = p[kMonitoring] ?: d.monitoring,
            onboardingDone = p[kOnb] ?: d.onboardingDone
        )
    }

    suspend fun update(f: (AppSettings) -> AppSettings) {
        context.store.edit { p ->
            val n = f(read(p))
            p[kFuel] = n.fuelPrice
            p[kKmL] = n.kmPerLiter
            p[kMaint] = n.maintenancePerKm
            p[kDepr] = n.depreciationPerKm
            p[kOther] = n.otherPerKm
            p[kMinKm] = n.minPerKm
            p[kMinHour] = n.minPerHour
            p[kPickup] = n.maxPickupMin
            p[kRating] = n.minRating
            p[kOverlay] = n.overlayEnabled
            p[kSeconds] = n.overlaySeconds
            p[kPosition] = n.overlayPosition
            p[kDiag] = n.diagnostics
            p[kMonitoring] = n.monitoring
            p[kOnb] = n.onboardingDone
        }
    }
}
