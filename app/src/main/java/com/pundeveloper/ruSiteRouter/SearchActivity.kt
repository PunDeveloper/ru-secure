/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast

class SearchActivity : Activity() {

    companion object {
        private const val STATE_ENGINE_ID = "engine_id"
    }

    internal lateinit var webView: WebView
    private var promptDialog: AlertDialog? = null
    internal var loadedEngineId: String? = null
    private var clearHistoryWhenLoaded = false

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SiteStore.ensureDefaultSites(this)
        GeositeUpdater.load(this)

        if (RouterSettings.isUseGeosite(this) && GeositeUpdater.isStale(this)) {
            Thread { GeositeUpdater.update(this) }.start()
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // На edge-to-edge устройствах контент уходит под статус-бар — добавляем инсеты
        @Suppress("DEPRECATION")
        root.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                0,
                insets.systemWindowInsetTop,
                0,
                insets.systemWindowInsetBottom
            )
            insets
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(dp(8), dp(4), dp(8), 0)
        }

        val settingsButton = ImageButton(this).apply {
            val ripple = android.util.TypedValue()
            theme.resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless, ripple, true
            )
            setBackgroundResource(ripple.resourceId)
            setImageResource(R.drawable.ic_settings)
            contentDescription = "Настройки"
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            setOnClickListener {
                startActivity(Intent(this@SearchActivity, SettingsActivity::class.java))
            }
        }

        headerRow.addView(settingsButton)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            // Без маркера "; wv" поисковики отдают полноценную мобильную версию
            settings.userAgentString = settings.userAgentString?.replace("; wv)", ")")

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    return handleNavigation(request.url)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    // После смены поисковика «назад» не должен вести на старый
                    if (clearHistoryWhenLoaded) {
                        clearHistoryWhenLoaded = false
                        view.clearHistory()
                    }
                }
            }
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
            loadedEngineId = savedInstanceState.getString(STATE_ENGINE_ID)
        } else {
            loadEngineHome(resetHistory = false)
        }

        root.addView(headerRow)
        root.addView(webView)
        setContentView(root)
    }

    private fun loadEngineHome(resetHistory: Boolean) {
        val engine = SearchEngines.current(this)
        loadedEngineId = engine.id
        if (resetHistory) {
            clearHistoryWhenLoaded = true
        }
        webView.loadUrl(engine.homeUrl)
    }

    private fun handleNavigation(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()

        if (scheme != "http" && scheme != "https") {
            openNonHttp(uri, scheme)
            return true
        }

        // Google прячет результаты за редиректом google.com/url?q=<target>
        val unwrapped = SearchEngines.unwrapGoogleRedirect(uri)
        if (unwrapped != null) {
            val target = Uri.parse(unwrapped)
            Router.open(this, target)
            return true
        }

        val engine = SearchEngines.current(this)
        if (SearchEngines.isInternal(engine, uri)) {
            // Служебные страницы поисковика остаются внутри
            return false
        }

        Router.open(this, uri)
        return true
    }

    private fun openNonHttp(uri: Uri, scheme: String?) {
        when (scheme) {
            "tel" -> tryStart(Intent(Intent.ACTION_DIAL, uri))
            "mailto" -> tryStart(Intent(Intent.ACTION_SENDTO, uri))
            "intent" -> handleIntentScheme(uri)
            else -> tryStart(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun handleIntentScheme(uri: Uri) {
        try {
            val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            intent.selector?.let { selector ->
                selector.addCategory(Intent.CATEGORY_BROWSABLE)
                selector.component = null
            }
            startActivity(intent)
        } catch (_: Exception) {
            // Приложение не установлено — игнорируем
        }
    }

    private fun tryStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Не найдено приложение для ссылки", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            // другая ошибка запуска
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
        outState.putString(STATE_ENGINE_ID, loadedEngineId)
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        promptDialog?.dismiss()
        promptDialog = null
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        reloadEngineIfChanged()
        showDefaultBrowserPromptIfNeeded()
    }

    private fun reloadEngineIfChanged() {
        val engine = SearchEngines.current(this)
        // loadedEngineId == null — состояние восстановлено без id, не перезаписываем его
        if (loadedEngineId != null && loadedEngineId != engine.id) {
            loadEngineHome(resetHistory = true)
        }
    }

    private fun showDefaultBrowserPromptIfNeeded() {
        if (DefaultBrowserHelper.isDefaultBrowser(this)) return
        if (promptDialog?.isShowing == true) return

        promptDialog = AlertDialog.Builder(this)
            .setTitle("Назначьте RuSecure браузером по умолчанию")
            .setMessage(
                "Без этого приложение не сможет перехватывать ссылки. " +
                        "Выберите RuSecure в системных настройках."
            )
            .setPositiveButton("Открыть настройки") { _, _ ->
                DefaultBrowserHelper.openDefaultBrowserSettings(this)
            }
            .setNegativeButton("Позже", null)
            .setOnDismissListener { promptDialog = null }
            .show()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
