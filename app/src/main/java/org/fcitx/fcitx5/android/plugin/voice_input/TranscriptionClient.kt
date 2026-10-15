/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Voice Input Plugin Contributors
 */
package org.fcitx.fcitx5.android.plugin.voice_input

import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 语音转写客户端，支持三种接口形态（"接口模式"配置项，auto 自动识别）：
 *
 * 1. openai —— OpenAI Audio Transcriptions 规范：
 *    POST {baseUrl}/audio/transcriptions，multipart（file/model/language/response_format），
 *    响应取 JSON 的 "text" 字段。
 *
 * 2. dashscope_native —— 阿里云百炼（DashScope）原生多模态生成接口：
 *    POST {root}/api/v1/services/aigc/multimodal-generation/generation
 *    JSON body：input.messages[].content[].input_audio.data 传 base64 Data URL，
 *    parameters.format=wav、parameters.sample_rate=16000，
 *    Header X-DashScope-SSE: disable；
 *    响应取 output.choices[0].message.content[].text。
 *    （qwen-audio-3.0-asr-flash 等 Token Plan 专属域名场景用这个）
 *
 * 3. dashscope_compatible —— 百炼 OpenAI 兼容 chat/completions + input_audio：
 *    POST {root}/compatible-mode/v1/chat/completions，
 *    响应取 choices[0].message.content。
 *
 * auto：baseUrl 含 maas.aliyuncs.com / dashscope.aliyuncs.com 时选 dashscope_native，
 * 否则选 openai。
 */
class TranscriptionClient {

    class ApiException(val code: Int, message: String) : Exception(message)

    enum class Mode { OpenAi, DashScopeNative, DashScopeCompatible }

    fun resolveMode(cfg: VoicePrefs): Mode = when (cfg.apiMode) {
        VoicePrefs.API_MODE_OPENAI -> Mode.OpenAi
        VoicePrefs.API_MODE_DS_NATIVE -> Mode.DashScopeNative
        VoicePrefs.API_MODE_DS_COMPAT -> Mode.DashScopeCompatible
        else -> {
            val b = cfg.baseUrl.lowercase()
            if (b.contains("maas.aliyuncs.com") || b.contains("dashscope.aliyuncs.com"))
                Mode.DashScopeNative
            else Mode.OpenAi
        }
    }

    fun transcribe(wav: ByteArray, cfg: VoicePrefs): String = when (resolveMode(cfg)) {
        Mode.OpenAi -> transcribeOpenAi(wav, cfg)
        Mode.DashScopeNative -> transcribeDashScope(wav, cfg, native = true)
        Mode.DashScopeCompatible -> transcribeDashScope(wav, cfg, native = false)
    }

    // ---------- OpenAI multipart ----------

    private fun transcribeOpenAi(wav: ByteArray, cfg: VoicePrefs): String {
        val endpoint = cfg.baseUrl.let {
            if (it.endsWith("/audio/transcriptions")) it else "$it/audio/transcriptions"
        }
        val boundary = "----Fcitx5Voice${System.nanoTime()}"
        val body = buildMultipart(boundary, wav, cfg)
        val resp = httpPost(
            endpoint, "multipart/form-data; boundary=$boundary", body, cfg.apiKey, null
        )
        return parseOpenAiText(resp)
    }

    private fun parseOpenAiText(body: String): String {
        return runCatching { JSONObject(body).optString("text", "") }.getOrElse {
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

    // ---------- DashScope（原生 / OpenAI兼容 chat） ----------

    private fun dashScopeDataUrl(wav: ByteArray): String =
        "data:audio/wav;base64," + Base64.encodeToString(wav, Base64.NO_WRAP)

    private fun transcribeDashScope(wav: ByteArray, cfg: VoicePrefs, native: Boolean): String {
        val endpoint = if (native) {
            val b = cfg.baseUrl
            when {
                b.endsWith("/multimodal-generation/generation") -> b
                b.endsWith("/api/v1") -> "$b/services/aigc/multimodal-generation/generation"
                b.endsWith("/v1") -> "$b/services/aigc/multimodal-generation/generation"
                else -> "$b/api/v1/services/aigc/multimodal-generation/generation"
            }
        } else {
            val b = cfg.baseUrl
            when {
                b.endsWith("/chat/completions") -> b
                b.endsWith("/compatible-mode/v1") -> "$b/chat/completions"
                b.endsWith("/v1") && b.contains("compatible-mode") -> "$b/chat/completions"
                else -> "$b/compatible-mode/v1/chat/completions"
            }
        }

        val audioPart = JSONObject()
            .put("type", "input_audio")
            .put("input_audio", JSONObject().put("data", dashScopeDataUrl(wav)))
        val userMsg = JSONObject()
            .put("role", "user")
            .put("content", JSONArray().put(audioPart))

        val json = if (native) {
            JSONObject()
                .put("model", cfg.model)
                .put("input", JSONObject().put("messages", JSONArray().put(userMsg)))
                .put("parameters", JSONObject()
                    .put("format", "wav")
                    .put("sample_rate", VoicePrefs.SAMPLE_RATE.toString()))
        } else {
            JSONObject()
                .put("model", cfg.model)
                .put("messages", JSONArray().put(userMsg))
                .put("stream", false)
                .apply {
                    if (cfg.language != "auto") {
                        put("asr_options", JSONObject().put("language", cfg.language))
                    }
                }
        }

        val headers = if (native) mapOf("X-DashScope-SSE" to "disable") else null
        val resp = httpPost(
            endpoint, "application/json", json.toString().toByteArray(Charsets.UTF_8),
            cfg.apiKey, headers
        )
        return if (native) parseDashScopeNative(resp) else parseChatCompletion(resp)
    }

    /** 原生响应：output.choices[0].message.content 是 [{text:...}] 数组 */
    private fun parseDashScopeNative(body: String): String {
        val output = JSONObject(body).optJSONObject("output") ?: return ""
        val choices = output.optJSONArray("choices") ?: return ""
        if (choices.length() == 0) return ""
        val content = choices.getJSONObject(0)
            .optJSONObject("message")?.opt("content")
        return when (content) {
            is JSONArray -> (0 until content.length())
                .joinToString("") { content.optJSONObject(it)?.optString("text", "") ?: "" }
            is String -> content
            else -> ""
        }
    }

    /** chat/completions 响应：choices[0].message.content 是字符串 */
    private fun parseChatCompletion(body: String): String {
        val choices = JSONObject(body).optJSONArray("choices") ?: return ""
        if (choices.length() == 0) return ""
        return choices.getJSONObject(0)
            .optJSONObject("message")?.optString("content", "") ?: ""
    }

    // ---------- HTTP 公共部分 ----------

    private fun httpPost(
        endpoint: String,
        contentType: String,
        body: ByteArray,
        apiKey: String,
        extraHeaders: Map<String, String>?
    ): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("Content-Type", contentType)
            if (apiKey.isNotEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            extraHeaders?.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.outputStream.use { it.write(body) }

            val status = conn.responseCode
            val text = (if (status in 200..299) conn.inputStream else conn.errorStream)
                ?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            if (status !in 200..299) {
                Log.w(TAG, "HTTP $status: ${text.take(500)}")
                // DashScope 错误体形如 {"code":"...","message":"...","request_id":"..."}
                val friendly = runCatching {
                    val o = JSONObject(text)
                    val code = o.optString("code")
                    val msg = o.optString("message")
                    listOf(code, msg).filter { it.isNotEmpty() }.joinToString(": ")
                }.getOrDefault("")
                throw ApiException(status, friendly.ifEmpty { "HTTP $status: ${text.take(200)}" })
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val TAG = "VoiceTranscribe"
    }
}
