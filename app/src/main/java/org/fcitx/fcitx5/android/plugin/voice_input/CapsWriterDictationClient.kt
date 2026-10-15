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
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * CapsWriter-Offline 自建服务端实时识别引擎。
 *
 * 协议（docs/第三方WebSocket实时识别接入说明.md）：
 *   ws://{host}:6016，子协议 "binary"，可选 X-CapsWriter-Key 鉴权头；
 *   上行 JSON 文本帧 AudioMessage：task_id / source="mic" /
 *     data=base64(float32 LE,16kHz,mono) / is_final / time_start /
 *     seg_duration / seg_overlap / context / language；
 *   下行 RecognitionMessage：is_final=false 为 partial（text 为累积全文），
 *     is_final=true 为带标点的最终结果，收到即任务结束。
 *
 * 生命周期：一次按住说话 = 一个 task_id；松开后补发 data="" 的 final 空包，
 * 等待最终结果（服务端尾片推理，最长 15s）后关闭连接。
 */
class CapsWriterDictationClient(
    private val cfg: VoicePrefs,
    private val context: String,
    private val callback: DictationEngine.Callback
) : DictationEngine {

    override var onAudioReady: (() -> Unit)? = null

    private val http = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1)) // 允许 ws:// 明文
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val taskId: String = UUID.randomUUID().toString()
    private val timeStart: Double = System.currentTimeMillis() / 1000.0

    private var webSocket: WebSocket? = null
    @Volatile private var streaming = false
    @Volatile private var finalSent = false
    @Volatile private var pendingFinal = false
    @Volatile private var closed = false
    private var recorder: AudioRecord? = null
    private var audioThread: Thread? = null

    override fun start() {
        val request = Request.Builder()
            .url(resolveUrl(cfg))
            .header("Sec-WebSocket-Protocol", "binary")
            .apply {
                if (cfg.apiKey.isNotEmpty()) header("X-CapsWriter-Key", cfg.apiKey)
            }
            .build()
        webSocket = http.newWebSocket(request, listener)
    }

    override fun stop() {
        stopAudio()
        val ws = webSocket
        if (ws == null || closed) {
            closeNow()
            return
        }
        if (!finalSent) {
            finalSent = true
            if (pendingFinal) {
                // 收尾空包：服务端把剩余缓冲整体提交并回最终结果
                ws.send(buildMessage(ByteArray(0), isFinal = true))
            } else {
                // 从未成功送出音频：直接结束
                closeNow()
                return
            }
        }
        thread(name = "voice-cw-drain") {
            val deadline = System.currentTimeMillis() + 15_000 // 尾片推理可能较慢
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

    override fun cancel() {
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
            startAudio()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val obj = runCatching { JSONObject(text) }.getOrNull() ?: return
            if (obj.optString("task_id") != taskId) return
            val recognized = obj.optString("text")
            if (obj.optBoolean("is_final")) {
                pendingFinal = false
                if (recognized.isNotBlank()) callback.onFinal(recognized)
            } else {
                if (recognized.isNotBlank()) {
                    pendingFinal = true
                    callback.onPartial(recognized)
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (closed) return
            closed = true
            stopAudio()
            val detail = when {
                response?.code == 401 -> "401 鉴权失败（检查 API Key 与服务端 ws_api_key）"
                else -> t.message ?: "WebSocket 连接失败"
            }
            callback.onFailure(detail)
            callback.onClosed()
        }
    }

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
        val shorts = ShortArray(1600) // 100ms @16k
        audioThread = thread(name = "voice-cw-audio") {
            try {
                r.startRecording()
                while (streaming && !closed) {
                    val n = r.read(shorts, 0, shorts.size)
                    if (n <= 0) continue
                    val payload = if (n == shorts.size) shorts else shorts.copyOf(n)
                    val b64 = Base64.encodeToString(toFloat32Le(payload), Base64.NO_WRAP)
                    val msg = buildMessageRaw(b64, isFinal = false)
                    if (webSocket?.send(msg) == true) {
                        pendingFinal = true
                    } else {
                        break
                    }
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

    private fun buildMessage(pcmFloat32: ByteArray, isFinal: Boolean): String =
        buildMessageRaw(Base64.encodeToString(pcmFloat32, Base64.NO_WRAP), isFinal)

    private fun buildMessageRaw(dataB64: String, isFinal: Boolean): String =
        JSONObject()
            .put("task_id", taskId)
            .put("source", "mic")
            .put("data", dataB64)
            .put("is_final", isFinal)
            .put("time_start", timeStart)
            .put("seg_duration", SEG_DURATION)
            .put("seg_overlap", SEG_OVERLAP)
            .put("context", context)
            .put("language", cfg.language)
            .toString()

    companion object {
        /** 实时场景推荐值（文档 §3）：约 8s 出第一条 partial */
        const val SEG_DURATION = 4.0
        const val SEG_OVERLAP = 2.0

        /** int16 PCM -> float32 小端（服务端内部格式） */
        fun toFloat32Le(samples: ShortArray): ByteArray {
            val buf = ByteBuffer.allocate(samples.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) buf.putFloat(s / 32768f)
            return buf.array()
        }

        /**
         * 解析实际 WS 地址：
         * - ws:// 或 wss:// 开头：原样使用；无端口则补默认 6016
         * - http(s)://（显式选了 CapsWriter 模式）：换 scheme；
         *   端口 6017（其 OpenAI 端口）或缺省 → 映射到 WS 默认 6016
         */
        fun resolveUrl(cfg: VoicePrefs): String {
            var b = cfg.baseUrl.trimEnd('/')
            val lower = b.lowercase()
            when {
                lower.startsWith("https://") -> b = "wss://" + b.removePrefix("https://")
                lower.startsWith("http://") -> b = "ws://" + b.removePrefix("http://")
            }
            if (b.contains(":6017")) b = b.replace(":6017", ":6016")
            val m = Regex("^(wss?)://([^/:]+)(:\\d+)?(/.*)?$").matchEntire(b) ?: return b
            if (m.groupValues[3].isEmpty()) {
                b = "${m.groupValues[1]}://${m.groupValues[2]}:6016${m.groupValues[4]}"
            }
            return b
        }
    }
}
