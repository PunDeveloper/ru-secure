/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import androidx.test.platform.app.InstrumentationRegistry

/**
 * Espresso требует window focus: если экран устройства гаснет посреди
 * прогона, тесты падают с RootViewWithoutFocusException.
 */
object DeviceWake {

    fun keepScreenOn() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP")
        // держим экран включённым, пока устройство на USB
        uiAutomation.executeShellCommand("svc power stayon usb")
    }

    fun restoreScreenTimeout() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.executeShellCommand("svc power stayon false")
    }
}
