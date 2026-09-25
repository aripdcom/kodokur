import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kodokur çekirdeği: saf Kotlin/JVM, Android'e bağımlı değil.
// ZXing ile çözme, ISBN/ISSN/GTIN doğrulama ve tireleme, içerik ayrıştırma, CSV.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // ZXing core saf Java'dır (Apache-2.0); Play Services gerektirmez.
    api(libs.zxing.core)
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}
