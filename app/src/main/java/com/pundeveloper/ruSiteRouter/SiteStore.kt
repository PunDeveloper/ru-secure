/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import android.content.Context
import java.net.IDN
import java.net.URLDecoder
import java.util.Locale
import androidx.core.content.edit
import androidx.core.net.toUri

object SiteStore {
    private const val PREFS_NAME = "link_router_prefs"
    private const val KEY_SITES = "custom_sites"
    private const val KEY_EXCLUDE = "custom_exclude"
    private const val KEY_DEFAULT_SITES_ADDED = "default_sites_added"

    private val defaultSites = listOf(
        "gosuslugi.ru",
        "nalog.gov.ru",
        "mos.ru",
        "gosuslugi.mosreg.ru",
        "kremlin.ru",
        "government.ru"
    )

    fun getSites(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_SITES, emptySet()) ?: emptySet()
    }

    fun getExclude(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_EXCLUDE, emptySet()) ?: emptySet()
    }

    fun addSite(context: Context, rawSite: String, exclude: Boolean) {
        val site = normalize(rawSite)
        if (site.isEmpty()) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val targetKey = if (exclude) KEY_EXCLUDE else KEY_SITES
        val otherKey = if (exclude) KEY_SITES else KEY_EXCLUDE

        val target = prefs.getStringSet(targetKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        val other = prefs.getStringSet(otherKey, emptySet())?.toMutableSet() ?: mutableSetOf()

        target.add(site)
        other.remove(site)

        prefs.edit {
            putStringSet(targetKey, target)
                .putStringSet(otherKey, other)
        }
    }

    fun removeSite(context: Context, site: String, fromExclude: Boolean) {
        val key = if (fromExclude) KEY_EXCLUDE else KEY_SITES

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()

        set.remove(site)

        prefs.edit { putStringSet(key, set) }
    }

    fun ensureDefaultSites(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (prefs.getBoolean(KEY_DEFAULT_SITES_ADDED, false)) {
            return
        }

        val current = prefs.getStringSet(KEY_SITES, emptySet())
            ?.toMutableSet()
            ?: mutableSetOf()

        defaultSites.forEach { site ->
            val normalized = normalize(site)
            if (normalized.isNotEmpty()) {
                current.add(normalized)
            }
        }

        prefs.edit {
            putBoolean(KEY_DEFAULT_SITES_ADDED, true)
                .putStringSet(KEY_SITES, current)
        }
    }

    fun normalize(raw: String): String {
        val firstLine = raw.trim().lineSequence().firstOrNull()?.trim() ?: return ""

        if (firstLine.isEmpty()) return ""

        // Если пользователь ввёл суффикс или маску: .ru, *.ru, .рф, *.рф
        if (firstLine.startsWith(".") || firstLine.startsWith("*")) {
            return normalizeRule(firstLine)
        }

        var candidate = firstLine

        if (!candidate.contains("://")) {
            candidate = "http://$candidate"
        }

        var host = try {
            candidate.toUri().host?.trim()?.lowercase(Locale.ROOT) ?: ""
        } catch (_: Exception) {
            ""
        }

        if (host.isEmpty()) {
            host = fallbackExtractHost(firstLine)
        }

        host = host.lowercase(Locale.ROOT).trim()

        if (host.contains("%")) {
            host = try {
                URLDecoder.decode(host, "UTF-8")
            } catch (_: Exception) {
                host
            }
        }

        if (host.startsWith("www.")) {
            host = host.removePrefix("www.")
        }

        if (host.isEmpty()) return ""

        return try {
            IDN.toASCII(host)
        } catch (_: Exception) {
            host
        }
    }

    private fun normalizeRule(raw: String): String {
        var s = raw.trim().lowercase(Locale.ROOT)

        if (s.isEmpty()) return ""

        if (s.startsWith("*.")) {
            s = s.removePrefix("*")
        } else if (s.startsWith("*")) {
            s = s.removePrefix("*")
        }

        s = s.trim()

        if (s.isEmpty() || s == ".") return ""

        s = if (s.startsWith(".")) {
            "." + toAscii(s.removePrefix("."))
        } else {
            toAscii(s)
        }

        return s
    }

    private fun fallbackExtractHost(raw: String): String {
        var s = raw.trim().lowercase(Locale.ROOT)

        if (s.contains("://")) {
            s = s.substringAfter("://")
        }

        s = s.substringBefore("/")
        s = s.substringBefore("?")
        s = s.substringBefore("#")

        if (s.contains("@")) {
            s = s.substringAfter("@")
        }

        s = s.substringBefore(":")

        return s.trim()
    }

    private fun toAscii(value: String): String {
        return try {
            IDN.toASCII(value)
        } catch (_: Exception) {
            value
        }
    }
}