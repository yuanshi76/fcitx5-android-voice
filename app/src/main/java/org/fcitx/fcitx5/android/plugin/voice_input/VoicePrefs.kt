/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Voice Input Plugin Contributors
 */
package org.fcitx.fcitx5.android.plugin.voice_input

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * 插件配置：OpenAI 兼容语音转写接口的 Base URL / API Key / 模型名等，
 * 由用户在插件配置页面（[SettingsActivity]）自行填写。
 * 主程序（fcitx5-android release 版）"设置 -> 插件"页点击本插件时，
 * 会通过 org.fcitx.fcitx5.android.plugin.MANIFEST intent-filter 直接打开该页面。
 */
class VoicePrefs(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /** OpenAI 兼容根地址，通常以 /v1 结尾，例如 https://api.openai.com/v1 */
    val baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL)!!.trim().trimEnd('/')

    val apiKey: String
        get() = prefs.getString(KEY_API_KEY, "")!!.trim()

    val model: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL)!!.trim()

    /** "auto" 或 OpenAI 语言代码（zh/en/ja/ko/yue …） */
    val language: String
        get() = prefs.getString(KEY_LANGUAGE, "auto")!!

    /** 接口模式：auto / openai / dashscope_native / dashscope_compatible */
    val apiMode: String
        get() = prefs.getString(KEY_API_MODE, API_MODE_AUTO)!!

    /**
     * 即时热词（仅 qwen-audio-3.0-asr-flash 支持）：逗号分隔，可带权重 "马永泽,张三:5,李四:50"。
     * 权重 [1,5] 越大越倾向输出该词；50 为超级热词（最多 50 个）。
     */
    val hotwords: String
        get() = prefs.getString(KEY_HOTWORDS, "")!!.trim()

    /** 预编译热词列表 ID（百炼 createVocabulary 生成，可选） */
    val vocabularyId: String
        get() = prefs.getString(KEY_VOCAB_ID, "")!!.trim()

    /** 上下文增强：把最近几轮识别结果随音频一起发送，提升连贯性与专名召回 */
    val useContext: Boolean
        get() = prefs.getBoolean(KEY_USE_CONTEXT, true)

    /** 上屏后返回 fcitx5 时优先激活的子类型（extraValue = fcitx 输入法 uniqueName，"" 为自动） */
    val fcitxSubtype: String
        get() = prefs.getString(KEY_FCITX_SUBTYPE, "")!!.trim()

    val autoReturn: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RETURN, true)

    /** 是否已具备发起请求的最低配置 */
    fun isConfigured(): Boolean = baseUrl.isNotBlank() && model.isNotBlank()

    /**
     * 听写引擎路由（确定性规则，见方案约定）：
     * 1. Base URL 以 ws:// 或 wss:// 开头，或接口模式显式选了 capswriter → CapsWriter 实时；
     * 2. 模型名含 realtime → 百炼实时（wss 由 https baseUrl 自动推导）；
     * 3. 其余 → 同步转写（openai / 百炼原生 / 百炼兼容）。
     */
    fun engineKind(): EngineKind {
        val b = baseUrl.lowercase()
        return when {
            b.startsWith("ws://") || b.startsWith("wss://") || apiMode == API_MODE_CAPSWRITER ->
                EngineKind.CapsWriterRealtime
            model.contains("realtime", ignoreCase = true) -> EngineKind.BailianRealtime
            else -> EngineKind.Sync
        }
    }

    companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
        const val KEY_LANGUAGE = "language"
        const val KEY_API_MODE = "api_mode"
        const val KEY_HOTWORDS = "hotwords"
        const val KEY_VOCAB_ID = "vocabulary_id"
        const val KEY_USE_CONTEXT = "use_context"
        const val KEY_AUTO_RETURN = "auto_return_fcitx"
        const val KEY_FCITX_SUBTYPE = "fcitx_return_subtype"

        const val API_MODE_AUTO = "auto"
        const val API_MODE_OPENAI = "openai"
        const val API_MODE_DS_NATIVE = "dashscope_native"
        const val API_MODE_DS_COMPAT = "dashscope_compatible"
        const val API_MODE_CAPSWRITER = "capswriter"

        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_MODEL = "whisper-1"

        /** 录音参数：16 kHz 单声道 16-bit PCM（Whisper 推荐输入格式） */
        const val SAMPLE_RATE = 16000

        /** 单次录音上限，防止忘记停止（16k/16bit/mono ≈ 32KB/s，120s 约 3.8MB，
         *  base64 后约 5.1MB，在百炼 10MB 与 OpenAI 25MB 限制内） */
        const val MAX_RECORD_SECONDS = 120
    }
}
