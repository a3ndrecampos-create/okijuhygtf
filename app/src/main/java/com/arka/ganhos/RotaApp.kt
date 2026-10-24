package com.arka.ganhos

import android.app.Application
import android.content.Context
import com.arka.ganhos.data.HistoryRepository
import com.arka.ganhos.data.SettingsRepository

class RotaApp : Application() {
    lateinit var settings: SettingsRepository
    lateinit var history: HistoryRepository

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        history = HistoryRepository(this)
    }
}

val Context.rotaApp: RotaApp get() = applicationContext as RotaApp
