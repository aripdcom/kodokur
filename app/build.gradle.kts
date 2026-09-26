import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Sürüm tek kaynaktan yönetilir: release.yml, etiketten türettiği sürümü
// -PappVersion=X.Y.Z olarak geçirir; yerel derlemeler alttaki varsayılanı
// kullanır. versionCode = major*10000 + minor*100 + patch.
val appVersion: String = (project.findProperty("appVersion") as? String) ?: "1.1.0"
val appVersionCode: Int = appVersion.split('.').map { it.toInt() }.let { (major, minor, patch) ->
    require(major < 214 && minor < 100 && patch < 100) { "Geçersiz sürüm: $appVersion" }
    // AGP, versionCode için pozitif tamsayı ister.
    (major * 10_000 + minor * 100 + patch).coerceAtLeast(1)
}

android {
    namespace = "com.aripd.kodokur"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aripd.kodokur"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
    }

    // Release imzası CI'da ortam değişkenleriyle sağlanır (bkz. release.yml).
    // Değişkenler yoksa imzasız release üretilir; debug derlemeler etkilenmez.
    //
    // Boş dizge de "yok" sayılır. Tanımlı ama boş bir değişken `file("")` ile
    // modül kökünü keystore diye gösterirdi; parola da boşken AGP böyle bir
    // yapılandırmayı imzaya hazır saymaz, uyarıp imzasız APK üretir ve derleme
    // yeşil döner. Etiketli koşumda imzasızlığı release.yml ayrıca hata sayar.
    val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
    if (releaseKeystorePath != null) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "kodokur"
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
                    ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Kamera: önizleme + kare çözümleme. Çözme ZXing ile :core'da.
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
