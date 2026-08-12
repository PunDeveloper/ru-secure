/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import android.app.role.RoleManager
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultBrowserPromptTest {

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        DeviceWake.keepScreenOn()
    }

    @After
    fun tearDown() {
        DeviceWake.restoreScreenTimeout()
    }

    @Test
    fun promptShownWhenNotDefault() {
        assumeFalse(
            "Устройство уже назначило RuSecure браузером по умолчанию",
            DefaultBrowserHelper.isDefaultBrowser(context)
        )

        ActivityScenario.launch(SearchActivity::class.java).use {
            onView(withText("Назначьте RuSecure браузером по умолчанию"))
                .inRoot(RootMatchers.isDialog())
                .check(matches(isDisplayed()))

            onView(withText("Открыть настройки"))
                .inRoot(RootMatchers.isDialog())
                .check(matches(isDisplayed()))

            onView(withText("Позже"))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
        }
    }

    @Test
    fun detectionMatchesSystemRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            val heldByUs = roleManager?.isRoleHeld(RoleManager.ROLE_BROWSER) == true
            assertEquals(heldByUs, DefaultBrowserHelper.isDefaultBrowser(context))
        }
    }
}
