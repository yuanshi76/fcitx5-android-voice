/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Voice Input Plugin Contributors
 */
package org.fcitx.fcitx5.android.plugin.voice_input

/** 听写执行器类别（由 Base URL / 模型名 / 接口模式共同决定） */
enum class EngineKind { Sync, BailianRealtime, CapsWriterRealtime }

/**
 * 实时听写引擎统一接口：按住说话（start）→ 流式上行 → 松开（stop，等待最终结果）→ cancel 立即放弃。
 * partial 为"截止当前的显示文本"（百炼为增量拼接文本，CapsWriter 为累积全文——composing 整体替换语义下均可直接使用）。
 */
interface DictationEngine {

    interface Callback {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onFailure(message: String)
        fun onClosed()
    }

    /** 音频通道就绪回调（WS 握手 + 会话建立完成），测试流程用于对齐计时 */
    var onAudioReady: (() -> Unit)?

    fun start()

    /** 停止送音；等待最后一句最终结果后关闭 */
    fun stop()

    /** 立即放弃并关闭 */
    fun cancel()
}
