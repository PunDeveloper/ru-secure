package com.pundeveloper.ruSiteRouter

import android.content.Context
import androidx.core.content.edit

object RouterSettings {

    const val AUTO = "auto"

    private const val PREFS_NAME = "link_router_prefs"
    private const val KEY_USE_ZONES = "use_zones"

    private const val KEY_RUSSIAN_BROWSER = "russian_browser"
    private const val KEY_OTHER_BROWSER = "other_browser"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isUseZones(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_USE_ZONES, true)
    }

    fun setUseZones(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_USE_ZONES, value) }
    }

    fun getRussianBrowser(context: Context): String {
        return prefs(context).getString(KEY_RUSSIAN_BROWSER, AUTO) ?: AUTO
    }

    fun setRussianBrowser(context: Context, value: String) {
        prefs(context).edit { putString(KEY_RUSSIAN_BROWSER, value) }
    }

    fun getOtherBrowser(context: Context): String {
        return prefs(context).getString(KEY_OTHER_BROWSER, AUTO) ?: AUTO
    }

    fun setOtherBrowser(context: Context, value: String) {
        prefs(context).edit { putString(KEY_OTHER_BROWSER, value) }
    }
}