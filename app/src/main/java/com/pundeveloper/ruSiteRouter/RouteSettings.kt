/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (C) 2025 PunDeveloper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.pundeveloper.ruSiteRouter

import android.content.Context
import androidx.core.content.edit

object RouterSettings {

    const val AUTO = "auto"

    private const val PREFS_NAME = "link_router_prefs"
    private const val KEY_USE_ZONES = "use_zones"

    private const val KEY_RUSSIAN_BROWSER = "russian_browser"
    private const val KEY_OTHER_BROWSER = "other_browser"

    private const val KEY_USE_GEOSITE = "use_geosite"

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


    fun isUseGeosite(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_USE_GEOSITE, true)
    }

    fun setUseGeosite(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_USE_GEOSITE, value) }
    }
}