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

    val autoReturn: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RETURN, true)

    /** 是否已具备发起请求的最低配置 */
    fun isConfigured(): Boolean = baseUrl.isNotBlank() && model.isNotBlank()

    companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
        const val KEY_LANGUAGE = "language"
        const val KEY_AUTO_RETURN = "auto_return_fcitx"

        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_MODEL = "whisper-1"

        /** 录音参数：16 kHz 单声道 16-bit PCM（Whisper 推荐输入格式） */
        const val SAMPLE_RATE = 16000

        /** 单次录音上限，防止忘记停止 */
        const val MAX_RECORD_SECONDS = 60
    }
}
