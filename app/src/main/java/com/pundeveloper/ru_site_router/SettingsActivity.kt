/*
RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
остальные — в браузере по выбору.
Copyright (c) 2025 PunDeveloper
SPDX-License-Identifier: MIT
*/
package com.pundeveloper.ru_site_router

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var modeHintView: TextView
    private lateinit var listsStatusView: TextView
    private lateinit var updateButton: Button
    private lateinit var listsEntryCounts: TextView

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun secondaryColor(): Int {
        val tv = TypedValue()
        return if (theme.resolveAttribute(android.R.attr.textColorSecondary, tv, true)) {
            tv.data
        } else {
            0xFF808080.toInt()
        }
    }

    private fun verticalParams(bottomMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { this.bottomMargin = bottomMargin }

    private fun sectionHeader(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(secondaryColor())
        layoutParams = verticalParams(dp(8)).apply { topMargin = dp(20) }
    }

    private fun divider(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        )
        setBackgroundColor(0x33808080)
    }

    private fun hintView(text: String, bottomMargin: Int = dp(8)): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(secondaryColor())
            layoutParams = verticalParams(bottomMargin)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeositeUpdater.load(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        val scroll = ScrollView(this)
        @Suppress("DEPRECATION")
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            insets
        }

        val title = TextView(this).apply {
            text = "RuSecure"
            textSize = 20f
            layoutParams = verticalParams(dp(4))
        }
        root.addView(title)

        // === Секция: режим определения российских сайтов ===
        root.addView(sectionHeader("Какие сайты считать российскими"))

        val modeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            layoutParams = verticalParams(dp(8))
        }
        val mincifraRadio = RadioButton(this).apply {
            text = "Только сайты с сертификатом Минцифры"
            layoutParams = verticalParams(dp(4))
        }
        val allRuRadio = RadioButton(this).apply {
            text = "Все российские сайты"
            layoutParams = verticalParams(dp(4))
        }
        modeGroup.addView(mincifraRadio)
        modeGroup.addView(allRuRadio)
        root.addView(modeGroup)

        modeHintView = hintView("")
        root.addView(modeHintView)
        root.addView(
            hintView(
                "Ваши сайты и исключения учитываются в любом режиме " +
                        "и имеют приоритет над списками.",
                dp(8)
            )
        )

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = verticalParams(dp(4))
        }
        listsStatusView = TextView(this).apply {
            textSize = 13f
            setTextColor(secondaryColor())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        updateButton = Button(this).apply {
            text = "Обновить"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { updateListsNow() }
        }
        statusRow.addView(listsStatusView)
        statusRow.addView(updateButton)
        root.addView(statusRow)

        // Миграция старых настроек: если был включён v2fly или зоны — режим "все российские"
        val initialAllRu = RouterSettings.isUseV2fly(this) || RouterSettings.isUseZones(this)
        if (initialAllRu) allRuRadio.isChecked = true else mincifraRadio.isChecked = true
        modeGroup.setOnCheckedChangeListener { _, checkedId ->
            val allRu = checkedId == allRuRadio.id
            RouterSettings.setUseGeosite(this@SettingsActivity, true)
            RouterSettings.setUseV2fly(this@SettingsActivity, allRu)
            RouterSettings.setUseZones(this@SettingsActivity, allRu)
            modeHintView.text = modeHint(allRu)
        }
        modeHintView.text = modeHint(initialAllRu)

        // === Секция: Браузеры ===
        root.addView(divider())
        root.addView(sectionHeader("Браузеры"))

        val russianLabel = TextView(this).apply {
            text = "Для российских сайтов"
            textSize = 14f
            layoutParams = verticalParams(dp(8))
        }
        root.addView(russianLabel)

        val russianSpinner = Spinner(this).apply {
            layoutParams = verticalParams(dp(16))
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
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val option = russianOptions.getOrNull(position) ?: return
                RouterSettings.setRussianBrowser(this@SettingsActivity, option.packageName)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        root.addView(russianSpinner)

        val otherLabel = TextView(this).apply {
            text = "Для остальных сайтов"
            textSize = 14f
            layoutParams = verticalParams(dp(8))
        }
        root.addView(otherLabel)

        val otherSpinner = Spinner(this).apply {
            layoutParams = verticalParams(dp(4))
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
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val option = otherOptions.getOrNull(position) ?: return
                RouterSettings.setOtherBrowser(this@SettingsActivity, option.packageName)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        root.addView(otherSpinner)

        // === Секция: Главный экран ===
        // === Секция: Главный экран ===
        root.addView(divider())
        root.addView(sectionHeader("Главный экран"))

        val searchLabel = TextView(this).apply {
            text = "Поисковик"
            textSize = 14f
            layoutParams = verticalParams(dp(8))
        }
        root.addView(searchLabel)

        val searchSpinner = Spinner(this).apply {
            layoutParams = verticalParams(dp(4))
        }
        val engineOptions = SearchEngines.all

// Кастомный адаптер с иконками
        val engineAdapter = object : ArrayAdapter<SearchEngine>(
            this,
            android.R.layout.simple_spinner_item,
            engineOptions
        ) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent) as TextView
                val engine = getItem(position)
                if (engine != null) {
                    view.text = engine.label
                    view.setCompoundDrawablesWithIntrinsicBounds(engine.iconResId, 0, 0, 0)
                    view.compoundDrawablePadding = dp(8)
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                val engine = getItem(position)
                if (engine != null) {
                    view.text = engine.label
                    view.setCompoundDrawablesWithIntrinsicBounds(engine.iconResId, 0, 0, 0)
                    view.compoundDrawablePadding = dp(8)
                }
                return view
            }
        }

        searchSpinner.adapter = engineAdapter
        searchSpinner.setSelection(
            getEnginePosition(engineOptions, RouterSettings.getSearchEngine(this))
        )
        searchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long
            ) {
                val option = engineOptions.getOrNull(position) ?: return
                RouterSettings.setSearchEngine(this@SettingsActivity, option.id)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        root.addView(searchSpinner)


        // === Секция: Списки сайтов (вход на второй экран) ===
        root.addView(divider())
        root.addView(sectionHeader("Свои правила"))

        val listsEntry = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(12), dp(8), dp(12))
            layoutParams = verticalParams(0)
            val ta = theme.obtainStyledAttributes(
                intArrayOf(android.R.attr.selectableItemBackground)
            )
            val bg = ta.getResourceId(0, 0)
            ta.recycle()
            if (bg != 0) setBackgroundResource(bg)
            setOnClickListener {
                startActivity(Intent(this@SettingsActivity, ListsActivity::class.java))
            }
        }
        val listsEntryTitle = TextView(this).apply {
            text = "Изменить списки"
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        listsEntryCounts = TextView(this).apply {
            textSize = 13f
            setTextColor(secondaryColor())
        }
        val listsArrow = TextView(this).apply {
            text = "→"
            textSize = 16f
            setPadding(dp(8), 0, 0, 0)
        }
        listsEntry.addView(listsEntryTitle)
        listsEntry.addView(listsEntryCounts)
        listsEntry.addView(listsArrow)
        root.addView(listsEntry)

        scroll.addView(root)
        setContentView(scroll)
        refreshUpdateStatus()
        refreshListsEntry()
    }

    override fun onResume() {
        super.onResume()
        refreshUpdateStatus()
        refreshListsEntry()
    }

    private fun modeHint(allRu: Boolean): String = if (allRu) {
        "Список Минцифры + список v2fly + зоны .ru / .рф."
    } else {
        "Только официальный список Минцифры."
    }

    private fun refreshListsEntry() {
        val includeCount = SiteStore.getSites(this).size
        val excludeCount = SiteStore.getExclude(this).size
        listsEntryCounts.text = "Российских: $includeCount · Исключений: $excludeCount"
    }

    private fun refreshUpdateStatus() {
        val t1 = GeositeUpdater.getUpdatedAt(this, GeositeUpdater.KEY_MINCIFRA_UPDATED)
        val t2 = GeositeUpdater.getUpdatedAt(this, GeositeUpdater.KEY_V2FLY_UPDATED)
        listsStatusView.text = when {
            t1 == 0L && t2 == 0L -> "Списки ещё не обновлялись"
            t1 == 0L || t2 == 0L -> "Часть списков ещё не обновлялась"
            else -> "Списки обновлены: ${formatUpdatedAt(minOf(t1, t2))}"
        }
    }

    private fun formatUpdatedAt(timestamp: Long): String {
        if (timestamp == 0L) return "ещё не обновлялся"
        return java.text.SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            java.util.Locale.getDefault()
        ).format(java.util.Date(timestamp))
    }

    private fun updateListsNow() {
        updateButton.isEnabled = false
        updateButton.text = "…"
        Thread {
            val mincifraOk = GeositeUpdater.updateMinCifra(this)
            val v2flyOk = GeositeUpdater.updateV2fly(this)
            runOnUiThread {
                updateButton.isEnabled = true
                updateButton.text = "Обновить"
                val message = "Минцифра: ${if (mincifraOk) "✓" else "✗"}, " +
                        "v2fly: ${if (v2flyOk) "✓" else "✗"}"
                Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_SHORT).show()
                refreshUpdateStatus()
            }
        }.start()
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