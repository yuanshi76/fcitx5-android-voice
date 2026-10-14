/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Voice Input Plugin Contributors
 */
package org.fcitx.fcitx5.android.plugin.voice_input

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

/**
 * fcitx5-android 语音输入插件主体。
 *
 * 交互流程（遵循 fcitx5-android 的语音输入集成规范）：
 * 1. 主程序设置开启"显示语音输入按钮"后，fcitx5 键盘工具栏出现麦克风图标；
 * 2. 点击该图标 -> 主程序 InputMethodUtil.switchInputMethod() 切换到本输入法
 *    （依据：本输入法在 res/xml/method.xml 中声明了 imeSubtypeMode="voice"）；
 * 3. 进入语音界面后，**按住**中央麦克风按钮开始录音，**松开**即停止并识别；
 * 4. 录音封装为 16kHz/mono/16bit WAV，调用 OpenAI 兼容的
 *    POST {baseUrl}/audio/transcriptions 转写，识别文本经 commitText() 上屏；
 * 5. 识别完成后停留在语音界面支持连续听写；当用户收起输入窗口时
 *    （onWindowHidden）自动把当前输入法还原为 fcitx5（配置项）。
 */
class VoiceInputMethodService : android.inputmethodservice.InputMethodService() {

    private enum class State { Idle, Recording, Recognizing }

    private lateinit var prefs: VoicePrefs
    private val client = TranscriptionClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var recorder: WavRecorder? = null
    private var state = State.Idle
    private var recordSeconds = 0

    private var statusText: TextView? = null
    private var micButton: ImageButton? = null

    private val tick = object : Runnable {
        override fun run() {
            if (state != State.Recording) return
            recordSeconds++
            statusText?.text = getString(R.string.voice_recording, recordSeconds.toString())
            if (recordSeconds >= VoicePrefs.MAX_RECORD_SECONDS) {
                Toast.makeText(this@VoiceInputMethodService, R.string.voice_recognizing, Toast.LENGTH_SHORT).show()
                finishRecordingAndRecognize()
                return
            }
            mainHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = VoicePrefs(this)
    }

    /** 语音输入法不依赖编辑器可写状态，始终展示输入视图 */
    override fun onEvaluateInputViewShown(): Boolean = true

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.voice_input_panel, null)
        statusText = view.findViewById(R.id.status_text)
        micButton = view.findViewById(R.id.btn_mic)
        // 按住说话（push-to-talk）：按下开始录音，松开停止并识别
        micButton?.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (state == State.Idle) startRecording()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (state == State.Recording) finishRecordingAndRecognize()
                    true
                }
                else -> false
            }
        }
        view.findViewById<View>(R.id.btn_settings)?.setOnClickListener { openSettings() }
        view.findViewById<View>(R.id.btn_keyboard)?.setOnClickListener { switchToFcitx5() }
        return view
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        render()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        // 输入法被收起时放弃未提交的录音，避免资源泄漏
        if (state == State.Recording) cancelRecording()
    }

    /**
     * 键盘窗口隐藏 = 用户收起了输入窗口（或切走）。
     * 此时才把当前输入法还原为 fcitx5：
     * 识别完成期间保持停留在语音界面，支持连续听写；
     * 下次唤起键盘时默认回到 fcitx5 键盘。
     */
    override fun onWindowHidden() {
        super.onWindowHidden()
        if (prefs.autoReturn) switchToFcitx5(showPickerOnFail = false)
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    private fun startRecording() {
        if (!prefs.isConfigured()) {
            showError(getString(R.string.voice_error_not_configured))
            openSettings()
            return
        }
        if (!hasRecordPermission()) {
            showError(getString(R.string.voice_error_permission))
            openSettings()
            return
        }
        val r = WavRecorder()
        if (!r.start()) {
            showError(getString(R.string.voice_error_generic, "AudioRecord"))
            return
        }
        recorder = r
        state = State.Recording
        recordSeconds = 0
        render()
        mainHandler.postDelayed(tick, 1000L)
    }

    private fun cancelRecording() {
        recorder?.cancel()
        recorder = null
        state = State.Idle
        mainHandler.removeCallbacks(tick)
        render()
    }

    private fun finishRecordingAndRecognize() {
        val r = recorder ?: return
        recorder = null
        mainHandler.removeCallbacks(tick)
        val wav = r.stopAndRelease()
        if (wav == null || wav.size < 44 + VoicePrefs.SAMPLE_RATE / 4 /* < ~125ms */) {
            state = State.Idle
            showError(getString(R.string.voice_empty))
            render()
            return
        }
        state = State.Recognizing
        render()
        thread(name = "voice-transcribe") {
            try {
                val text = client.transcribe(wav, prefs)
                mainHandler.post { onResult(text) }
            } catch (e: Exception) {
                Log.w(TAG, "transcription failed", e)
                val msg = when {
                    e is TranscriptionClient.ApiException && e.code == 401 -> "401 Unauthorized（检查 API Key）"
                    e is java.net.UnknownHostException -> "无法解析 ${prefs.baseUrl}"
                    e is java.net.SocketTimeoutException -> "请求超时"
                    else -> e.message ?: e.javaClass.simpleName
                }
                mainHandler.post {
                    state = State.Idle
                    render()
                    showError(getString(R.string.voice_error_generic, msg))
                }
            }
        }
    }

    private fun onResult(text: String) {
        state = State.Idle
        render()
        if (text.isBlank()) {
            showError(getString(R.string.voice_empty))
            return
        }
        currentInputConnection?.commitText(text, 1)
        statusText?.text = getString(R.string.voice_committed)
        // 识别完成后继续停留在语音界面，便于连续听写；
        // 回到 fcitx5 键盘的时机在 onWindowHidden（用户收起键盘时）
    }

    /**
     * 切回 fcitx5-android 主输入法。
     * 组件名 org.fcitx.fcitx5.android/.input.FcitxInputMethodService 与
     * 主程序 InputMethodUtil 的语音切换方式互为对称操作。
     */
    private fun switchToFcitx5(showPickerOnFail: Boolean = true) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val fcitxImi = imm.enabledInputMethodList
            .firstOrNull { it.packageName == BuildConfig.MAIN_APPLICATION_ID }
        val fcitxId = fcitxImi?.id
        if (fcitxId == null) {
            // 未安装 / 未启用主程序：仅在用户主动点击时退回系统输入法选择器
            if (showPickerOnFail) runCatching { imm.showInputMethodPicker() }
            return
        }
        // Android 14+ 上 fcitx5 会为每个激活的输入法注册动态 subtype（extraValue = 输入法 uniqueName），
        // 系统按"上次使用的 subtype"恢复。若该记忆被污染（如早期版本误传静态英文 subtype），
        // 这里显式传真实输入法的 subtype 以恢复；无法唯一确定时传 null 走系统记忆。
        val subtype = pickFcitxCurrentSubtype(imm, fcitxImi)
        @Suppress("DEPRECATION")
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            switchInputMethod(fcitxId, subtype)
        } else {
            window.window?.attributes?.token?.let {
                imm.setInputMethodAndSubtype(it, fcitxId, subtype)
            }
        }
    }

    /**
     * 从 fcitx5 已启用的 subtype 中挑选"用户真实输入法"对应的 subtype：
     * 动态 subtype 的 extraValue 即 fcitx 输入法 uniqueName（如 "pinyin"）；
     * 静态英文 subtype 的 extraValue 为空，"keyboard-us" 也排除。
     * 恰好一个候选时返回它（顺带修复被污染的系统记忆），否则返回 null（尊重系统记忆）。
     */
    private fun pickFcitxCurrentSubtype(
        imm: InputMethodManager,
        fcitxImi: android.view.inputmethod.InputMethodInfo
    ): android.view.inputmethod.InputMethodSubtype? {
        val candidates = imm.getEnabledInputMethodSubtypeList(fcitxImi, true)
            .filter { it.extraValue.isNotEmpty() && it.extraValue != "keyboard-us" }
        return candidates.singleOrNull()
    }

    private fun openSettings() {
        // singleTask + CLEAR_TOP：保证配置页最多只有一个实例，返回一次即可退出
        startActivity(
            Intent(this, SettingsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    private fun showError(msg: String) {
        statusText?.text = msg
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun render() {
        val s = statusText ?: return
        when (state) {
            State.Idle -> s.text = getString(R.string.voice_idle)
            State.Recording -> s.text = getString(R.string.voice_listening)
            State.Recognizing -> s.text = getString(R.string.voice_recognizing)
        }
        // 录音中麦克风变红提示
        micButton?.setColorFilter(
            if (state == State.Recording) 0xFFFF5252.toInt() else 0xFFB2DFDB.toInt()
        )
    }

    companion object {
        private const val TAG = "VoiceIMS"
    }
}
