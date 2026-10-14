/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Voice Input Plugin Contributors
 */
package org.fcitx.fcitx5.android.plugin.voice_input

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * 16 kHz 单声道 16-bit PCM 录音，stop 时封装为 WAV（RIFF）字节。
 * WAV 是 OpenAI /audio/transcriptions 支持的通用格式，
 * whisper.cpp-server / faster-whisper 等兼容实现均可直接接收。
 */
class WavRecorder {

    private var record: AudioRecord? = null
    private val buffer = ByteArrayOutputStream()
    private var thread: Thread? = null
    @Volatile
    private var running = false

    // 调用方（VoiceInputMethodService / SettingsActivity）均已先行检查 RECORD_AUDIO 运行时权限
    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        val minSize = AudioRecord.getMinBufferSize(
            VoicePrefs.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(VoicePrefs.SAMPLE_RATE) // >= 0.5s 缓冲
        val r = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                VoicePrefs.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minSize * 2
            )
        } catch (e: Exception) {
            return false
        }
        if (r.state != AudioRecord.STATE_INITIALIZED) {
            r.release()
            return false
        }
        record = r
        buffer.reset()
        running = true
        thread = thread(name = "voice-record") {
            val chunk = ByteArray(minSize)
            try {
                r.startRecording()
                while (running) {
                    val n = r.read(chunk, 0, chunk.size)
                    if (n > 0) buffer.write(chunk, 0, n)
                }
            } catch (e: Exception) {
                // ignore: cancelled by stop/cancel
            }
        }
        return true
    }

    /** 停止并返回完整 WAV 字节；录音失败或为空返回 null */
    fun stopAndRelease(): ByteArray? {
        running = false
        thread?.join(1000)
        thread = null
        release()
        val pcm = buffer.toByteArray()
        buffer.reset()
        if (pcm.isEmpty()) return null
        return wavFromPcm(pcm, VoicePrefs.SAMPLE_RATE)
    }

    fun cancel() {
        running = false
        thread?.join(500)
        thread = null
        release()
        buffer.reset()
    }

    private fun release() {
        record?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        record = null
    }

    companion object {
        fun wavFromPcm(pcm: ByteArray, sampleRate: Int): ByteArray {
            val channels = 1
            val bitsPerSample = 16
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val dataLen = pcm.size
            val out = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)
            out.put("RIFF".toByteArray())
            out.putInt(36 + dataLen)
            out.put("WAVE".toByteArray())
            out.put("fmt ".toByteArray())
            out.putInt(16)                       // fmt chunk size
            out.putShort(1.toShort())            // PCM
            out.putShort(channels.toShort())
            out.putInt(sampleRate)
            out.putInt(byteRate)
            out.putShort((channels * bitsPerSample / 8).toShort()) // block align
            out.putShort(bitsPerSample.toShort())
            out.put("data".toByteArray())
            out.putInt(dataLen)
            out.put(pcm)
            return out.array()
        }
    }
}
