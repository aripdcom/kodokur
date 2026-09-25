package com.aripd.kodokur.platform

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Uygulamanın dili (Reyon'daki yapının aynısı).
 *
 * Varsayılan dil İngilizce'dir (`res/values`); her çeviri kendi `res/values-<dil>`
 * klasöründe durur. Dili telefondan bağımsız seçmek iki yoldan yürür:
 * - Android 13+: sistemin uygulama-dili altyapısı ([LocaleManager]). Kullanıcı dili
 *   Ayarlar'dan da değiştirebilir; doğru kaynak her zaman sistemdir.
 * - Android 8–12: seçim [SettingsStore]'da saklanır, [wrap] etkinliğin taban
 *   bağlamını o dile sarar. appcompat eklemeye gerek kalmaz.
 */
object AppLocale {

    /** "Telefonun dili": kullanıcı özel bir dil seçmemiş. */
    const val SYSTEM = ""

    /**
     * Desteklenen diller: yalnız gerçekten çevirisi olanlar.
     * `res/xml/locales_config.xml` ile birebir aynı olmalı; `tools/check_strings.py`
     * ikisini karşılaştırır.
     */
    val TAGS: List<String> = listOf(
        "en", "tr", "de", "fr", "nl", "es", "pt", "it", "da", "sv", "nb", "fi", "ru", "ar",
    )

    /** Dilin kendi dilindeki adı; seçicide kullanıcı kendi dilini tanısın diye. */
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

    /** Kullanıcının seçtiği dil, ya da [SYSTEM]. */
    fun selected(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            if (locales == null || locales.isEmpty) SYSTEM else normalize(locales[0]) ?: SYSTEM
        } else {
            SettingsStore(context).language
        }

    /** Dili uygular ve saklar; etkinlik yeniden oluşur. */
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

    /** Taban bağlamı seçili dile sarar; `MainActivity.attachBaseContext` çağırır. */
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

    /** `de-DE` → `de`, `pt-BR` → `pt`, `no` → `nb`; desteklenmiyorsa null. */
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
