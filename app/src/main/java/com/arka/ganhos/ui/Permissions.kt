package com.arka.ganhos.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.arka.ganhos.service.OfferAccessibilityService

fun isAccessibilityEnabled(ctx: Context): Boolean {
    val me = ComponentName(ctx, OfferAccessibilityService::class.java)
    val enabled = Settings.Secure.getString(
        ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').mapNotNull { ComponentName.unflattenFromString(it) }.any { it == me }
}

fun openAccessibilitySettings(ctx: Context) {
    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Android 13+: apps instalados fora da Play Store precisam ter "configurações restritas" liberadas. */
fun openAppInfo(ctx: Context) {
    ctx.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
