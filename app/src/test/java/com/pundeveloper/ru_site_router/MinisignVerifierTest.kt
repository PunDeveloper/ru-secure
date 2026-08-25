/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ru_site_router

import org.bouncycastle.util.encoders.Base64
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinisignVerifierTest {

    @Test
    fun verifiesRealReleaseSignatureInHashedMode() {
        // Релиз подписан с `minisign -H`: Ed25519 поверх BLAKE2b-512 дайджеста
        assertTrue(
            MinisignVerifier.verifySignatureWithKey(
                publicKey(),
                fixtureBytes("ru-cert-rules.txt"),
                fixture("ru-cert-rules.txt.minisig")
            )
        )
    }

    @Test
    fun rejectsTamperedMessage() {
        val tampered = fixtureBytes("ru-cert-rules.txt") + "domain-suffix:evil.example".toByteArray()
        assertFalse(
            MinisignVerifier.verifySignatureWithKey(
                publicKey(),
                tampered,
                fixture("ru-cert-rules.txt.minisig")
            )
        )
    }

    @Test
    fun rejectsWrongPublicKey() {
        // Валидный по структуре ключ (42 байта), но не тот, которым подписан релиз
        val blob = ByteArray(42)
        blob[0] = 'E'.code.toByte()
        blob[1] = 'd'.code.toByte()
        blob[10] = 1
        val fakeKey = "untrusted comment: fake key\n" + Base64.toBase64String(blob) + "\n"

        assertFalse(
            MinisignVerifier.verifySignatureWithKey(
                fakeKey,
                fixtureBytes("ru-cert-rules.txt"),
                fixture("ru-cert-rules.txt.minisig")
            )
        )
    }

    @Test
    fun rejectsMalformedSignature() {
        assertFalse(
            MinisignVerifier.verifySignatureWithKey(
                publicKey(),
                fixtureBytes("ru-cert-rules.txt"),
                "это не подпись"
            )
        )
    }

    @Test
    fun verifySha256PassesWithReleaseChecksum() {
        assertTrue(
            MinisignVerifier.verifySha256(
                fixtureBytes("ru-cert-rules.txt"),
                fixture("checksum.sha256")
            )
        )
    }

    @Test
    fun verifySha256RejectsWrongChecksum() {
        val wrongChecksum = "0".repeat(64) + "  ru-cert-rules.txt"
        assertFalse(
            MinisignVerifier.verifySha256(
                fixtureBytes("ru-cert-rules.txt"),
                wrongChecksum
            )
        )
    }

    @Test
    fun parsePublicKeyAcceptsRealKey() {
        assertNotNull(MinisignVerifier.parsePublicKey(publicKey()))
    }

    @Test
    fun parsePublicKeyRejectsWrongSize() {
        // 40 байт — ошибка старого парсера; реальный ключ minisign занимает 42 байта
        val shortBlob = ByteArray(40)
        val shortKey = "untrusted comment: short\n" + Base64.toBase64String(shortBlob) + "\n"
        assertNull(MinisignVerifier.parsePublicKey(shortKey))
    }

    private fun publicKey(): String = fixture("public.key")

    private fun fixture(name: String): String = fixtureBytes(name).toString(Charsets.UTF_8)

    private fun fixtureBytes(name: String): ByteArray {
        val stream = javaClass.classLoader!!.getResourceAsStream("minisign/$name")
            ?: error("Fixture not found: minisign/$name")
        return stream.use { it.readBytes() }
    }
}
