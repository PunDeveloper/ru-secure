package com.pundeveloper.ru_site_router

import java.util.Locale

data class GeositeRules(
    val suffix: Set<String>,
    val full: Set<String>,
    val regex: List<Regex>
) {
    fun matches(host: String): Boolean {
        if (host.isEmpty()) return false
        if (host in full) return true
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
    // URL для v2fly
    const val V2FLY_URL = "https://github.com/v2fly/domain-list-community/releases/latest/download/dlc.dat_plain.yml"
    const val V2FLY_CATEGORY = "category-ru"

    // URL для Минцифры
    const val MINCIFRA_URL = "https://github.com/PunDeveloper/ru-certs-sites/releases/latest/download/ru-cert-rules.txt"
    const val MINCIFRA_SIG_URL = "$MINCIFRA_URL.minisig"
    const val MINCIFRA_CHECKSUM_URL = "https://github.com/PunDeveloper/ru-certs-sites/releases/latest/download/checksum.sha256"

    @Volatile
    var v2flyRules: GeositeRules = GeositeRules.EMPTY
        private set

    @Volatile
    var mincifraRules: GeositeRules = GeositeRules.EMPTY
        private set

    fun parseCategoryYml(text: String, category: String = V2FLY_CATEGORY): GeositeRules {
        val suffix = mutableSetOf<String>()
        val full = mutableSetOf<String>()
        val regex = mutableListOf<Regex>()
        var inTarget = false
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("- name:")) {
                val name = line.substringAfter("- name:").trim().trim('"')
                inTarget = name.equals(category, ignoreCase = true)
                continue
            }
            if (!inTarget) continue
            if (line.startsWith("- name:")) break
            if (!line.startsWith("- \"") && !line.startsWith("- '")) continue
            val rule = line.removePrefix("- ").trim().trim('"').trim('\'')
            if (rule.isEmpty()) continue
            when {
                rule.startsWith("regexp:") -> runCatching { regex.add(Regex(rule.removePrefix("regexp:"))) }
                rule.startsWith("full:") -> full.add(rule.removePrefix("full:").lowercase(Locale.ROOT))
                rule.startsWith("domain:") -> suffix.add(rule.removePrefix("domain:").lowercase(Locale.ROOT))
                else -> suffix.add(rule.lowercase(Locale.ROOT))
            }
        }
        return GeositeRules(suffix, full, regex)
    }

    fun parseMinCifraRules(text: String): GeositeRules {
        val suffix = mutableSetOf<String>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("#") || line.isEmpty()) continue
            if (line.startsWith("domain-suffix:")) {
                suffix.add(line.removePrefix("domain-suffix:").lowercase(Locale.ROOT))
            }
        }
        return GeositeRules(suffix, emptySet(), emptyList())
    }

    fun updateV2flyRules(newRules: GeositeRules) { v2flyRules = newRules }
    fun updateMinCifraRules(newRules: GeositeRules) { mincifraRules = newRules }

    fun isRussian(host: String, useV2flyMode: Boolean): Boolean {
        if (mincifraRules.matches(host)) return true
        if (useV2flyMode && v2flyRules.matches(host)) return true
        return false
    }
}