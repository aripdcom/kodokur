package com.aripd.kodokur.platform

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The app language (same setup as in Reyon).
 *
 * The default language is English (`res/values`); each translation lives in its own
 * `res/values-<lang>` folder. Choosing a language independent of the phone works two ways:
 * - Android 13+: the system per-app language support ([LocaleManager]). The user can
 *   also change it in Settings; the system is always the source of truth.
 * - Android 8–12: the choice is stored in [SettingsStore], and [wrap] wraps the
 *   activity's base context in that language. No need to add appcompat.
 */
object AppLocale {

    /** "Phone language": the user has not picked a specific language. */
    const val SYSTEM = ""

    /**
     * Supported languages: only those that actually have a translation.
     * Must match `res/xml/locales_config.xml` exactly; `tools/check_strings.py`
     * compares the two.
     */
    val TAGS: List<String> = listOf(
        "en", "tr", "de", "fr", "nl", "es", "pt", "it", "da", "sv", "nb", "fi", "ru", "ar",
    )

    /** Each language's name in that language, so users recognize their own in the picker. */
    private val ENDONYMS: Map<String, String> = mapOf(
        "en" to "English",
        "tr" to "Türkçe",
        "de" to "Deutsch",
        "fr" to "Français",
        "nl" to "Nederlands",
        "es" to "Español",
        "pt" to "Português",
        "it" to "Italiano",
        "da" to "Dansk",
        "sv" to "Svenska",
        "nb" to "Norsk bokmål",
        "fi" to "Suomi",
        "ru" to "Русский",
        "ar" to "العربية",
    )

    fun endonym(tag: String): String = ENDONYMS[tag] ?: tag

    /** The language the user picked, or [SYSTEM]. */
    fun selected(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            if (locales == null || locales.isEmpty) SYSTEM else normalize(locales[0]) ?: SYSTEM
        } else {
            SettingsStore(context).language
        }

    /** Applies and stores the language; the activity is recreated. */
    fun choose(context: Context, tag: String) {
        val clean = if (tag in TAGS) tag else SYSTEM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java) ?: return
            manager.applicationLocales =
                if (clean == SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(clean)
        } else {
            val store = SettingsStore(context)
            if (store.language == clean) return
            store.language = clean
            context.findActivity()?.recreate()
        }
    }

    /** Wraps the base context in the chosen language; called from `MainActivity.attachBaseContext`. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = SettingsStore(base).language
        if (tag == SYSTEM || tag !in TAGS) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return ContextWrapper(base.createConfigurationContext(config))
    }

    /** `de-DE` → `de`, `pt-BR` → `pt`, `no` → `nb`; null if unsupported. */
    fun normalize(locale: Locale): String? {
        val language = locale.language.lowercase(Locale.ROOT)
        val canonical = if (language == "no") "nb" else language
        return canonical.takeIf { it in TAGS }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
