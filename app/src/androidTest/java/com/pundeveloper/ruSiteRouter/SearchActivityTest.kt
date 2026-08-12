/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchActivityTest {

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val testPage = """
        <html><body>
        <a id="ru" href="https://gosuslugi.ru/">ru</a>
        <a id="ext" href="https://example.com/">ext</a>
        <a id="internal" href="https://yandex.ru/pogoda">internal</a>
        </body></html>
    """.trimIndent()

    @Before
    fun setUp() {
        DeviceWake.keepScreenOn()
        RouterSettings.setUseGeosite(context, false)
        RouterSettings.setUseZones(context, true)
        RouterSettings.setSearchEngine(context, RouterSettings.SEARCH_GOOGLE)
        RouterSettings.setRussianBrowser(context, RouterSettings.AUTO)
        RouterSettings.setOtherBrowser(context, RouterSettings.AUTO)

        Intents.init()
        // Внешние браузеры не запускаются по-настоящему — только проверяем intent
        intending(hasAction(Intent.ACTION_VIEW))
            .respondWith(android.app.Instrumentation.ActivityResult(0, null))
    }

    @After
    fun tearDown() {
        Intents.release()
        DeviceWake.restoreScreenTimeout()
    }

    /**
     * Диалог «назначьте браузером по умолчанию» блокирует ввод,
     * поэтому в тестах закрываем его кнопкой «Позже».
     * Диалог показывается в onResume асинхронно — ретраим до появления.
     */
    private fun dismissDefaultBrowserPrompt() {
        if (DefaultBrowserHelper.isDefaultBrowser(context)) return

        val deadline = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(withText("Позже"))
                    .inRoot(RootMatchers.isDialog())
                    .perform(click())
                return
            } catch (_: Exception) {
                Thread.sleep(200)
            }
        }
    }

    /**
     * Проверяет записанный intent маршрутизации напрямую через getIntents():
     * intended() требует window focus, который теряется после stub-startActivity.
     */
    private fun assertRouted(uri: String, packageName: String) {
        val matched = Intents.getIntents().any {
            it.action == Intent.ACTION_VIEW &&
                    it.data?.toString() == uri &&
                    it.`package` == packageName
        }
        assertTrue("Ожидался intent $uri -> $packageName", matched)
    }

    /**
     * Ждёт, пока в WebView загрузится страница выбранного поисковика.
     * Коммит навигации зависит от сети — поллируем с запасом.
     * Возвращает последний URL для диагностики.
     */
    private fun waitForEngineHome(scenario: ActivityScenario<SearchActivity>, engine: SearchEngine): String? {
        val deadline = System.currentTimeMillis() + 8000
        var url: String? = null
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity { url = it.webView.url }
            if (url != null && SearchEngines.isInternal(engine, Uri.parse(url))) return url
            Thread.sleep(200)
        }
        return url
    }

    private fun assertEngineHome(scenario: ActivityScenario<SearchActivity>, engine: SearchEngine) {
        val url = waitForEngineHome(scenario, engine)
        if (url != null && SearchEngines.isInternal(engine, Uri.parse(url))) return

        var loadedId: String? = null
        scenario.onActivity { loadedId = it.loadedEngineId }
        val prefsEngine = RouterSettings.getSearchEngine(context)
        assertTrue(
            "Ожидалась страница поисковика ${engine.label}; url=$url, " +
                    "loadedEngineId=$loadedId, prefs=$prefsEngine",
            false
        )
    }

    @Test
    fun launchLoadsSelectedEngineHome() {
        ActivityScenario.launch(SearchActivity::class.java).use { scenario ->
            dismissDefaultBrowserPrompt()

            assertEngineHome(scenario, SearchEngines.current(context))

            assertTrue(
                "Внешних intent'ов при запуске быть не должно",
                Intents.getIntents().none { it.action == Intent.ACTION_VIEW }
            )
        }
    }

    @Test
    fun engineSwitchAppliesOnReturn() {
        ActivityScenario.launch(SearchActivity::class.java).use { scenario ->
            dismissDefaultBrowserPrompt()

            assertEngineHome(scenario, SearchEngines.current(context))

            RouterSettings.setSearchEngine(context, RouterSettings.SEARCH_YANDEX)

            // Имитируем возврат из настроек: активность уходит из resumed и возвращается
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            dismissDefaultBrowserPrompt()

            assertEngineHome(scenario, SearchEngines.byId(RouterSettings.SEARCH_YANDEX))
        }
    }

    @Test
    fun webViewRussianLinkRoutesToYandex() {
        ActivityScenario.launch(SearchActivity::class.java).use { scenario ->
            dismissDefaultBrowserPrompt()
            scenario.onActivity {
                it.webView.loadDataWithBaseURL(null, testPage, "text/html", "UTF-8", null)
            }

            onWebView().withElement(findElement(Locator.ID, "ru")).perform(webClick())

            assertRouted("https://gosuslugi.ru/", "com.yandex.browser")
        }
    }

    @Test
    fun webViewExternalLinkRoutesToChrome() {
        ActivityScenario.launch(SearchActivity::class.java).use { scenario ->
            dismissDefaultBrowserPrompt()
            scenario.onActivity {
                it.webView.loadDataWithBaseURL(null, testPage, "text/html", "UTF-8", null)
            }

            onWebView().withElement(findElement(Locator.ID, "ext")).perform(webClick())

            assertRouted("https://example.com/", "com.android.chrome")
        }
    }

    @Test
    fun webViewInternalLinkStaysInside() {
        ActivityScenario.launch(SearchActivity::class.java).use { scenario ->
            dismissDefaultBrowserPrompt()
            scenario.onActivity {
                it.webView.loadDataWithBaseURL(null, testPage, "text/html", "UTF-8", null)
            }

            // Ссылка на домен активного поисковика остаётся внутри WebView
            RouterSettings.setSearchEngine(context, RouterSettings.SEARCH_YANDEX)

            onWebView().withElement(findElement(Locator.ID, "internal")).perform(webClick())

            // Навигация осталась внутри WebView: url становится https-адресом поисковика.
            // Коммит навигации зависит от сети — поллируем с запасом.
            val deadline = System.currentTimeMillis() + 8000
            var url: String? = null
            while (System.currentTimeMillis() < deadline) {
                scenario.onActivity { url = it.webView.url }
                if (url?.startsWith("https://") == true) break
                Thread.sleep(200)
            }

            assertTrue(
                "Ожидалась внутренняя навигация, получен: $url",
                url?.startsWith("https://") == true
            )
            assertTrue(
                "Внешних intent'ов быть не должно",
                Intents.getIntents().none { it.action == Intent.ACTION_VIEW }
            )
        }
    }
}
