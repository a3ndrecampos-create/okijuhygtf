package com.rotacerta.app

import android.app.Application
import android.content.Context
import com.rotacerta.app.data.HistoryRepository
import com.rotacerta.app.data.SettingsRepository

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
