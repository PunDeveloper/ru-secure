/*
RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
остальные — в браузере по выбору.
Copyright (c) 2025 PunDeveloper
SPDX-License-Identifier: MIT
*/
package com.pundeveloper.ru_site_router

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class ListsActivity : Activity() {

    private lateinit var includeContainer: LinearLayout
    private lateinit var excludeContainer: LinearLayout

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // Верхняя панель с кнопкой назад
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = verticalParams(dp(12))
        }
        val back = TextView(this).apply {
            text = "←"
            textSize = 20f
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { finish() }
        }
        val screenTitle = TextView(this).apply {
            text = "Списки сайтов"
            textSize = 18f
        }
        topBar.addView(back)
        topBar.addView(screenTitle)
        root.addView(topBar)

        val hint = TextView(this).apply {
            text = "Можно вставлять полный URL.\nУдаление — долгим нажатием по строке."
            textSize = 13f
            setTextColor(secondaryColor())
            layoutParams = verticalParams(dp(12))
        }
        root.addView(hint)

        val input = EditText(this).apply {
            setHint("https://gosuslugi.ru")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            layoutParams = verticalParams(dp(8))
        }
        root.addView(input)

        val excludeCheckbox = CheckBox(this).apply {
            text = "Добавить как исключение (всегда в другом браузере)"
            layoutParams = verticalParams(dp(12))
        }
        root.addView(excludeCheckbox)

        val addButton = Button(this).apply {
            text = "Добавить"
            layoutParams = verticalParams(dp(16))
        }
        root.addView(addButton)

        val includeTitle = TextView(this).apply {
            text = "Российские сайты (в браузере для российских)"
            textSize = 15f
            layoutParams = verticalParams(dp(8))
        }
        root.addView(includeTitle)
        includeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = verticalParams(0)
        }
        root.addView(includeContainer)

        val excludeTitle = TextView(this).apply {
            text = "Исключения (всегда в другом браузере)"
            textSize = 15f
            layoutParams = verticalParams(dp(8)).apply { topMargin = dp(16) }
        }
        root.addView(excludeTitle)
        excludeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = verticalParams(0)
        }
        root.addView(excludeContainer)

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
                SiteStore.removeSite(this@ListsActivity, site, fromExclude)
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
}