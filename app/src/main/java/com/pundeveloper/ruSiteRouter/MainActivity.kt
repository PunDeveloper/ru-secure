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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import java.net.IDN
import java.util.Locale

class MainActivity : Activity() {

    private val yandexPackages = listOf(
        "com.yandex.browser",
        "com.yandex.browser.lite",
        "com.yandex.browser.beta"
    )

    private val chromePackages = listOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary"
    )

    private val russianSuffixes = listOf(
        ".ru",
        ".xn--p1ai" // .рф
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SiteStore.ensureDefaultSites(this)
        GeositeUpdater.load(this)

        if (RouterSettings.isUseGeosite(this) && GeositeUpdater.isStale(this)) {
            Thread { GeositeUpdater.update(this) }.start()
        }

        val uri = intent?.data
        if (uri == null) {
            finish()
            return
        }

        val host = normalizeHost(uri.host)

        val isRussian = isRussian(host)

        val preferred = getPreferredPackages(isRussian)
        val fallback = getFallbackPackages(isRussian)

        if (!openInFirstAvailable(uri, preferred)) {
            openInFirstAvailable(uri, fallback)
        }

        finish()
    }

    private fun normalizeHost(rawHost: String?): String {
        var host = rawHost?.trim()?.lowercase(Locale.ROOT) ?: return ""

        if (host.startsWith("www.")) {
            host = host.removePrefix("www.")
        }

        if (host.isEmpty()) return ""

        return try {
            IDN.toASCII(host)
        } catch (_: Exception) {
            host
        }
    }

    private fun isRussian(host: String): Boolean {
        if (host.isEmpty()) return false

        val include = SiteStore.getSites(this)
        val exclude = SiteStore.getExclude(this)

        if (matchesCustom(host, exclude)) return false
        if (matchesCustom(host, include)) return true


        if (RouterSettings.isUseGeosite(this) && GeositeRepo.rules.matches(host)) {
            return true
        }

        if (RouterSettings.isUseZones(this)) {
            return russianSuffixes.any { host.endsWith(it) }
        }

        return false
    }

    private fun matchesCustom(host: String, rules: Set<String>): Boolean {
        return rules.any { rule ->
            matchesRule(host, rule)
        }
    }

    private fun matchesRule(host: String, rule: String): Boolean {
        if (host.isEmpty() || rule.isEmpty()) return false

        return if (rule.startsWith(".")) {
            host.endsWith(rule) || host == rule.removePrefix(".")
        } else {
            host == rule || host.endsWith(".$rule")
        }
    }

    private fun getPreferredPackages(isRussian: Boolean): List<String> {
        val selected = if (isRussian) {
            RouterSettings.getRussianBrowser(this)
        } else {
            RouterSettings.getOtherBrowser(this)
        }

        if (selected == RouterSettings.AUTO || selected.isBlank() || selected == packageName) {
            return if (isRussian) {
                yandexPackages + chromePackages
            } else {
                chromePackages + yandexPackages
            }
        }

        return listOf(selected)
    }

    private fun getFallbackPackages(isRussian: Boolean): List<String> {
        val selected = if (isRussian) {
            RouterSettings.getRussianBrowser(this)
        } else {
            RouterSettings.getOtherBrowser(this)
        }

        val auto = if (isRussian) {
            yandexPackages + chromePackages
        } else {
            chromePackages + yandexPackages
        }

        return auto.filter { it != selected && it != packageName }
    }

    private fun openInFirstAvailable(uri: Uri, packages: List<String>): Boolean {
        for (packageName in packages) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(packageName)
                startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // браузер не установлен
            } catch (_: Exception) {
                // другая ошибка запуска
            }
        }

        return false
    }
}