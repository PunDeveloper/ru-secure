/*
 * RuSecure — маршрутизатор ссылок: российские сайты в Яндекс Браузере,
 * остальные — в браузере по выбору.
 * Copyright (c) 2025 PunDeveloper
 * SPDX-License-Identifier: MIT
 */
package com.pundeveloper.ru_site_router

import android.content.Context
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.util.encoders.Base64
import java.security.MessageDigest

object MinisignVerifier {

    // Блобы minisign: 2 байта алгоритма + 8 байт key ID + полезные данные
    private const val ALG_SIZE = 2
    private const val KEY_ID_SIZE = 8
    private const val ED25519_SIZE = 64 // и подпись, и дайджест BLAKE2b-512
    private const val ED25519_KEY_SIZE = 32
    private const val SIGNATURE_BLOB_SIZE = ALG_SIZE + KEY_ID_SIZE + ED25519_SIZE // 74
    private const val PUBLIC_KEY_BLOB_SIZE = ALG_SIZE + KEY_ID_SIZE + ED25519_KEY_SIZE // 42

    // "Ed" — подпись по сырым данным, "ED" — по BLAKE2b-512 дайджесту (режим -H)
    private const val ALG_ED_LEGACY = 0x4564
    private const val ALG_ED_HASHED = 0x4544

    fun verifySha256(message: ByteArray, sha256FileContent: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(message)
        val hexHash = hash.joinToString("") { "%02x".format(it) }
        // Формат checksum.sha256 обычно: "hash  filename" или просто "hash"
        val expectedHash = sha256FileContent.trim().split(Regex("\\s+")).firstOrNull()?.lowercase()
        return hexHash == expectedHash
    }

    fun verifySignature(context: Context, message: ByteArray, sigFileContent: String): Boolean {
        val pubKeyText = runCatching {
            context.assets.open("public.key").bufferedReader().readText()
        }.getOrNull() ?: return false
        return verifySignatureWithKey(pubKeyText, message, sigFileContent)
    }

    internal fun verifySignatureWithKey(
        publicKeyText: String,
        message: ByteArray,
        sigFileContent: String
    ): Boolean {
        val pubKey = parsePublicKey(publicKeyText) ?: return false
        val lines = sigFileContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
        // 4 строки: 2 комментария + подпись файла + глобальная подпись
        if (lines.size < 4) return false

        val sigBytes = decodeBase64(lines[1]) ?: return false
        if (sigBytes.size != SIGNATURE_BLOB_SIZE) return false

        val signature = sigBytes.copyOfRange(ALG_SIZE + KEY_ID_SIZE, SIGNATURE_BLOB_SIZE)

        val signedMessage = when (algorithm(sigBytes)) {
            ALG_ED_HASHED -> blake2b512(message)
            ALG_ED_LEGACY -> message
            else -> return false
        }

        return try {
            val signer = Ed25519Signer()
            signer.init(false, pubKey)
            signer.update(signedMessage, 0, signedMessage.size)
            signer.verifySignature(signature)
        } catch (e: Exception) {
            false
        }
    }

    internal fun parsePublicKey(text: String): Ed25519PublicKeyParameters? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 2) return null

        val keyBytes = decodeBase64(lines[1]) ?: return null
        if (keyBytes.size != PUBLIC_KEY_BLOB_SIZE) return null

        return try {
            Ed25519PublicKeyParameters(keyBytes, ALG_SIZE + KEY_ID_SIZE)
        } catch (e: Exception) {
            null
        }
    }

    private fun algorithm(blob: ByteArray): Int =
        ((blob[0].toInt() and 0xFF) shl 8) or (blob[1].toInt() and 0xFF)

    private fun blake2b512(data: ByteArray): ByteArray {
        val digest = Blake2bDigest(ED25519_SIZE * 8)
        digest.update(data, 0, data.size)
        val out = ByteArray(ED25519_SIZE)
        digest.doFinal(out, 0)
        return out
    }

    private fun decodeBase64(value: String): ByteArray? = try {
        Base64.decode(value)
    } catch (e: Exception) {
        null
    }
}
