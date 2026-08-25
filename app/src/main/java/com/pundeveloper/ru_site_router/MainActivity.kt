/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ru_site_router

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SiteStore.ensureDefaultSites(this)
        GeositeUpdater.load(this)

        val mincifraStale = GeositeUpdater.isStale(this, GeositeUpdater.KEY_MINCIFRA_UPDATED)
        val v2flyStale = GeositeUpdater.isStale(this, GeositeUpdater.KEY_V2FLY_UPDATED)

        // ВРЕМЕННАЯ ОТЛАДКА: показываем состояние
        android.util.Log.d("MainActivity", "mincifraStale=$mincifraStale, v2flyStale=$v2flyStale")

        if (mincifraStale) {
            Thread {
                val result = GeositeUpdater.updateMinCifra(this)
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this,
                        if (result) "Минцифра обновлена ✓" else "Минцифра: ОШИБКА ✗",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }.start()
        }

        if (RouterSettings.isUseV2fly(this) && v2flyStale) {
            Thread {
                val result = GeositeUpdater.updateV2fly(this)
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this,
                        if (result) "v2fly обновлён ✓" else "v2fly: ОШИБКА ✗",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }.start()
        }

        val uri = intent?.data
        if (uri != null) {
            Router.open(this, uri)
        }

        finish()
    }
}