/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Voice Input Plugin Contributors
 */
package org.fcitx.fcitx5.android.plugin.voice_input

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlin.concurrent.thread

/**
 * 插件配置页：用户在 fcitx5-android「设置 -> 插件 -> 语音输入」中点击本插件
 * 即打开此页（依据插件规范的 org.fcitx.fcitx5.android.plugin.MANIFEST intent-filter），
 * 自行配置 OpenAI 兼容接口的 Base URL / API Key / 模型名 / 语言，
 * 并可将整套配置保存为命名「方案」在多个渠道之间快速切换。
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
        private lateinit var store: ProfileStore
        private lateinit var cfg: VoicePrefs

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            store = ProfileStore(requireContext())
            cfg = VoicePrefs(requireContext())

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

            // ---- 语音方案 ----
            findPreference<ListPreference>("profile_switch")?.setOnPreferenceChangeListener { _, value ->
                val name = value as String
                if (store.applyProfile(name)) {
                    syncConfigUi()
                    refreshProfiles()
                    toast(getString(R.string.profile_applied, name))
                }
                false // summary 由 refreshProfiles 统一维护
            }

            findPreference<Preference>("profile_save")?.setOnPreferenceClickListener {
                promptSaveProfile()
                true
            }

            findPreference<Preference>("profile_manage")?.setOnPreferenceClickListener {
                showManageDialog()
                true
            }

            populateReturnSubtypes()
            refreshProfiles()
        }

        /** 列出 fcitx5 主程序的非英文动态子类型（extraValue=输入法 uniqueName）供固定选择 */
        private fun populateReturnSubtypes() {
            val pref = findPreference<ListPreference>("fcitx_return_subtype") ?: return
            val imm = requireContext()
                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
            val fcitxImi = imm.enabledInputMethodList
                .firstOrNull { it.packageName == BuildConfig.MAIN_APPLICATION_ID }
            val subtypes = fcitxImi?.let {
                imm.getEnabledInputMethodSubtypeList(it, true)
                    .filter { s -> s.extraValue.isNotEmpty() && s.extraValue != "keyboard-us" }
                    .map { s -> s.extraValue }
            } ?: emptyList()
            val entries = mutableListOf(getString(R.string.pref_return_subtype_auto))
            val values = mutableListOf("")
            subtypes.forEach { entries.add(it); values.add(it) }
            pref.entries = entries.toTypedArray()
            pref.entryValues = values.toTypedArray()
        }

        override fun onResume() {
            super.onResume()
            // 手动编辑 Base URL 等之后回到页面时，刷新"（已修改）"标记
            refreshProfiles()
        }

        // ---------- 方案 UI ----------

        private fun refreshProfiles() {
            val switch = findPreference<ListPreference>("profile_switch") ?: return
            val profiles = store.list()
            val names = profiles.map { it.name }
            switch.entries = names.toTypedArray<CharSequence>()
            switch.entryValues = names.toTypedArray<CharSequence>()
            val active = store.active()
            switch.value = active?.takeIf { names.contains(it) }
            switch.summary = when {
                names.isEmpty() -> getString(R.string.profile_none_hint)
                active == null -> getString(R.string.profile_none_hint)
                isActiveModified(profiles.first { it.name == active }) ->
                    active + " " + getString(R.string.profile_modified_suffix)
                else -> active + " " + getString(R.string.profile_current_suffix)
            }
        }

        private fun isActiveModified(p: VoiceProfile): Boolean =
            p.baseUrl != cfg.baseUrl || p.apiKey != cfg.apiKey ||
                    p.model != cfg.model || p.language != cfg.language ||
                    p.apiMode != cfg.apiMode

        /** 方案应用/另存后，把值同步回可见的配置控件 */
        private fun syncConfigUi() {
            findPreference<EditTextPreference>("base_url")?.text = cfg.baseUrl
            findPreference<EditTextPreference>("api_key")?.text = cfg.apiKey
            findPreference<EditTextPreference>("model")?.text = cfg.model
            findPreference<ListPreference>("language")?.value = cfg.language
            findPreference<ListPreference>("api_mode")?.value = cfg.apiMode
        }

        private fun promptSaveProfile() {
            val input = EditText(requireContext()).apply {
                hint = getString(R.string.profile_name_hint)
                isSingleLine = true
            }
            val container = FrameLayout(requireContext()).apply {
                val pad = (20 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad / 2, pad, 0)
                addView(input)
            }
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_name_dialog_title)
                .setView(container)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            // 覆盖默认行为：校验/二次确认后再手动关闭
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    toast(R.string.profile_name_empty)
                    return@setOnClickListener
                }
                val existing = store.list().any { it.name == name }
                dialog.dismiss()
                if (existing) {
                    AlertDialog.Builder(requireContext())
                        .setMessage(getString(R.string.profile_overwrite_confirm, name))
                        .setPositiveButton(android.R.string.ok) { _, _ -> doSave(name) }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                } else {
                    doSave(name)
                }
            }
        }

        private fun doSave(name: String) {
            store.saveCurrentAs(name, cfg)
            refreshProfiles()
            toast(getString(R.string.profile_saved, name))
        }

        private fun showManageDialog() {
            val profiles = store.list()
            if (profiles.isEmpty()) {
                promptSaveProfile()
                return
            }
            val active = store.active()
            val labels = profiles.map { p ->
                if (p.name == active) "${p.name} ${getString(R.string.profile_current_suffix)}" else p.name
            } + listOf(
                getString(R.string.profile_save_new_label),
                getString(R.string.profile_delete_label)
            )
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_manage_dialog_title)
                .setItems(labels.toTypedArray<CharSequence>()) { _, idx ->
                    when {
                        idx < profiles.size -> {
                            val name = profiles[idx].name
                            if (store.applyProfile(name)) {
                                syncConfigUi()
                                refreshProfiles()
                                toast(getString(R.string.profile_applied, name))
                            }
                        }
                        idx == profiles.size -> promptSaveProfile()
                        else -> showDeleteDialog(profiles.map { it.name })
                    }
                }
                .show()
        }

        private fun showDeleteDialog(names: List<String>) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_delete_title)
                .setItems(names.toTypedArray<CharSequence>()) { _, idx ->
                    val name = names[idx]
                    AlertDialog.Builder(requireContext())
                        .setMessage(getString(R.string.profile_delete_confirm, name))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            store.delete(name)
                            refreshProfiles()
                            toast(getString(R.string.profile_deleted, name))
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
                .show()
        }

        // ---------- 测试连接 ----------

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
            if (prefs.engineKind() != EngineKind.Sync) {
                testRealtime(ctx, prefs)
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

        /**
         * 实时模型测试：连 WS → 等音频通道就绪 → 流式送 5 秒麦克风音频 → 等最终转写。
         * 未收到 completed 时用最后一次 delta 兜底（避免"未识别到语音"误报）。
         */
        private fun testRealtime(ctx: android.content.Context, prefs: VoicePrefs) {
            val finals = StringBuilder()
            val lastPartial = java.util.concurrent.atomic.AtomicReference("")
            val audioReady = java.util.concurrent.CountDownLatch(1)
            val done = java.util.concurrent.CountDownLatch(1)
            var error: String? = null
            val cb = object : DictationEngine.Callback {
                override fun onPartial(text: String) {
                    lastPartial.set(text)
                    activity?.runOnUiThread {
                        Toast.makeText(ctx, ctx.getString(R.string.voice_listening) + " " + text,
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFinal(text: String) {
                    finals.append(text)
                }

                override fun onFailure(message: String) {
                    error = message
                    audioReady.countDown()
                    done.countDown()
                }

                override fun onClosed() {
                    done.countDown()
                }
            }
            val client: DictationEngine = when (prefs.engineKind()) {
                EngineKind.CapsWriterRealtime -> CapsWriterDictationClient(prefs, "", cb)
                else -> RealtimeDictationClient(prefs, "", cb)
            }
            client.onAudioReady = { audioReady.countDown() }
            toast(R.string.voice_rt_test_hint)
            // CapsWriter 是分块推理，final 尾片等待更长
            val finalWait = if (prefs.engineKind() == EngineKind.CapsWriterRealtime) 18L else 8L
            thread(name = "voice-rt-test") {
                try {
                    client.start()
                    // 等握手 + session 就绪（最多 8s），再开始 5s 录音窗口
                    audioReady.await(8, java.util.concurrent.TimeUnit.SECONDS)
                    Thread.sleep(5000L)
                    client.stop()
                    done.await(finalWait, java.util.concurrent.TimeUnit.SECONDS)
                    val msg = error ?: (
                            finals.toString().ifBlank { lastPartial.get() }
                                .ifBlank { ctx.getString(R.string.voice_empty) })
                    activity?.runOnUiThread {
                        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    client.cancel()
                    activity?.runOnUiThread {
                        Toast.makeText(ctx, e.message ?: e.javaClass.simpleName, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        private fun toast(resId: Int) {
            Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
        }

        private fun toast(msg: String) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }
}
