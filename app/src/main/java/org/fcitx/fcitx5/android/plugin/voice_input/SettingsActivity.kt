/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Voice Input Plugin Contributors
 */
package org.fcitx.fcitx5.android.plugin.voice_input

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlin.concurrent.thread

/**
 * 插件配置页：用户在 fcitx5-android「设置 -> 插件 -> 语音输入」中点击本插件
 * 即打开此页（依据插件规范的 org.fcitx.fcitx5.android.plugin.MANIFEST intent-filter），
 * 自行配置 OpenAI 兼容接口的 Base URL / API Key / 模型名 / 语言。
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var permissionLauncher: ActivityResultLauncher<String>

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                toast(if (granted) R.string.voice_committed else R.string.voice_error_permission)
            }

            findPreference<Preference>("grant_record_permission")?.setOnPreferenceClickListener {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    toast(R.string.pref_grant_permission_title)
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                true
            }

            findPreference<Preference>("enable_ime")?.setOnPreferenceClickListener {
                startActivity(android.content.Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }

            findPreference<Preference>("test_api")?.setOnPreferenceClickListener {
                testApi()
                true
            }
        }

        /** 录 2 秒并发起一次真实转写，验证 Base URL / API Key / 模型名 */
        private fun testApi() {
            val ctx = requireContext().applicationContext
            val prefs = VoicePrefs(ctx)
            if (!prefs.isConfigured()) {
                toast(R.string.voice_error_not_configured)
                return
            }
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                toast(R.string.voice_error_permission)
                return
            }
            toast(R.string.voice_recognizing)
            thread(name = "voice-test") {
                try {
                    val rec = WavRecorder()
                    if (!rec.start()) error("AudioRecord init failed")
                    Thread.sleep(2000L)
                    val wav = rec.stopAndRelease() ?: error("empty recording")
                    val text = TranscriptionClient().transcribe(wav, prefs)
                    val msg = text.ifBlank { requireContext().getString(R.string.voice_empty) }
                    activity?.runOnUiThread { Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show() }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        Toast.makeText(ctx, e.message ?: e.javaClass.simpleName, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        private fun toast(resId: Int) {
            Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
        }
    }
}
