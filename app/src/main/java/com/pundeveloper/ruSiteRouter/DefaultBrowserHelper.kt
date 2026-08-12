/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

object DefaultBrowserHelper {

    fun isDefaultBrowser(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_BROWSER) == true
        } else {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com"))
            val info = try {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            } catch (_: Exception) {
                null
            }
            info?.activityInfo?.packageName == context.packageName
        }
    }

    /**
     * Открывает системный экран «Приложения по умолчанию»,
     * где выбирается браузер по умолчанию.
     */
    fun openDefaultBrowserSettings(activity: Activity) {
        try {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            try {
                activity.startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                // настройки недоступны
            }
        }
    }
}
