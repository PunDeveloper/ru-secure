/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var includeContainer: LinearLayout
    private lateinit var excludeContainer: LinearLayout

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SiteStore.ensureDefaultSites(this)
        GeositeUpdater.load(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }

        val title = TextView(this).apply {
            text = "RuSecure"
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        val scroll = ScrollView(this)

        @Suppress("DEPRECATION")
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                0,
                insets.systemWindowInsetTop,
                0,
                insets.systemWindowInsetBottom
            )
            insets
        }

        val useGeosite = CheckBox(this).apply {
            text = "Использовать список geosite (category-ru)"
            isChecked = RouterSettings.isUseGeosite(this@SettingsActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            setOnCheckedChangeListener { _, isChecked ->
                RouterSettings.setUseGeosite(this@SettingsActivity, isChecked)
            }
        }

        val updatedAt = GeositeUpdater.getUpdatedAt(this)
        val statusText = if (updatedAt == 0L) "не обновлялся" else {
            java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(updatedAt))
        }

        val geositeStatus = TextView(this).apply {
            text = "Список обновлён: $statusText"
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        val updateButton = Button(this).apply {
            text = "Обновить список сейчас"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
            setOnClickListener {
                isEnabled = false
                text = "Обновление..."
                Thread {
                    val ok = GeositeUpdater.update(this@SettingsActivity)
                    runOnUiThread {
                        text = if (ok) "Обновлён" else "Ошибка обновления"
                        isEnabled = true
                        val newTime = GeositeUpdater.getUpdatedAt(this@SettingsActivity)
                        geositeStatus.text = "Список обновлён: " +
                                java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(newTime))
                    }
                }.start()
            }
        }

        val useZones = CheckBox(this).apply {
            text = "Считать .ru и .рф российскими"
            isChecked = RouterSettings.isUseZones(this@SettingsActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
            setOnCheckedChangeListener { _, isChecked ->
                RouterSettings.setUseZones(this@SettingsActivity, isChecked)
            }
        }

        val searchEngineLabel = TextView(this).apply {
            text = "Поисковик на главном экране"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        val searchEngineHint = TextView(this).apply {
            text = "Результаты поиска открываются внутри RuSecure, " +
                    "ссылки из них маршрутизируются по вашим правилам."
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        val searchEngineSpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        }

        val engineOptions = SearchEngines.all
        val engineAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            engineOptions.map { it.label }
        )
        engineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        searchEngineSpinner.adapter = engineAdapter
        searchEngineSpinner.setSelection(
            getEnginePosition(engineOptions, RouterSettings.getSearchEngine(this))
        )
        searchEngineSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val option = engineOptions.getOrNull(position) ?: return
                RouterSettings.setSearchEngine(this@SettingsActivity, option.id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val russianLabel = TextView(this).apply {
            text = "Браузер для российских сайтов"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        val russianSpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        }

        val russianOptions = BrowserHelper.getBrowserOptions(this)
        val russianAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            russianOptions.map { it.label }
        )
        russianAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        russianSpinner.adapter = russianAdapter
        russianSpinner.setSelection(
            getSelectedPosition(russianOptions, RouterSettings.getRussianBrowser(this))
        )
        russianSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val option = russianOptions.getOrNull(position) ?: return
                RouterSettings.setRussianBrowser(this@SettingsActivity, option.packageName)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val otherLabel = TextView(this).apply {
            text = "Браузер для остальных сайтов"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        val otherSpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(24) }
        }

        val otherOptions = BrowserHelper.getBrowserOptions(this)
        val otherAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            otherOptions.map { it.label }
        )
        otherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        otherSpinner.adapter = otherAdapter
        otherSpinner.setSelection(
            getSelectedPosition(otherOptions, RouterSettings.getOtherBrowser(this))
        )
        otherSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val option = otherOptions.getOrNull(position) ?: return
                RouterSettings.setOtherBrowser(this@SettingsActivity, option.packageName)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val includeTitle = TextView(this).apply {
            text = "Российские сайты (открывать в Яндексе)"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        val hint = TextView(this).apply {
            text = "Можно вставлять полный URL.\nУдаление — долгим нажатием по строке."
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        val input = EditText(this).apply {
            setHint("https://gosuslugi.ru")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        val excludeCheckbox = CheckBox(this).apply {
            text = "Добавить как исключение (всегда в другом браузере)"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        val addButton = Button(this).apply {
            text = "Добавить"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        }

        includeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val excludeTitle = TextView(this).apply {
            text = "Исключения (всегда в другом браузере)"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
                bottomMargin = dp(8)
            }
        }

        excludeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        addButton.setOnClickListener {
            val raw = input.text.toString()
            val isExclude = excludeCheckbox.isChecked

            if (raw.isBlank()) {
                Toast.makeText(this, "Введите сайт", Toast.LENGTH_SHORT).show()
            } else {
                SiteStore.addSite(this, raw, isExclude)
                input.setText("")
                excludeCheckbox.isChecked = false
                rebuildLists()
            }
        }

        rebuildLists()

        root.addView(title)
        root.addView(useGeosite)
        root.addView(geositeStatus)
        root.addView(updateButton)
        root.addView(useZones)
        root.addView(searchEngineLabel)
        root.addView(searchEngineHint)
        root.addView(searchEngineSpinner)
        root.addView(russianLabel)
        root.addView(russianSpinner)
        root.addView(otherLabel)
        root.addView(otherSpinner)
        root.addView(includeTitle)
        root.addView(hint)
        root.addView(input)
        root.addView(excludeCheckbox)
        root.addView(addButton)
        root.addView(includeContainer)
        root.addView(excludeTitle)
        root.addView(excludeContainer)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun rebuildLists() {
        includeContainer.removeAllViews()

        val includeSites = SiteStore.getSites(this).sorted()
        if (includeSites.isEmpty()) {
            includeContainer.addView(emptyRow())
        } else {
            includeSites.forEach { site ->
                includeContainer.addView(rowView(site, fromExclude = false))
            }
        }

        excludeContainer.removeAllViews()

        val excludeSites = SiteStore.getExclude(this).sorted()
        if (excludeSites.isEmpty()) {
            excludeContainer.addView(emptyRow())
        } else {
            excludeSites.forEach { site ->
                excludeContainer.addView(rowView(site, fromExclude = true))
            }
        }
    }

    private fun rowView(site: String, fromExclude: Boolean): TextView {
        return TextView(this).apply {
            text = site
            textSize = 14f
            setPadding(dp(8), dp(10), dp(8), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }

            setOnLongClickListener {
                SiteStore.removeSite(this@SettingsActivity, site, fromExclude)
                rebuildLists()
                true
            }
        }
    }

    private fun emptyRow(): TextView {
        return TextView(this).apply {
            text = "— пусто —"
            textSize = 13f
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
    }

    private fun getSelectedPosition(
        options: List<BrowserOption>,
        storedPackage: String
    ): Int {
        val index = options.indexOfFirst { it.packageName == storedPackage }
        return if (index >= 0) index else 0
    }

    private fun getEnginePosition(
        options: List<SearchEngine>,
        storedId: String
    ): Int {
        val index = options.indexOfFirst { it.id == storedId }
        return if (index >= 0) index else 0
    }
}