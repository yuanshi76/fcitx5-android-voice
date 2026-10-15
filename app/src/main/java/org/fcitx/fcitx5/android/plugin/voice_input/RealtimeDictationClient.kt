/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Voice Input Plugin Contributors
 */
package org.fcitx.fcitx5.android.plugin.voice_input

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * 实时听写引擎（qwen-audio-3.0-realtime-plus 等 realtime 模型）。
 *
 * 协议：百炼 Qwen-Audio Realtime WebSocket API（OpenAI Realtime 风格）
 *   wss://{host}/api-ws/v1/realtime?model={model}   Header: Authorization: Bearer {key}
 *   客户端：session.update（modalities=text、server_vad 断句、instructions 注入热词）
 *           input_audio_buffer.append（16kHz/16bit/mono PCM 的 base64，100ms 一帧）
 *   服务端：conversation.item.input_audio_transcription.delta   -> {text 已定, stash 暂存}
 *           conversation.item.input_audio_transcription.completed -> {transcript 整句}
 *           error -> {error.message}
 *
 * 交互：按住说话期间维持一个 WS 会话；server_vad 自动断句，
 * 每句 completed 即 commitText 上屏，delta 实时以 composing text 显示（边说边上屏）。
 */
class RealtimeDictationClient(
    private val cfg: VoicePrefs,
    private val callback: Callback
) {

    interface Callback {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onFailure(message: String)
        fun onClosed()
    }

    /** 音频已开始上行（WS 握手 + session 就绪后），测试流程用于对齐计时 */
    @Volatile
    var onAudioReady: (() -> Unit)? = null

    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null

    @Volatile
    private var sessionReady = false

    @Volatile
    private var streaming = false

    /** 已送出音频但尚未收到该句 completed */
    @Volatile
    private var pendingFinal = false

    @Volatile
    private var closed = false

    private var recorder: AudioRecord? = null
    private var audioThread: Thread? = null

    fun start() {
        val request = Request.Builder()
            .url(realtimeUrl(cfg))
            .apply {
                if (cfg.apiKey.isNotEmpty()) addHeader("Authorization", "Bearer ${cfg.apiKey}")
            }
            .build()
        webSocket = http.newWebSocket(request, listener)
    }

    /** 停止送音；等待最后一句 completed（最多 1.5s）后关闭会话 */
    fun stop() {
        stopAudio()
        if (!pendingFinal) {
            closeNow()
            return
        }
        thread(name = "voice-rt-drain") {
            val deadline = System.currentTimeMillis() + 2500
            while (pendingFinal && System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(50)
                } catch (_: InterruptedException) {
                    break
                }
            }
            closeNow()
        }
    }

    /** 立即放弃并关闭（收起键盘等场景） */
    fun cancel() {
        pendingFinal = false
        stopAudio()
        closeNow()
    }

    private fun closeNow() {
        if (closed) return
        closed = true
        runCatching { webSocket?.close(1000, null) }
        webSocket = null
        callback.onClosed()
    }

    // ---------- WebSocket ----------

    private val listener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            webSocket.send(sessionUpdate().toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val obj = runCatching { JSONObject(text) }.getOrNull() ?: return
            when (obj.optString("type")) {
                "session.created", "session.updated" ->
                    if (!sessionReady) {
                        sessionReady = true
                        startAudio()
                    }

                "conversation.item.input_audio_transcription.delta" -> {
                    val partial = obj.optString("text") + obj.optString("stash")
                    if (partial.isNotBlank()) {
                        pendingFinal = true
                        callback.onPartial(partial)
                    }
                }

                "conversation.item.input_audio_transcription.completed" -> {
                    pendingFinal = false
                    val transcript = obj.optString("transcript")
                    if (transcript.isNotBlank()) callback.onFinal(transcript)
                }

                "error" -> {
                    val msg = obj.optJSONObject("error")?.optString("message")
                        ?.ifEmpty { null } ?: obj.optString("message", "实时服务返回错误")
                    callback.onFailure(msg)
                    pendingFinal = false
                    closeNow()
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (closed) return
            closed = true
            stopAudio()
            callback.onFailure(t.message ?: "WebSocket 连接失败")
            callback.onClosed()
        }
    }

    private fun sessionUpdate(): JSONObject {
        val session = JSONObject()
            .put("modalities", JSONArray().put("text"))
            .put("input_audio_format", "pcm")
            .put(
                "turn_detection",
                JSONObject()
                    .put("type", "server_vad")
                    .put("threshold", 0.5)
                    .put("silence_duration_ms", 700)
            )
        // 对话模型会"回答"，用系统指令约束其只做转写；并注入即时热词
        val instructions = buildString {
            append("你是语音输入法的听写引擎。把用户语音逐字转写为文本，")
            append("不要回答问题、不要解释、不要翻译、不要补充任何非转写内容。")
            val hot = hotwordHint()
            if (hot.isNotEmpty()) append("注意这些专有词汇的正确写法：").append(hot).append("。")
        }
        session.put("instructions", instructions)
        return JSONObject()
            .put("type", "session.update")
            .put("session", session)
    }

    private fun hotwordHint(): String =
        parseHotwordsSimple(cfg.hotwords).joinToString("、")

    // ---------- 音频流 ----------

    @SuppressLint("MissingPermission") // 调用方已检查 RECORD_AUDIO
    private fun startAudio() {
        if (streaming || closed) return
        val minSize = AudioRecord.getMinBufferSize(
            VoicePrefs.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(VoicePrefs.SAMPLE_RATE / 2)
        val r = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                VoicePrefs.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minSize * 2
            )
        } catch (e: Exception) {
            callback.onFailure("AudioRecord 初始化失败: ${e.message}")
            return
        }
        if (r.state != AudioRecord.STATE_INITIALIZED) {
            r.release()
            callback.onFailure("AudioRecord 不可用")
            return
        }
        recorder = r
        streaming = true
        onAudioReady?.invoke()
        val frame = ByteArray(3200) // 100ms @16k/16bit/mono
        audioThread = thread(name = "voice-rt-audio") {
            try {
                r.startRecording()
                while (streaming && !closed) {
                    val n = r.read(frame, 0, frame.size)
                    if (n <= 0) continue
                    val payload = if (n == frame.size) frame else frame.copyOf(n)
                    val b64 = Base64.encodeToString(payload, Base64.NO_WRAP)
                    val msg = JSONObject()
                        .put("type", "input_audio_buffer.append")
                        .put("audio", b64)
                        .toString()
                    if (webSocket?.send(msg) != true) break
                }
            } catch (e: Exception) {
                // stop/cancel 竞态，忽略
            }
        }
    }

    private fun stopAudio() {
        streaming = false
        audioThread?.join(300)
        audioThread = null
        recorder?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        recorder = null
    }

    companion object {
        /** 复用热词解析："词1,词2:5" -> [词1, 词2] */
        fun parseHotwordsSimple(raw: String): List<String> =
            raw.split(Regex("[,，;；\\n]+")).map { token ->
                token.substringBefore(':').substringBefore('：').trim()
            }.filter { it.isNotEmpty() }

        /** 由 Base URL 推导实时 WS 地址：换 scheme、路径 /api-ws/v1/realtime?model= */
        fun realtimeUrl(cfg: VoicePrefs): String {
            var root = cfg.baseUrl.trimEnd('/')
            for (suffix in listOf("/compatible-mode/v1", "/api/v1", "/v1")) {
                if (root.endsWith(suffix)) {
                    root = root.removeSuffix(suffix)
                    break
                }
            }
            val wsRoot = when {
                root.startsWith("https://") -> "wss://" + root.removePrefix("https://")
                root.startsWith("http://") -> "ws://" + root.removePrefix("http://")
                else -> root
            }
            return wsRoot + "/api-ws/v1/realtime?model=" +
                    URLEncoder.encode(cfg.model, "UTF-8")
        }
    }
}
