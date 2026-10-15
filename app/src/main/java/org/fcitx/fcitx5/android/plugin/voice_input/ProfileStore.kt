/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Voice Input Plugin Contributors
 */
package org.fcitx.fcitx5.android.plugin.voice_input

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONObject

/** 一套完整的转写渠道配置 */
data class VoiceProfile(
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val language: String,
    val apiMode: String
)

/**
 * 语音方案存储：把 baseurl + apikey + 模型名 + 语言 打包成命名方案，
 * 支持保存/切换/覆盖/删除，便于在多个 TTS/ASR 渠道间快速切换。
 * 方案以 JSON 数组存于默认 SharedPreferences；
 * "应用方案" 即把方案值写回 [VoicePrefs] 的四个键，其余代码零改动生效。
 */
class ProfileStore(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun list(): List<VoiceProfile> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                VoiceProfile(
                    name = o.getString("name"),
                    baseUrl = o.optString("baseUrl"),
                    apiKey = o.optString("apiKey"),
                    model = o.optString("model"),
                    language = o.optString("language"),
                    apiMode = o.optString("apiMode", VoicePrefs.API_MODE_AUTO)
                        .ifEmpty { VoicePrefs.API_MODE_AUTO }
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<VoiceProfile>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("name", p.name)
                    .put("baseUrl", p.baseUrl)
                    .put("apiKey", p.apiKey)
                    .put("model", p.model)
                    .put("language", p.language)
                    .put("apiMode", p.apiMode)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    /** 新增或按名覆盖一个方案 */
    fun put(profile: VoiceProfile) {
        val l = list().toMutableList()
        val i = l.indexOfFirst { it.name == profile.name }
        if (i >= 0) l[i] = profile else l.add(profile)
        persist(l)
    }

    fun delete(name: String) {
        persist(list().filterNot { it.name == name })
        if (active() == name) prefs.edit().remove(KEY_ACTIVE).apply()
    }

    fun active(): String? = prefs.getString(KEY_ACTIVE, null)

    /** 把方案写回当前配置并标记为活动方案 */
    fun applyProfile(name: String): Boolean {
        val p = list().firstOrNull { it.name == name } ?: return false
        prefs.edit()
            .putString(VoicePrefs.KEY_BASE_URL, p.baseUrl)
            .putString(VoicePrefs.KEY_API_KEY, p.apiKey)
            .putString(VoicePrefs.KEY_MODEL, p.model)
            .putString(VoicePrefs.KEY_LANGUAGE, p.language)
            .putString(VoicePrefs.KEY_API_MODE, p.apiMode)
            .putString(KEY_ACTIVE, name)
            .apply()
        return true
    }

    /** 以当前配置内容创建/覆盖方案，并设为活动方案 */
    fun saveCurrentAs(name: String, cfg: VoicePrefs): VoiceProfile {
        val p = VoiceProfile(name, cfg.baseUrl, cfg.apiKey, cfg.model, cfg.language, cfg.apiMode)
        put(p)
        prefs.edit().putString(KEY_ACTIVE, name).apply()
        return p
    }

    companion object {
        const val KEY = "voice_profiles"
        const val KEY_ACTIVE = "active_profile"
    }
}
