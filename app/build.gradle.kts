import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// The version has a single source: release.yml derives it from the tag and
// passes it as -PappVersion=X.Y.Z; local builds use the default below.
// versionCode = major*10000 + minor*100 + patch.
val appVersion: String = (project.findProperty("appVersion") as? String) ?: "1.1.0"
val appVersionCode: Int = appVersion.split('.').map { it.toInt() }.let { (major, minor, patch) ->
    require(major < 214 && minor < 100 && patch < 100) { "Invalid version: $appVersion" }
    // AGP requires a positive integer versionCode.
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

    // Release signing comes from environment variables in CI (see release.yml).
    // Without them an unsigned release is built; debug builds are unaffected.
    //
    // An empty string also counts as unset. A defined but empty variable would
    // point `file("")` at the module root as the keystore; with an empty password
    // too, AGP does not consider that config ready to sign, so it warns, builds an
    // unsigned APK and the build stays green. On tagged runs release.yml
    // separately treats an unsigned APK as a failure.
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

    // Camera: preview + frame analysis. Decoding happens in :core with ZXing.
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
