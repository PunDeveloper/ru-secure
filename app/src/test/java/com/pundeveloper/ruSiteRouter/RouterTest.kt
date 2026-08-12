/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ruSiteRouter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterTest {

    @Test
    fun normalizeHostTrimsAndLowercases() {
        assertEquals("example.com", Router.normalizeHost("  Example.COM "))
    }

    @Test
    fun normalizeHostStripsWww() {
        assertEquals("example.com", Router.normalizeHost("www.example.com"))
    }

    @Test
    fun normalizeHostHandlesEmpty() {
        assertEquals("", Router.normalizeHost(null))
        assertEquals("", Router.normalizeHost(""))
        assertEquals("", Router.normalizeHost("www."))
    }

    @Test
    fun normalizeHostConvertsPunycode() {
        // .рф -> xn--p1ai
        assertEquals("xn--p1ai", Router.normalizeHost("РФ"))
        assertEquals("xn--p1ai", Router.normalizeHost("www.рф"))
    }

    @Test
    fun matchesRuleExact() {
        assertTrue(Router.matchesRule("gosuslugi.ru", "gosuslugi.ru"))
        assertFalse(Router.matchesRule("notgosuslugi.ru", "gosuslugi.ru"))
    }

    @Test
    fun matchesRuleSubdomain() {
        assertTrue(Router.matchesRule("api.gosuslugi.ru", "gosuslugi.ru"))
        assertFalse(Router.matchesRule("gosuslugi-ru.example.com", "gosuslugi.ru"))
    }

    @Test
    fun matchesRuleWithDotPrefix() {
        assertTrue(Router.matchesRule("api.gosuslugi.ru", ".gosuslugi.ru"))
        assertTrue(Router.matchesRule("gosuslugi.ru", ".gosuslugi.ru"))
    }

    @Test
    fun matchesRuleHandlesEmpty() {
        assertFalse(Router.matchesRule("", "gosuslugi.ru"))
        assertFalse(Router.matchesRule("gosuslugi.ru", ""))
    }
}
