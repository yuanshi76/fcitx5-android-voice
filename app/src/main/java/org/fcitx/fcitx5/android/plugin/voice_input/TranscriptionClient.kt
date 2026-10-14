/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Voice Input Plugin Contributors
 */
package org.fcitx.fcitx5.android.plugin.voice_input

import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 参照 OpenAI Audio Transcriptions API 规范实现的调用客户端：
 *
 *   POST {baseUrl}/audio/transcriptions
 *   Authorization: Bearer {apiKey}
 *   Content-Type: multipart/form-data
 *     file        = <audio bytes>  (audio/wav)
 *     model       = whisper-1 ...
 *     language    = zh | en | ...   （可选，"auto" 时不传，由模型自检）
 *     response_format = json
 *
 * 返回 {"text": "..."} —— 识别出的文本。
 * 该规范同样适用于 DashScope / Azure / 本地 whisper.cpp-server 等兼容实现，
 * 因此 baseurl 与模型名均可由用户自行配置。
 */
class TranscriptionClient {

    class ApiException(val code: Int, message: String) : Exception(message)

    fun transcribe(wav: ByteArray, cfg: VoicePrefs): String {
        val url = URL("${cfg.baseUrl}/audio/transcriptions")
        val conn = url.openConnection() as HttpURLConnection
        val boundary = "----Fcitx5Voice${System.nanoTime()}"
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            if (cfg.apiKey.isNotEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer ${cfg.apiKey}")
            }
            conn.outputStream.use { out ->
                out.write(buildMultipart(boundary, wav, cfg))
            }

            val status = conn.responseCode
            val body = (if (status in 200..299) conn.inputStream else conn.errorStream)
                ?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""

            if (status !in 200..299) {
                Log.w(TAG, "HTTP $status: ${body.take(500)}")
                throw ApiException(status, "HTTP $status: ${body.take(200)}")
            }
            return parseText(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseText(body: String): String {
        // 兼容 response_format=json / verbose_json：取 "text" 字段
        return runCatching { JSONObject(body).optString("text", "") }.getOrElse {
            // 少数兼容实现直接返回纯文本
            if (body.trim().startsWith("{")) "" else body.trim()
        }
    }

    private fun buildMultipart(boundary: String, wav: ByteArray, cfg: VoicePrefs): ByteArray {
        val out = ByteArrayOutputStream()
        fun w(s: String) = out.write(s.toByteArray(Charsets.UTF_8))
        fun field(name: String, value: String) {
            w("--$boundary\r\n")
            w("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
            w("$value\r\n")
        }
        w("--$boundary\r\n")
        w("Content-Disposition: form-data; name=\"file\"; filename=\"voice.wav\"\r\n")
        w("Content-Type: audio/wav\r\n\r\n")
        out.write(wav)
        w("\r\n")
        field("model", cfg.model)
        if (cfg.language != "auto") field("language", cfg.language)
        field("response_format", "json")
        w("--$boundary--\r\n")
        return out.toByteArray()
    }

    companion object {
        private const val TAG = "VoiceTranscribe"
    }
}
