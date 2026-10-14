# fcitx5-android 语音输入插件（Voice Input Plugin）

为 [fcitx5-android](https://github.com/fcitx5-android/fcitx5-android) 提供**网络语音识别**输入能力的独立插件 APK。
语音识别接口按 **OpenAI Audio Transcriptions API**（`POST {baseUrl}/audio/transcriptions`，Whisper 规范）设计，
模型名称、Base URL、API Key 均由用户在插件配置页自行填写，可对接 OpenAI、DashScope（Qwen-Omni/whisper 兼容模式）、
自建 whisper.cpp / faster-whisper-server 等任意兼容实现。

## 工作原理（遵循 fcitx5-android 插件与语音集成规范）

fcitx5-android 的已发布版本内置了语音输入集成机制（`app/.../input/bar/KawaiiBarComponent` +
`InputMethodUtil.findVoiceSubtype`，判定条件为 **`subtype.mode == "voice"`**）：

1. 本插件 APK 按规范注册为 fcitx5-android 的插件：
   - 包名 `org.fcitx.fcitx5.android.plugin.voice_input`（插件包名前缀规范）；
   - `res/xml/plugin.xml`：`apiVersion 0.1` 插件描述符（主程序 `DataManager.detectPlugins()` 解析）；
   - `assets/descriptor.json`：规范要求的空数据层级描述符；
   - `SettingsActivity` 携带 `org.fcitx.fcitx5.android.plugin.MANIFEST` intent-filter——
     这是主程序发现插件并打开插件页面的标准钩子，因此
     **主程序「设置 → 插件 → 语音输入」直接进入本插件的配置页**。
2. 插件同时是一个系统输入法（`InputMethodService`），其子类型在 `res/xml/method.xml` 中声明
   `android:imeSubtypeMode="voice"`。
3. 在主程序中开启「键盘设置 → 显示语音输入按钮」并首选本语音输入法后，
   **fcitx5 键盘工具栏上会出现麦克风图标**；点击即切换到本插件输入法。
4. 切换到本输入法后（默认自动开始）录音，16 kHz 单声道 PCM 封装为 WAV，
   调用 OpenAI 兼容接口转写，识别文本经 `InputConnection.commitText()` 直接上屏到当前应用输入框。
5. 识别完成可自动切回 fcitx5 输入法（配置项）。

```
┌───────────────────────────┐        ┌──────────────────────────────┐
│  fcitx5-android 主程序      │  🎤点击 │  本插件 (voice_input)         │
│  键盘工具栏语音图标          │ ──────▶ │  InputMethodService          │
│  (查找 mode="voice" 子类型) │        │   ├─ AudioRecord 录音→WAV     │
│                           │  上屏文本│   ├─ POST {base}/audio/       │
│  InputConnection ◀──────── │ ◀────── │   │  transcriptions (OpenAI)  │
│  (commitText)             │  可切回  │   └─ SettingsActivity 配置页  │
└───────────────────────────┘        └──────────────────────────────┘
```

## API 接口设计（OpenAI 兼容）

- `POST {baseUrl}/audio/transcriptions`
- Header：`Authorization: Bearer {api_key}`（留空则不带）
- `multipart/form-data` 字段：
  - `file`：`voice.wav`，`audio/wav`（16 kHz、16-bit、单声道 PCM WAV）
  - `model`：用户配置（默认 `whisper-1`）
  - `language`：仅在非 `auto` 时发送（OpenAI 语言代码，如 `zh`、`en`）
  - `response_format`：`json`
- 响应解析：取 JSON 的 `text` 字段（兼容 `verbose_json`；部分兼容实现返回纯文本亦兼容）。

## 配置项（插件配置页）

| 配置 | 说明 | 默认 |
|---|---|---|
| Base URL | OpenAI 兼容根地址（含 `/v1`） | `https://api.openai.com/v1` |
| API Key | Bearer Token | 空 |
| 模型名称 | 传给转写接口 | `whisper-1` |
| 识别语言 | `auto` 或 zh/en/ja/ko/yue | auto |
| 切换后立即录音 | 点击 🎤 即开始录音 | 开 |
| 上屏后返回 fcitx5 | 自动切回主程序输入法 | 开 |
| 测试连接 | 录 2 秒真实转写一次 | — |

## 目录结构

```
fcitx5-android-voice/
├── app/
│   ├── build.gradle.kts              # AGP 9.4.1 / Kotlin 2.4.10（与 fcitx5-android master 一致）
│   └── src/main/
│       ├── AndroidManifest.xml       # 输入法服务 + SettingsActivity（含 MANIFEST intent-filter）+ 权限
│       ├── assets/descriptor.json    # 插件规范的（空）数据层级描述符
│       ├── java/org/fcitx/fcitx5/android/plugin/voice_input/
│       │   ├── VoiceInputMethodService.kt  # 输入法主体：录音→转写→上屏→切回
│       │   ├── WavRecorder.kt              # AudioRecord→WAV（16k/mono/16bit）
│       │   ├── TranscriptionClient.kt      # OpenAI 兼容 multipart 转写客户端
│       │   ├── VoicePrefs.kt               # 配置存取（SharedPreferences）
│       │   └── SettingsActivity.kt         # 插件配置页（权限/测试连接/启用输入法）
│       └── res/
│           ├── xml/method.xml        # 输入法元数据：imeSubtypeMode="voice"（🎤图标的关键）
│           ├── xml/plugin.xml        # fcitx5 插件描述符（apiVersion 0.1）
│           ├── xml/preferences.xml   # 配置页定义
│           ├── layout/voice_input_panel.xml
│           └── drawable/…            # 麦克风/停止/设置/键盘图标
├── gradle/、settings.gradle.kts、build.gradle.kts、README.md
```

## 构建（已在本仓库环境实测通过）

- 版本组合沿用 fcitx5-android master 已验证配置：**AGP 9.4.1 + Gradle 9.6.1（AGP 9 内置 Kotlin 支持，
  无需再应用 KGP 插件）+ Kotlin DSL**；`compileSdk 36 / minSdk 26 / targetSdk 34`。
- 国内网络：`settings.gradle.kts` 已配阿里云 Maven 镜像，`gradle-wrapper.properties` 已配腾讯云 Gradle 镜像
  （`dl.google.com`/`services.gradle.org` 直连在此网络不可用）。
- 工作目录路径含中文时需 `gradle.properties` 里的 `android.overridePathCheck=true`（已内置）。

```bash
./gradlew assembleDebug      # 调试版（applicationId 带 .debug 后缀，对接 debug 版主程序）
./gradlew assembleRelease    # 产物 app/build/outputs/apk/release/app-release-unsigned.apk
```

> 当前仓库已生成测试用 keystore（`voice-release.jks`，口令 `voiceinput`，已被 .gitignore 排除）
> 并签名产出 `app/build/outputs/apk/release/app-release.apk`。
> **正式发布请替换为你自己的 keystore。**

安装/推送：`adb install app/build/outputs/apk/debug/app-debug.apk`。

## 使用步骤

1. 安装 **fcitx5-android 主程序**（release 版即可，本插件不要求主程序修改/重编译）。
2. 安装本**插件 APK**；打开插件设置页（桌面图标「语音输入」，或 fcitx5-android「设置 → 插件」中点击本插件）。
3. 配置页中：填 Base URL / API Key / 模型名称 → 授予**麦克风权限** → 点「测试连接」确认可用 →
   「启用本输入法」并在系统输入法列表勾选。
4. fcitx5-android「设置 → 键盘 → 显示语音输入按钮」打开，并在「首选语音输入」选择本插件。
5. 调出 fcitx5 键盘 → 点击工具栏 **🎤 图标** → 开始说话 → 再点一次（或说满 60 秒）停止 →
   识别文本自动输入到当前输入框，并（默认）切回 fcitx5 键盘继续打字。

> 说明：release 与 debug 版主程序互相隔离（包名 `.debug` 后缀与 action 后缀），请用与主程序变体一致的插件构建。

## 已知限制 / 后续方向

- 采用"一次性整段录音→转写"，非实时流式（OpenAI `/audio/transcriptions` 本身即整段接口）；
  后续可接入 OpenAI Realtime / DashScope 双向流式协议实现边说边上屏。
- 长录音单次 Binder 体积在 60 秒上限内（约 1.9 MB），若需更长可分片上传。
- 上游 `feat/native-plugin-ipc` 分支正在开发新的插件 IPC（`IFcitxPluginService` + native addon 桥，
  见 cloud-pinyin 插件）。待其合入主线，本插件可平滑迁移为 native addon 形态，
  将 🎤 直接注入 fcitx5 状态区、免系统输入法切换。当前实现刻意选择了 release 版主程序已具备的
  voice-subtype 集成路径，保证即装即用。
