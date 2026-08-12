/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SiteStore.ensureDefaultSites(this)
        GeositeUpdater.load(this)

        if (RouterSettings.isUseGeosite(this) && GeositeUpdater.isStale(this)) {
            Thread { GeositeUpdater.update(this) }.start()
        }

        val uri = intent?.data
        if (uri != null) {
            Router.open(this, uri)
        }

        finish()
    }
}
