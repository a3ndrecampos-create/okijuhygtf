plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.arka.ganhos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.arka.ganhos"
        minSdk = 26
        targetSdk = 36
        // O workflow do GitHub passa -PversionCode=<número da execução>: cada .aab enviado precisa de um código maior.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0.0"
    }

    // Chave de envio (upload key). Vem de variáveis de ambiente, nunca fica no código.
    // Veja PUBLICAR.md: rode o workflow "Gerar Keystore" uma vez e cadastre os 4 segredos no GitHub.
    val keystorePath: String? = System.getenv("ARKA_KEYSTORE_PATH")
    val hasUploadKey = keystorePath != null && file(keystorePath).exists()
    signingConfigs {
        if (hasUploadKey) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("ARKA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ARKA_KEY_ALIAS")
                keyPassword = System.getenv("ARKA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Sem a chave de envio (testes locais) o release sai assinado com a chave debug e serve só para instalar.
            // A Play Store NÃO aceita esse arquivo: o .aab de publicação precisa vir do workflow com a chave de envio.
            signingConfig = if (hasUploadKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.android.billingclient:billing:8.3.0")
    testImplementation("junit:junit:4.13.2")

    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
