package com.aripd.kodokur.platform

import android.content.Context

/** Küçük ayarlar. Şimdilik yalnız Android 8–12'de seçilen dil (bkz. [AppLocale]). */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString("language", AppLocale.SYSTEM) ?: AppLocale.SYSTEM
        set(value) = prefs.edit().putString("language", value).apply()
}
