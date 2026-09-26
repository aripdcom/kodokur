import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kodokur core: pure Kotlin/JVM, no Android dependency.
// ZXing decoding, ISBN/ISSN/GTIN validation and hyphenation, content parsing, CSV.
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
    // ZXing core is pure Java (Apache-2.0); no Play Services required.
    api(libs.zxing.core)
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}
