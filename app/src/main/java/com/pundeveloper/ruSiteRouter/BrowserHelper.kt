/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */

package com.pundeveloper.ruSiteRouter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import kotlin.collections.iterator

data class BrowserOption(
    val label: String,
    val packageName: String
)

object BrowserHelper {

    private val knownBrowsers = mapOf(
        "com.yandex.browser" to "Яндекс Браузер",
        "com.yandex.browser.lite" to "Яндекс Браузер Лайт",
        "com.yandex.browser.beta" to "Яндекс Браузер Бета",

        "com.android.chrome" to "Chrome",
        "com.chrome.beta" to "Chrome Beta",
        "com.chrome.dev" to "Chrome Dev",
        "com.chrome.canary" to "Chrome Canary",

        "com.huawei.browser" to "Браузер Huawei",
        "com.hihonor.browser" to "Браузер Honor",
        "com.android.browser" to "Браузер",
        "com.google.android.browser" to "Браузер",

        "org.mozilla.firefox" to "Firefox",
        "org.mozilla.firefox_beta" to "Firefox Beta",

        "com.microsoft.emmx" to "Edge",
        "com.brave.browser" to "Brave",
        "com.opera.browser" to "Opera",
        "com.opera.mini.native" to "Opera Mini",
        "com.duckduckgo.mobile.android" to "DuckDuckGo",
        "com.vivaldi.browser" to "Vivaldi",
        "com.kiwibrowser.browser" to "Kiwi Browser",
        "com.sec.android.app.sbrowser" to "Samsung Internet"
    )

    fun getBrowserOptions(context: Context): List<BrowserOption> {
        val pm = context.packageManager
        val ownPackageName = context.packageName

        val found = linkedMapOf<String, BrowserOption>()

        // Сначала добавляем известные браузеры, если они установлены
        for ((packageName, fallbackLabel) in knownBrowsers) {
            if (packageName == ownPackageName) continue

            if (isPackageInstalled(context, packageName)) {
                found[packageName] = BrowserOption(
                    getAppLabel(context, packageName, fallbackLabel),
                    packageName
                )
            }
        }

        // Дополнительно спрашиваем систему, какие приложения могут открывать ссылки
        val intents = listOf(
            createBrowserIntent("https"),
            createBrowserIntent("http")
        )

        for (intent in intents) {
            val resolveInfos = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(
                            PackageManager.MATCH_ALL.toLong()
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                }
            } catch (_: Exception) {
                emptyList()
            }

            for (info in resolveInfos) {
                val packageName = info.activityInfo?.packageName ?: continue

                if (packageName == ownPackageName) continue

                if (!found.containsKey(packageName)) {
                    found[packageName] = BrowserOption(
                        getAppLabel(context, packageName, packageName),
                        packageName
                    )
                }
            }
        }

        val auto = BrowserOption("Авто", RouterSettings.AUTO)

        return listOf(auto) + found.values.sortedBy { it.label.lowercase() }
    }

    private fun createBrowserIntent(scheme: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://example.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getAppLabel(
        context: Context,
        packageName: String,
        fallback: String
    ): String {
        return try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (_: Exception) {
            fallback
        }
    }
}