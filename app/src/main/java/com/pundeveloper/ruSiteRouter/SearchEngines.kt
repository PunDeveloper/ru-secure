/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import android.content.Context
import android.net.Uri

data class SearchEngine(
    val id: String,
    val label: String,
    // Домашняя страница, которая открывается на главном экране
    val homeUrl: String,
    // Домены, которые остаются внутри WebView (служебные страницы поисковика)
    val internalSuffixes: List<String>
)

object SearchEngines {

    val all = listOf(
        SearchEngine(
            id = RouterSettings.SEARCH_GOOGLE,
            label = "Google",
            homeUrl = "https://www.google.com/",
            internalSuffixes = listOf(
                "google.com",
                "google.ru",
                "googleusercontent.com"
            )
        ),
        SearchEngine(
            id = RouterSettings.SEARCH_YANDEX,
            label = "Яндекс",
            // ya.ru — чистая поисковая страница; yandex.ru редиректит на dzen.ru
            homeUrl = "https://ya.ru/",
            internalSuffixes = listOf(
                "yandex.ru",
                "yandex.com",
                "ya.ru",
                "yandex.net"
            )
        )
    )

    fun byId(id: String): SearchEngine {
        return all.firstOrNull { it.id == id } ?: all.first()
    }

    fun current(context: Context): SearchEngine {
        return byId(RouterSettings.getSearchEngine(context))
    }

    /**
     * true, если ссылка ведёт на служебную страницу поисковика
     * и должна остаться внутри WebView.
     */
    fun isInternal(engine: SearchEngine, uri: Uri): Boolean {
        val host = Router.normalizeHost(uri.host)
        if (host.isEmpty()) return false

        return engine.internalSuffixes.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }

    /**
     * Google оборачивает результаты в редирект вида google.com/url?q=<target>.
     * Возвращает целевой URL, если это такой редирект, иначе null.
     */
    fun unwrapGoogleRedirect(uri: Uri): String? {
        val host = Router.normalizeHost(uri.host)

        val isGoogle = host == "google.com" || host.endsWith(".google.com") ||
                host == "google.ru" || host.endsWith(".google.ru")
        if (!isGoogle) return null

        if (uri.path?.trimStart('/')?.startsWith("url") != true) return null

        return uri.getQueryParameter("q") ?: uri.getQueryParameter("url")
    }
}
