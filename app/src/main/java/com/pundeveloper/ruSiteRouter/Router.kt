/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.IDN
import java.util.Locale

object Router {

    val yandexPackages = listOf(
        "com.yandex.browser",
        "com.yandex.browser.lite",
        "com.yandex.browser.beta"
    )

    val chromePackages = listOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary"
    )

    val russianSuffixes = listOf(
        ".ru",
        ".xn--p1ai" // .рф
    )

    /**
     * Открывает ссылку в подходящем браузере согласно настройкам.
     * Возвращает true, если удалось запустить хотя бы один браузер.
     */
    fun open(context: Context, uri: Uri): Boolean {
        val host = normalizeHost(uri.host)

        val isRussian = isRussian(context, host)

        val preferred = getPreferredPackages(context, isRussian)
        val fallback = getFallbackPackages(context, isRussian)

        return openInFirstAvailable(context, uri, preferred) ||
                openInFirstAvailable(context, uri, fallback)
    }

    fun normalizeHost(rawHost: String?): String {
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

    fun isRussian(context: Context, host: String): Boolean {
        if (host.isEmpty()) return false

        val include = SiteStore.getSites(context)
        val exclude = SiteStore.getExclude(context)

        if (matchesCustom(host, exclude)) return false
        if (matchesCustom(host, include)) return true

        if (RouterSettings.isUseGeosite(context) && GeositeRepo.rules.matches(host)) {
            return true
        }

        if (RouterSettings.isUseZones(context)) {
            return russianSuffixes.any { host.endsWith(it) }
        }

        return false
    }

    private fun matchesCustom(host: String, rules: Set<String>): Boolean {
        return rules.any { rule ->
            matchesRule(host, rule)
        }
    }

    fun matchesRule(host: String, rule: String): Boolean {
        if (host.isEmpty() || rule.isEmpty()) return false

        return if (rule.startsWith(".")) {
            host.endsWith(rule) || host == rule.removePrefix(".")
        } else {
            host == rule || host.endsWith(".$rule")
        }
    }

    private fun getPreferredPackages(context: Context, isRussian: Boolean): List<String> {
        val selected = if (isRussian) {
            RouterSettings.getRussianBrowser(context)
        } else {
            RouterSettings.getOtherBrowser(context)
        }

        if (selected == RouterSettings.AUTO || selected.isBlank() || selected == context.packageName) {
            return if (isRussian) {
                yandexPackages + chromePackages
            } else {
                chromePackages + yandexPackages
            }
        }

        return listOf(selected)
    }

    private fun getFallbackPackages(context: Context, isRussian: Boolean): List<String> {
        val selected = if (isRussian) {
            RouterSettings.getRussianBrowser(context)
        } else {
            RouterSettings.getOtherBrowser(context)
        }

        val auto = if (isRussian) {
            yandexPackages + chromePackages
        } else {
            chromePackages + yandexPackages
        }

        return auto.filter { it != selected && it != context.packageName }
    }

    private fun openInFirstAvailable(
        context: Context,
        uri: Uri,
        packages: List<String>
    ): Boolean {
        for (packageName in packages) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(packageName)
                context.startActivity(intent)
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
