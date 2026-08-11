/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (C) 2025 PunDeveloper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.pundeveloper.ruSiteRouter

import java.util.Locale

data class GeositeRules(
    val suffix: Set<String>,
    val full: Set<String>,
    val regex: List<Regex>
) {
    fun matches(host: String): Boolean {
        if (host.isEmpty()) return false

        if (host in full) return true

        // suffix-проход: a.b.c.ru -> b.c.ru -> c.ru -> ru
        var h = host
        while (h.isNotEmpty()) {
            if (h in suffix) return true
            val i = h.indexOf('.')
            if (i < 0) break
            h = h.substring(i + 1)
        }

        return regex.any { it.containsMatchIn(host) }
    }

    companion object {
        val EMPTY = GeositeRules(emptySet(), emptySet(), emptyList())
    }
}

object GeositeRepo {

    // Актуальный релиз. Ссылка редиректит на последний.
    const val URL =
        "https://github.com/v2fly/domain-list-community/releases/latest/download/dlc.dat_plain.yml"

    // Имя категории в yml
    const val CATEGORY = "category-ru"

    @Volatile
    var rules: GeositeRules = GeositeRules.EMPTY
        private set

    /**
     * Парсит dlc.dat_plain.yml и вытаскивает секцию category-ru.
     * Без YAML-библиотек, на state-machine по строкам.
     */
    fun parseCategoryYml(
        text: String,
        category: String = CATEGORY
    ): GeositeRules {
        val suffix = mutableSetOf<String>()
        val full = mutableSetOf<String>()
        val regex = mutableListOf<Regex>()

        var inTarget = false

        for (raw in text.lineSequence()) {
            val line = raw.trim()

            if (line.startsWith("- name:")) {
                val name = line
                    .substringAfter("- name:")
                    .trim()
                    .trim('"')

                inTarget = name.equals(category, ignoreCase = true)
                continue
            }

            if (!inTarget) continue

            // Новое имя — выходим из секции
            if (line.startsWith("- name:")) break

            if (!line.startsWith("- \"") && !line.startsWith("- '")) continue

            val rule = line
                .removePrefix("- ")
                .trim()
                .trim('"')
                .trim('\'')

            if (rule.isEmpty()) continue

            when {
                rule.startsWith("regexp:") -> {
                    runCatching { regex.add(Regex(rule.removePrefix("regexp:"))) }
                }
                rule.startsWith("full:") -> {
                    full.add(rule.removePrefix("full:").lowercase(Locale.ROOT))
                }
                rule.startsWith("domain:") -> {
                    suffix.add(rule.removePrefix("domain:").lowercase(Locale.ROOT))
                }
                else -> {
                    // на всякий случай, хотя в этом файле такого не бывает
                    suffix.add(rule.lowercase(Locale.ROOT))
                }
            }
        }

        return GeositeRules(suffix, full, regex)
    }

    fun updateRules(newRules: GeositeRules) {
        rules = newRules
    }
}