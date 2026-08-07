package com.pundeveloper.ruSiteRouter

import android.R
import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private val items = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SiteStore.ensureDefaultSites(this)

        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        @Suppress("DEPRECATION")
        root.setOnApplyWindowInsetsListener { v, insets ->
            val topInset = insets.systemWindowInsetTop
            val bottomInset = insets.systemWindowInsetBottom

            v.setPadding(
                dp(16),
                topInset + dp(16),
                dp(16),
                dp(16) + bottomInset
            )

            insets
        }

        val useZones = CheckBox(this).apply {
            text = "Считать .ru и .рф российскими"
            isChecked = RouterSettings.isUseZones(this@SettingsActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }

            setOnCheckedChangeListener { _, isChecked ->
                RouterSettings.setUseZones(this@SettingsActivity, isChecked)
            }
        }

        val russianLabel = TextView(this).apply {
            text = "Браузер для российских сайтов"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }

        val russianSpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        }

        val russianOptions = BrowserHelper.getBrowserOptions(this)
        val russianAdapter = ArrayAdapter(
            this,
            R.layout.simple_spinner_item,
            russianOptions.map { it.label }
        )

        russianAdapter.setDropDownViewResource(
            R.layout.simple_spinner_dropdown_item
        )

        russianSpinner.adapter = russianAdapter
        russianSpinner.setSelection(
            getSelectedPosition(
                russianOptions,
                RouterSettings.getRussianBrowser(this)
            )
        )

        russianSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val option = russianOptions.getOrNull(position) ?: return
                RouterSettings.setRussianBrowser(this@SettingsActivity, option.packageName)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // ничего не делаем
            }
        }

        val otherLabel = TextView(this).apply {
            text = "Браузер для остальных сайтов"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }

        val otherSpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(24)
            }
        }

        val otherOptions = BrowserHelper.getBrowserOptions(this)
        val otherAdapter = ArrayAdapter(
            this,
            R.layout.simple_spinner_item,
            otherOptions.map { it.label }
        )

        otherAdapter.setDropDownViewResource(
            R.layout.simple_spinner_dropdown_item
        )

        otherSpinner.adapter = otherAdapter
        otherSpinner.setSelection(
            getSelectedPosition(
                otherOptions,
                RouterSettings.getOtherBrowser(this)
            )
        )

        otherSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val option = otherOptions.getOrNull(position) ?: return
                RouterSettings.setOtherBrowser(this@SettingsActivity, option.packageName)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // ничего не делаем
            }
        }

        val sitesTitle = TextView(this).apply {
            text = "Российские сайты"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }

        val hint = TextView(this).apply {
            text = "Можно вставлять полный URL.\nУдаление — долгим нажатием."
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val input = EditText(this).apply {
            setHint("https://gosuslugi.ru")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val addButton = Button(this).apply {
            text = "Добавить"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        }

        val list = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                weight = 1f
            }
        }

        items.addAll(SiteStore.getSites(this).sorted())

        adapter = ArrayAdapter(
            this,
            R.layout.simple_list_item_1,
            items
        )

        list.adapter = adapter

        addButton.setOnClickListener {
            val raw = input.text.toString()

            if (raw.isBlank()) {
                Toast.makeText(this, "Введите сайт", Toast.LENGTH_SHORT).show()
            } else {
                SiteStore.addSite(this, raw)
                input.setText("")
                refresh()
            }
        }

        list.setOnItemLongClickListener { _, _, position, _ ->
            val site = items.getOrNull(position) ?: return@setOnItemLongClickListener false

            SiteStore.removeSite(this, site)
            refresh()

            true
        }

        root.addView(useZones)
        root.addView(russianLabel)
        root.addView(russianSpinner)
        root.addView(otherLabel)
        root.addView(otherSpinner)
        root.addView(sitesTitle)
        root.addView(hint)
        root.addView(input)
        root.addView(addButton)
        root.addView(list)

        setContentView(root)
    }

    private fun getSelectedPosition(
        options: List<BrowserOption>,
        storedPackage: String
    ): Int {
        val index = options.indexOfFirst { it.packageName == storedPackage }
        return if (index >= 0) index else 0
    }

    private fun refresh() {
        items.clear()
        items.addAll(SiteStore.getSites(this).sorted())
        adapter.notifyDataSetChanged()
    }
}