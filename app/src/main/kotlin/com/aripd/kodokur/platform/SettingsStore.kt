package com.aripd.kodokur.platform

import android.content.Context

/** Small settings. For now only the language chosen on Android 8–12 (see [AppLocale]). */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString("language", AppLocale.SYSTEM) ?: AppLocale.SYSTEM
        set(value) = prefs.edit().putString("language", value).apply()
}
