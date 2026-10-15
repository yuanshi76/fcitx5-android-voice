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
            when {
                // 百炼系：qwen-audio-3.0-asr-flash 走原生 multimodal-generation REST
                //（官方 fun-asr-flash HTTP 文档确认支持 data:audio/wav;base64 输入）。
                // 注意：此前的 "url error" 实为路径拼接 bug（已修），并非原生拒收 base64。
                b.contains("maas.aliyuncs.com") || b.contains("dashscope.aliyuncs.com") ->
                    Mode.DashScopeNative
                else -> Mode.OpenAi
            }
        }
    }

    fun transcribe(wav: ByteArray, cfg: VoicePrefs, context: List<String> = emptyList()): String {
        // qwen-audio-*-realtime-* 是纯 WebSocket 实时模型，不支持任何同步 HTTP 转写
        //（HTTP 调用只会得到 {"status_message":"Success."} 这类回执）
        if (cfg.model.contains("realtime", ignoreCase = true)) {
            throw ApiException(
                400,
                "「${cfg.model}」是实时语音对话模型（WebSocket），不能用于当前同步语音输入；" +
                        "非实时识别请改用 qwen-audio-3.0-asr-flash（实时听写功能将在下一版本支持）"
            )
        }
        return when (resolveMode(cfg)) {
            Mode.OpenAi -> transcribeOpenAi(wav, cfg, context)
            Mode.DashScopeNative -> transcribeDashScope(wav, cfg, native = true, context)
            Mode.DashScopeCompatible -> transcribeDashScope(wav, cfg, native = false, context)
        }
    }

    // ---------- 热词 / 语言辅助 ----------

    /** 解析 "马永泽,张三:5,李四:50" -> {"马永泽":3,"张三":5,"李四":50}（无权重默认3） */
    private fun parseHotwords(raw: String): JSONObject? {
        if (raw.isBlank()) return null
        val obj = JSONObject()
        raw.split(Regex("[,，;；\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
            val idx = token.lastIndexOf(':')
            if (idx > 0 && idx < token.length - 1) {
                val word = token.substring(0, idx).trim()
                val weight = token.substring(idx + 1).trim().toIntOrNull() ?: 3
                if (word.isNotEmpty()) obj.put(word, weight.coerceIn(1, 50))
            } else {
                obj.put(token, 3)
            }
        }
        return if (obj.length() == 0) null else obj
    }

    /** 语言配置 -> language_hints 数组（auto 时不传） */
    private fun languageHints(cfg: VoicePrefs): JSONArray? =
        if (cfg.language == "auto") null else JSONArray().put(cfg.language)

    // ---------- OpenAI multipart ----------

    private fun transcribeOpenAi(wav: ByteArray, cfg: VoicePrefs, context: List<String>): String {
        val endpoint = cfg.baseUrl.let {
            if (it.endsWith("/audio/transcriptions")) it else "$it/audio/transcriptions"
        }
        val boundary = "----Fcitx5Voice${System.nanoTime()}"
        val body = buildMultipart(boundary, wav, cfg, context)
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

    private fun buildMultipart(
        boundary: String, wav: ByteArray, cfg: VoicePrefs, context: List<String>
    ): ByteArray {
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
        // OpenAI 无原生热词：把热词与最近上下文塞进 prompt 字段（官方推荐的提示途径）
        val hotwordList = parseHotwords(cfg.hotwords)?.let { vo ->
            vo.keys().asSequence().toList().joinToString("、")
        }
        val prompt = buildList {
            if (!hotwordList.isNullOrBlank()) add("热词: $hotwordList")
            if (cfg.useContext && context.isNotEmpty())
                add("上下文: ${context.drop((context.size - 3).coerceAtLeast(0)).joinToString(" ")}")
        }.joinToString("\n")
        if (prompt.isNotEmpty()) field("prompt", prompt.take(800))
        w("--$boundary--\r\n")
        return out.toByteArray()
    }

    // ---------- DashScope（原生 / OpenAI兼容 chat） ----------

    /**
     * 取服务根地址（scheme://host[/自定义前缀]）：剥掉百炼已知的 API 版本路径，
     * 以便在不同模式间正确重拼。例如
     *   https://token-plan.cn-beijing.maas.aliyuncs.com/compatible-mode/v1
     *   -> https://token-plan.cn-beijing.maas.aliyuncs.com
     * 原生接口在 /api/v1/... 下，兼容模式在 /compatible-mode/v1/... 下，两者不能混拼。
     */
    private fun apiRoot(baseUrl: String): String {
        var b = baseUrl.trimEnd('/')
        for (suffix in listOf("/compatible-mode/v1", "/api/v1", "/v1")) {
            if (b.endsWith(suffix)) {
                b = b.removeSuffix(suffix)
                break
            }
        }
        return b
    }

    private fun dashScopeDataUrl(wav: ByteArray): String =
        "data:audio/wav;base64," + Base64.encodeToString(wav, Base64.NO_WRAP)

    private fun transcribeDashScope(
        wav: ByteArray, cfg: VoicePrefs, native: Boolean, context: List<String>
    ): String {
        val endpoint = if (native) {
            val b = cfg.baseUrl
            when {
                b.endsWith("/multimodal-generation/generation") -> b
                else -> "${apiRoot(b)}/api/v1/services/aigc/multimodal-generation/generation"
            }
        } else {
            val b = cfg.baseUrl
            when {
                b.endsWith("/chat/completions") -> b
                b.endsWith("/v1") -> "$b/chat/completions"
                else -> "${apiRoot(b)}/compatible-mode/v1/chat/completions"
            }
        }

        val audioPart = JSONObject()
            .put("type", "input_audio")
            .put("input_audio", JSONObject().put("data", dashScopeDataUrl(wav)))
        val userMsg = JSONObject()
            .put("role", "user")
            .put("content", JSONArray().put(audioPart))

        // 上下文增强：最近几轮识别结果作为 input_text 消息，音频消息必须放最后
        //（官方约束：上下文消息各最多 5 条，每轮文本总长 ≤400 字符）
        val messages = JSONArray()
        if (cfg.useContext) {
            context.drop((context.size - 5).coerceAtLeast(0)).forEach { prev ->
                messages.put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", JSONArray().put(
                            JSONObject()
                                .put("type", "input_text")
                                .put("text", prev.take(400))
                        ))
                )
            }
        }
        messages.put(userMsg)

        val json = if (native) {
            JSONObject()
                .put("model", cfg.model)
                .put("input", JSONObject().put("messages", messages))
                .put("parameters", JSONObject()
                    .put("format", "wav")
                    .put("sample_rate", VoicePrefs.SAMPLE_RATE.toString())
                    .apply {
                        languageHints(cfg)?.let { put("language_hints", it) }
                        // 即时热词仅 qwen-audio-3.0-asr-flash 支持
                        parseHotwords(cfg.hotwords)?.let { put("vocabulary", it) }
                        if (cfg.vocabularyId.isNotEmpty()) put("vocabulary_id", cfg.vocabularyId)
                    })
        } else {
            JSONObject()
                .put("model", cfg.model)
                .put("messages", messages)
                .put("stream", false)
                .apply {
                    val asrOptions = JSONObject()
                    if (cfg.language != "auto") asrOptions.put("language", cfg.language)
                    if (asrOptions.length() > 0) put("asr_options", asrOptions)
                }
        }

        val headers = if (native) mapOf("X-DashScope-SSE" to "disable") else null
        val resp = httpPost(
            endpoint, "application/json", json.toString().toByteArray(Charsets.UTF_8),
            cfg.apiKey, headers
        )
        return if (native) parseDashScopeNative(resp) else parseChatCompletion(resp)
    }

    /**
     * 原生响应，兼容两种结构族：
     * - qwen-asr 族：output.choices[0].message.content[].text
     * - fun-asr / qwen-audio-3.0-asr-flash 族：output.text（全文）、output.sentence（对象或数组）
     */
    private fun parseDashScopeNative(body: String): String {
        val root = JSONObject(body)
        // 网关可能在 HTTP 200 里返回业务错误（{"code":"...","message":"..."}），不要吞掉
        requireNoGatewayError(root, body)
        val output = root.optJSONObject("output")
            ?: throw IllegalStateException("响应缺少 output 字段: ${body.take(200)}")

        // 1) choices 结构
        output.optJSONArray("choices")?.takeIf { it.length() > 0 }?.let { choices ->
            val content = choices.getJSONObject(0).optJSONObject("message")?.opt("content")
            val text = when (content) {
                is JSONArray -> (0 until content.length())
                    .joinToString("") { content.optJSONObject(it)?.optString("text", "") ?: "" }
                is String -> content
                else -> ""
            }
            if (text.isNotBlank()) return text
        }
        // 2) output.text 顶层全文
        output.optString("text", "").takeIf { it.isNotBlank() }?.let { return it }
        // 3) output.sentence（对象或数组）
        when (val s = output.opt("sentence")) {
            is JSONObject ->
                s.optString("text", "").takeIf { it.isNotBlank() }?.let { return it }
            is JSONArray -> {
                val joined = (0 until s.length())
                    .joinToString("") { s.optJSONObject(it)?.optString("text", "") ?: "" }
                if (joined.isNotBlank()) return joined
            }
        }
        throw IllegalStateException("响应中未找到识别文本: ${body.take(200)}")
    }

    /** chat/completions 响应：choices[0].message.content 是字符串 */
    private fun parseChatCompletion(body: String): String {
        val root = JSONObject(body)
        requireNoGatewayError(root, body)
        val choices = root.optJSONArray("choices")
        if (choices == null || choices.length() == 0)
            throw IllegalStateException("响应缺少 choices 字段: ${body.take(200)}")
        return choices.getJSONObject(0)
            .optJSONObject("message")?.optString("content", "") ?: ""
    }

    /** HTTP 200 但 body 带网关错误码时，抛出可读异常 */
    private fun requireNoGatewayError(root: JSONObject, body: String) {
        val code = root.optString("code", "")
        if (code.isNotEmpty() && !root.has("output") && !root.has("choices")) {
            val message = root.optString("message", "")
            throw ApiException(200, listOf(code, message).filter { it.isNotEmpty() }
                .joinToString(": ").ifEmpty { "响应异常: ${body.take(200)}" })
        }
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
