pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kodokur"

// Çekirdek (çözücü, ISBN/ISSN, içerik ayrıştırma) saf Kotlin/JVM: Android SDK
// olmayan bir makinede de derlenir ve test edilir. Uygulama katmanı ayrı.
include(":core")
include(":app")
