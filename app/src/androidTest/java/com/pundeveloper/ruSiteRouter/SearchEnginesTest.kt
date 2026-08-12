/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchEnginesTest {

    private val google = SearchEngines.byId(RouterSettings.SEARCH_GOOGLE)
    private val yandex = SearchEngines.byId(RouterSettings.SEARCH_YANDEX)

    @Test
    fun homeUrlIsInternal() {
        for (engine in SearchEngines.all) {
            assertTrue(
                "Домашняя страница ${engine.label} должна оставаться внутри WebView",
                SearchEngines.isInternal(engine, Uri.parse(engine.homeUrl))
            )
        }
    }

    @Test
    fun isInternalGoogle() {
        assertTrue(SearchEngines.isInternal(google, Uri.parse("https://www.google.com/search?q=x")))
        assertTrue(SearchEngines.isInternal(google, Uri.parse("https://consent.google.com/terms")))
        assertFalse(SearchEngines.isInternal(google, Uri.parse("https://gismeteo.ru/")))
    }

    @Test
    fun isInternalYandex() {
        assertTrue(SearchEngines.isInternal(yandex, Uri.parse("https://yandex.ru/search/?text=x")))
        assertTrue(SearchEngines.isInternal(yandex, Uri.parse("https://ya.ru/")))
        assertFalse(SearchEngines.isInternal(yandex, Uri.parse("https://example.com/")))
    }

    @Test
    fun unwrapGoogleRedirect() {
        assertEquals(
            "https://example.com/",
            SearchEngines.unwrapGoogleRedirect(
                Uri.parse("https://www.google.com/url?q=https%3A%2F%2Fexample.com%2F")
            )
        )
    }

    @Test
    fun unwrapGoogleRedirectIgnoresPlainUrls() {
        assertNull(
            SearchEngines.unwrapGoogleRedirect(Uri.parse("https://www.google.com/search?q=x"))
        )
        assertNull(
            SearchEngines.unwrapGoogleRedirect(Uri.parse("https://example.com/url?q=x"))
        )
    }
}
