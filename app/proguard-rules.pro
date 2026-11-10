# Arka - ProGuard / R8 rules

# Mantém o serviço de acessibilidade (referenciado pelo sistema via reflection)
-keep class com.arka.ganhos.service.OfferAccessibilityService { *; }

# Mantém modelos de dados usados com serialização (DataStore / JSON)
-keep class com.arka.ganhos.data.** { *; }

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# ML Kit (OCR nativo — não ofuscar)
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_latin.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Evita warnings de classes opcionais do Kotlin
-dontwarn kotlin.Unit
-dontwarn kotlinx.coroutines.**
