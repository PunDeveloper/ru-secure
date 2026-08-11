package com.pundeveloper.ruSiteRouter

import android.content.Context
import java.io.File
import androidx.core.content.edit

object GeositeUpdater {
    private const val FILE_NAME = "geosite_category_ru.yml"
    private const val KEY_UPDATED_AT = "geosite_updated"

    private fun file(context: Context): File =
        File(context.filesDir, FILE_NAME)

    fun load(context: Context) {
        val text = when {
            file(context).exists() -> file(context).readText()
            else -> context.assets.open("geosite_category_ru.yml").bufferedReader().readText()
        }

        val parsed = GeositeRepo.parseCategoryYml(text)
        GeositeRepo.updateRules(parsed)
    }

    fun update(context: Context): Boolean {
        return try {
            val text = java.net.URL(GeositeRepo.URL)
                .openStream()
                .bufferedReader()
                .readText()

            if (text.isBlank()) return false

            val parsed = GeositeRepo.parseCategoryYml(text)
            if (parsed.suffix.isEmpty() && parsed.full.isEmpty() && parsed.regex.isEmpty()) {
                return false
            }

            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(text)
            tmp.renameTo(file(context))

            context.getSharedPreferences("link_router_prefs", Context.MODE_PRIVATE)
                .edit {
                    putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                }

            GeositeRepo.updateRules(parsed)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getUpdatedAt(context: Context): Long {
        return context.getSharedPreferences("link_router_prefs", Context.MODE_PRIVATE)
            .getLong(KEY_UPDATED_AT, 0)
    }

    fun isStale(
        context: Context,
        maxAgeMs: Long = 24 * 60 * 60 * 1000
    ): Boolean {
        val updated = getUpdatedAt(context)
        return System.currentTimeMillis() - updated > maxAgeMs
    }
}