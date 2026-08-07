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

        val uri = intent?.data
        if (uri == null) {
            finish()
            return
        }

        val host = normalizeHost(uri.host)
        val customSites = SiteStore.getSites(this)

        val isRussian = isRussian(host, customSites)

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

    private fun isRussian(host: String, customSites: Set<String>): Boolean {
        if (host.isEmpty()) return false

        if (matchesCustom(host, customSites)) return true

        if (RouterSettings.isUseZones(this)) {
            return russianSuffixes.any { suffix ->
                host.endsWith(suffix)
            }
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