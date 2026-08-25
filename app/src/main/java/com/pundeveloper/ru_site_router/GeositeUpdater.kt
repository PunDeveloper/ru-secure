/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ru_site_router

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object GeositeUpdater {
    private const val TAG = "GeositeUpdater"

    private const val V2FLY_FILE = "geosite_category_ru.yml"
    private const val MINCIFRA_FILE = "ru_cert_rules.txt"
    private const val PREFS_NAME = "link_router_prefs"

    const val KEY_V2FLY_UPDATED = "v2fly_updated"
    const val KEY_MINCIFRA_UPDATED = "mincifra_updated"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun v2flyFile(context: Context): File = File(context.filesDir, V2FLY_FILE)
    private fun mincifraFile(context: Context): File = File(context.filesDir, MINCIFRA_FILE)

    private fun downloadText(url: String): String? {
        val body = downloadBytes(url) ?: return null
        val text = body.toString(Charsets.UTF_8)
        if (text.isBlank()) {
            Log.w(TAG, "Blank response body from $url")
            return null
        }
        return text
    }

    private fun downloadBytes(url: String): ByteArray? {
        Log.d(TAG, "Starting download: $url")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "RuSecure/1.0")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                Log.d(TAG, "Response code: $code for $url")

                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP error $code for $url")
                    return null
                }

                val body = response.body?.bytes()
                Log.d(TAG, "Downloaded ${body?.size ?: 0} bytes from $url")

                if (body == null || body.isEmpty()) {
                    Log.w(TAG, "Empty response body from $url")
                    return null
                }

                body
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download exception for $url", e)
            null
        }
    }

    fun load(context: Context) {
        Log.d(TAG, "Loading rules from cache/assets")

        val v2flyText = when {
            v2flyFile(context).exists() -> {
                Log.d(TAG, "Loading v2fly from cache")
                v2flyFile(context).readText()
            }
            else -> runCatching {
                Log.d(TAG, "Loading v2fly from assets")
                context.assets.open("geosite_category_ru.yml").bufferedReader().readText()
            }.getOrNull()
        }

        if (v2flyText != null) {
            Log.d(TAG, "Parsing v2fly rules (${v2flyText.length} chars)")
            GeositeRepo.updateV2flyRules(GeositeRepo.parseCategoryYml(v2flyText))
        } else {
            Log.w(TAG, "No v2fly rules available")
        }

        val mincifraText = when {
            mincifraFile(context).exists() -> {
                Log.d(TAG, "Loading mincifra from cache")
                mincifraFile(context).readText()
            }
            else -> runCatching {
                Log.d(TAG, "Loading mincifra from assets")
                context.assets.open("ru-cert-rules.txt").bufferedReader().readText()
            }.getOrNull()
        }

        if (mincifraText != null) {
            Log.d(TAG, "Parsing mincifra rules (${mincifraText.length} chars)")
            GeositeRepo.updateMinCifraRules(GeositeRepo.parseMinCifraRules(mincifraText))
        } else {
            Log.w(TAG, "No mincifra rules available")
        }
    }

    fun updateMinCifra(context: Context): Boolean {
        Log.d(TAG, "=== Starting mincifra update ===")

        return try {
            Log.d(TAG, "Downloading rules file...")
            val rulesBytes = downloadBytes(GeositeRepo.MINCIFRA_URL)
            if (rulesBytes == null) {
                Log.e(TAG, "Failed to download rules file")
                return false
            }
            Log.d(TAG, "Rules file downloaded: ${rulesBytes.size} bytes")

            Log.d(TAG, "Downloading checksum...")
            val checksumText = downloadText(GeositeRepo.MINCIFRA_CHECKSUM_URL)
            if (checksumText == null) {
                Log.e(TAG, "Failed to download checksum")
                return false
            }
            Log.d(TAG, "Checksum: $checksumText")

            Log.d(TAG, "Downloading signature...")
            val sigText = downloadText(GeositeRepo.MINCIFRA_SIG_URL)
            if (sigText == null) {
                Log.e(TAG, "Failed to download signature")
                return false
            }
            Log.d(TAG, "Signature downloaded")

            Log.d(TAG, "Verifying SHA-256...")
            if (!MinisignVerifier.verifySha256(rulesBytes, checksumText)) {
                Log.e(TAG, "SHA-256 verification FAILED")
                return false
            }
            Log.d(TAG, "SHA-256 verification passed")

            Log.d(TAG, "Verifying Minisign signature...")
            if (!MinisignVerifier.verifySignature(context, rulesBytes, sigText)) {
                Log.e(TAG, "Minisign verification FAILED")
                return false
            }
            Log.d(TAG, "Minisign verification passed")

            val tmp = File(context.filesDir, "$MINCIFRA_FILE.tmp")
            tmp.writeBytes(rulesBytes)
            val renamed = tmp.renameTo(mincifraFile(context))
            Log.d(TAG, "File renamed: $renamed")

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putLong(KEY_MINCIFRA_UPDATED, System.currentTimeMillis())
            }

            val rules = GeositeRepo.parseMinCifraRules(rulesBytes.toString(Charsets.UTF_8))
            GeositeRepo.updateMinCifraRules(rules)
            Log.d(TAG, "Updated ${rules.suffix.size} mincifra rules")

            Log.d(TAG, "=== Mincifra update SUCCESS ===")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Update mincifra EXCEPTION", e)
            false
        }
    }

    fun updateV2fly(context: Context): Boolean {
        Log.d(TAG, "=== Starting v2fly update ===")

        return try {
            Log.d(TAG, "Downloading v2fly file...")
            val text = downloadText(GeositeRepo.V2FLY_URL)
            if (text == null) {
                Log.e(TAG, "Failed to download v2fly file")
                return false
            }
            Log.d(TAG, "V2fly file downloaded: ${text.length} chars")

            if (text.isBlank()) {
                Log.e(TAG, "V2fly file is blank")
                return false
            }

            Log.d(TAG, "Parsing v2fly YAML...")
            val parsed = GeositeRepo.parseCategoryYml(text)
            Log.d(TAG, "Parsed v2fly: ${parsed.suffix.size} suffix, ${parsed.full.size} full, ${parsed.regex.size} regex")

            if (parsed.suffix.isEmpty() && parsed.full.isEmpty() && parsed.regex.isEmpty()) {
                Log.e(TAG, "V2fly rules are empty after parsing")
                return false
            }

            val tmp = File(context.filesDir, "$V2FLY_FILE.tmp")
            tmp.writeText(text)
            val renamed = tmp.renameTo(v2flyFile(context))
            Log.d(TAG, "File renamed: $renamed")

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putLong(KEY_V2FLY_UPDATED, System.currentTimeMillis())
            }

            GeositeRepo.updateV2flyRules(parsed)

            Log.d(TAG, "=== V2fly update SUCCESS ===")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Update v2fly EXCEPTION", e)
            false
        }
    }

    fun getUpdatedAt(context: Context, key: String): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(key, 0)
    }

    fun isStale(context: Context, key: String, maxAgeMs: Long = 24 * 60 * 60 * 1000): Boolean {
        val updated = getUpdatedAt(context, key)
        return System.currentTimeMillis() - updated > maxAgeMs
    }
}