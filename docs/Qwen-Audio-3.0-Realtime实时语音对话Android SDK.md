> ## Documentation Index
> Fetch the complete documentation index at: https://docs.bailian.console.aliyun.com/llms.txt
> Use this file to discover all available pages before exploring further.

# Qwen-Audio-3.0-Realtime实时语音对话Android SDK

> 使用Qwen-Audio-3.0-Realtime实时语音对话Android SDK，实现实时音频输入以及语音或文本输出。

<meta property="og:title" content="Qwen-Audio-3.0-Realtime实时语音对话Android SDK" />

<strong>用户指南：</strong>关于模型介绍和选型建议请参见[实时语音对话](/zh/model-studio/qwen-audio-realtime-user-guides)。

## 快速开始 <span id="quick-start" />

1. [获取与配置 API Key](/zh/model-studio/get-api-key)
2. <strong>下载 SDK 并运行示例代码：</strong>
   - [下载最新 SDK 整合包](/zh/model-studio/sdk-selection-and-download)。
   - 解压 ZIP 包。在 `app/libs` 目录中获取 AAR 格式 SDK，并添加到项目依赖。需要 Android C++ 接入时，使用 ZIP 包内的 `android_libs` 与 `android_include` 获取动态库和头文件。
   - 用 Android Studio 打开工程。示例代码位于`DashQwenAudioChatActivity.java`，替换 API Key 后体验功能。

### 调用步骤 <span id="call-procedure" />

1. 初始化 SDK
2. 按业务需求设置参数：通过[initialize](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#initialize)接口的`parameters`参数设置[连接与控制参数](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#connection-and-control-parameters)；通过[setParams](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#set-params)接口设置[语音对话效果参数](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#conversation-parameters)。
3. 调用[startDialog](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#start-dialog)启动对话流程。
4. 在[onNuiAudioStateChanged](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#on-nui-audio-state-changed)回调中，根据音频状态开启录音设备。
5. 在[onNuiNeedAudioData](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#on-nui-need-audio-data)回调中持续提供录音数据，或者通过[updateAudio](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#update-audio)持续推送录音数据。
6. 在[onNuiAssistEventCallback](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#on-nui-assist-event-callback)回调中持续获得 AI 返回的语音数据。
7. 在[onNuiEventCallback](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#on-nui-event-callback)回调中监听事件并获取事件信息。
8. 调用[stopDialog](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#stop-dialog)停止对话，并通过监听EVENT\_TRANSCRIBER\_COMPLETE事件确认对话已结束。
9. 当对话功能不再使用时，调用[release](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#release)接口释放 SDK 资源。

## 请求参数 <span id="request-parameters" />

### 连接与控制参数 <span id="connection-and-control-parameters" />

通过在[initialize](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#initialize)接口的`parameters`参数中传入一个 JSON 字符串来配置。

<strong>参数示例：</strong>以下为 JSON 字符串示例，参数未完整列出。请按实际需求在编码时补充：

```
{
    "url": "wss://dashscope.aliyuncs.com/api-ws/v1/inference",
    "apikey": "st-****",
    "device_id": "my_device_id",
    "service_mode": "1"
}
```

<strong>参数说明</strong>

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "40%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>是否必须</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `url`
      </td>

      <td>
        `String`
      </td>

      <td>
        是
      </td>

      <td>
        服务地址：<ul><li>`wss://dashscope.aliyuncs.com/api-ws/v1/realtime?model=<model_name>`</li><li>华北2（北京）：`wss://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/api-ws/v1/realtime?model=<model_name>`</li><li>新加坡：`wss://{WorkspaceId}.ap-southeast-1.maas.aliyuncs.com/api-ws/v1/realtime?model=<model_name>`</li></ul>调用时，请将  `{WorkspaceId}`  替换为真实的[Workspace ID](/zh/model-studio/regions#h2_migrate_domain)。
      </td>
    </tr>

    <tr>
      <td>
        `apikey`
      </td>

      <td>
        `String`
      </td>

      <td>
        是
      </td>

      <td>
        API Key。
      </td>
    </tr>

    <tr>
      <td>
        `service_mode`
      </td>

      <td>
        `String`
      </td>

      <td>
        是
      </td>

      <td>
        运行模式。实时语音对话固定为 `"1"`。
      </td>
    </tr>

    <tr>
      <td>
        `device_id`
      </td>

      <td>
        `String`
      </td>

      <td>
        是
      </td>

      <td>
        用于标识终端用户的唯一字符串，可设为应用内用户 ID或客户端生成的设备唯一标识符。此 ID主要用于日志追踪和问题排查。
      </td>
    </tr>

    <tr>
      <td>
        audio\_update\_manually
      </td>

      <td>
        `String`
      </td>

      <td>
        否
      </td>

      <td>
        是否启用主动推送音频数据模式，默认值："false"。 <br />当设置为"true"启用主动推送音频数据模式时，且SDK版本支持端侧音频能力（如AEC、VAD），则默认开启端侧音频能力。
      </td>
    </tr>

    <tr>
      <td>
        workspace
      </td>

      <td>
        `String`
      </td>

      <td>
        否
      </td>

      <td>
        当参数audio\_update\_manually设置为"true"时，且启用端侧音频能力（如AEC、VAD）时，必须设置workspace，即端侧资源文件存储的路径。
      </td>
    </tr>

    <tr>
      <td>
        `debug_path`
      </td>

      <td>
        `String`
      </td>

      <td>
        否
      </td>

      <td>
        日志文件的存储路径。 此参数仅在调用[initialize](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#initialize)接口时将`save_log`设为true时生效。此时必须设置日志文件路径，否则将报错。 本地最多保留两个日志文件。
      </td>
    </tr>

    <tr>
      <td>
        `save_wav`
      </td>

      <td>
        `String`
      </td>

      <td>
        否
      </td>

      <td>
        是否保存调试用的音频文件。音频文件保存于`debug_path`下。<br />默认值："false"。<br />取值范围：<ul><li>"true"：是</li><li>"false"：否</li></ul>此参数仅在调用[initialize](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#initialize)接口时将`save_log`设为true时生效。 同时，`debug_path`也必须被设置。
      </td>
    </tr>

    <tr>
      <td>
        `max_log_file_size`
      </td>

      <td>
        `int`
      </td>

      <td>
        否
      </td>

      <td>
        设定日志文件的最大字节数。<br />此参数仅在调用[initialize](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#initialize)接口时将`save_log`设为true时生效。<br />默认值：104857600（100 \* 1024 \* 1024 字节, 即 100MiB）。
      </td>
    </tr>

    <tr>
      <td>
        `log_track_level`
      </td>

      <td>
        `int`
      </td>

      <td>
        否
      </td>

      <td>
        控制通过日志回调（`onNuiLogTrackCallback`）对外发送的日志内容的过滤级别。<br />默认值：2。<br />取值范围：<ul><li>0：LOG\_LEVEL\_VERBOSE</li><li>1：LOG\_LEVEL\_DEBUG</li><li>2：LOG\_LEVEL\_INFO</li><li>3：LOG\_LEVEL\_WARNING</li><li>4：LOG\_LEVEL\_ERROR</li><li>5：LOG\_LEVEL\_NONE（表示关闭此功能）</li></ul>注意：`log_track_level`与`level`（通过[initialize](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#initialize)接口设置）共同决定最终回调的日志。一条日志的级别数值必须同时大于或等于`log_track_level`和`level`的值，才会被回调。例如，`log_track_level`设为2 (INFO)，`level`设为3 (WARNING)，则只有WARNING及以上级别（数值>=3）的日志才会被回调。
      </td>
    </tr>

    <tr>
      <td>
        aec\_params
      </td>

      <td>
        `object`
      </td>

      <td>
        否
      </td>

      <td>
        端侧AEC能力高级参数配置对象。当参数audio\_update\_manually设置为"true"时才启用此配置对象。
      </td>
    </tr>

    <tr>
      <td>
        aec\_params.enable\_aec
      </td>

      <td>
        `boolean`
      </td>

      <td>
        否
      </td>

      <td>
        是否开启端侧AEC回声消除能力。<br />当参数audio\_update\_manually设置为"true"时，且SDK版本支持端侧AEC音频能力，则默认开启。
      </td>
    </tr>

    <tr>
      <td>
        aec\_params.save\_audio
      </td>

      <td>
        `boolean`
      </td>

      <td>
        否
      </td>

      <td>
        是否开启端侧AEC回声消除模块音频存储功能。 当`save_wav`为"true"，且设置了`debug_path`，则默认开启，将AEC运行音频数据存储到`debug_path`下。
      </td>
    </tr>

    <tr>
      <td>
        aec\_params.enable\_aec\_data\_callback
      </td>

      <td>
        `boolean`
      </td>

      <td>
        否
      </td>

      <td>
        是否将AEC后的数据送给用户，默认false；开启后在onNuiAssistEventCallback的EVENT\_AEC\_DATA接收。
      </td>
    </tr>

    <tr>
      <td>
        vad\_params
      </td>

      <td>
        `object`
      </td>

      <td>
        否
      </td>

      <td>
        端侧VAD能力高级参数配置对象。<br />当参数audio\_update\_manually设置为"true"时才启用此配置对象。
      </td>
    </tr>

    <tr>
      <td>
        vad\_params.enable\_vad
      </td>

      <td>
        `boolean`
      </td>

      <td>
        否
      </td>

      <td>
        是否开启端侧VAD人声检测能力。 <br />当参数audio\_update\_manually设置为"true"时，且SDK版本支持端侧VAD音频能力，则默认开启。
      </td>
    </tr>

    <tr>
      <td>
        vad\_params.save\_audio
      </td>

      <td>
        `boolean`
      </td>

      <td>
        否
      </td>

      <td>
        是否开启端侧VAD人声检测模块音频存储功能。 <br />当`save_wav`为"true"，且设置了`debug_path`，则默认开启，将VAD运行音频数据存储到`debug_path`下。
      </td>
    </tr>
  </tbody>
</table>

### 语音对话效果参数 <span id="conversation-parameters" />

通过在[setParams](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#set-params)接口的`params`参数中传入一个 JSON 字符串来配置。

<strong>参数示例：</strong>以下为 JSON 字符串示例，参数未完整列出。请按实际需求在编码时补充：

```
{
    "service_type": 4,
    "nls_config": {
        "model": "qwen-audio-3.1-realtime-plus",
        "sr_format": "pcm"
    }
}
```

<strong>参数说明</strong>

<table style={{ display: "table", tableLayout: "fixed", width: "100%", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "40%" }} />
  </colgroup>

  <thead>
    <tr>
      <th style={{ verticalAlign: "top" }}>
        <strong>一级参数</strong>
      </th>

      <th style={{ verticalAlign: "top" }}>
        <strong>类型</strong>
      </th>

      <th style={{ verticalAlign: "top" }}>
        <strong>是否必须</strong>
      </th>

      <th style={{ verticalAlign: "top" }}>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}>
        `service_type`
      </td>

      <td style={{ verticalAlign: "top" }}>
        `int`
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        语音服务类型。实时语音对话固定为 `4`。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        `nls_config`
      </td>

      <td style={{ verticalAlign: "top" }}>
        `object`
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        语音对话核心配置对象，包含模型选择、对话效果控制等关键参数。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        `nls_config.model`
      </td>

      <td style={{ verticalAlign: "top" }}>
        `string`
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        指定模型名。支持qwen-audio-3.1-realtime-plus、qwen-audio-3.0-realtime-plus和qwen-audio-3.0-realtime-flash系列模型。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        `nls_config.sr_format`
      </td>

      <td style={{ verticalAlign: "top" }}>
        `string`
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        输入音频格式。当前仅支持  `pcm`（16kHz 16bit 单声道），为默认值。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        `nls_config.modalities`
      </td>

      <td style={{ verticalAlign: "top" }}>
        `string`
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        array 格式的字符串，模型输出模态设置，可选值：<ul><li>`["text"]`：仅输出文本。</li><li>`["audio", "text"]`（默认值）：同时输出文本和音频。</li></ul>
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        nls\_config.voice
      </td>

      <td style={{ verticalAlign: "top" }}>
        `string`
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        TTS 音色名称，3.1 Plus 的默认值为 `longanqian_v3.1`，3.0 Plus/Flash 的默认值为 `longanqian`。支持两种类型，仅可在第一次  `session.update`  中设置，后续传入将被忽略。<ul><li><strong>系统音色</strong>：可选值：`longanqian`、`longanlingxin`、`longanlingxi`、`longanxiaoxin`、`longanlufeng`。</li><li><strong>声音复刻音色</strong>：通过声音复刻 API 创建，将返回的  `voice_id`  填入此参数。详见[音色配置](/zh/model-studio/qwen-audio-realtime-user-guides#fc60h311)。</li></ul>

        3.1 Plus 还支持 `longanqian_v3.1`、`longanhuan_v3.1`、`longanlingxin_v3.1`、`longanfengyue_v3.1`、`xunanchuan_v3.1`、`beth_v3.1`、`betty_v3.1`、`cally_v3.1`。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        nls\_config.enable\_speech\_emotion
      </td>

      <td style={{ verticalAlign: "top" }}>
        `boolean`
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        是否开启情绪增强功能。开启后，回复音色的情绪变化更明显。默认值：`true`。可选值：`true`、`false`。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        nls\_config.instructions
      </td>

      <td style={{ verticalAlign: "top" }}>
        `string`
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        系统指令，用于设定模型的角色身份、回答风格和行为偏好。对整个会话生效。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        nls\_config.max\_history\_turns
      </td>

      <td style={{ verticalAlign: "top" }}>
        `int`
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        允许单次请求的最大历史 QA 轮数。取值范围为 1-50，默认值为 20。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        nls\_config.tools
      </td>

      <td style={{ verticalAlign: "top" }}>
        `string`
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        array 格式的字符串。Function Calling 工具定义列表。配置后模型可根据用户输入自主决定是否调用工具。<br />内部参数说明：<br /><strong>type</strong>`string`<strong>（必选）</strong><br />固定为  `function`。<br /><strong>function.name</strong>`string`<strong>（必选）</strong><br />工具函数名称。<br /><strong>function.description</strong>`string`（可选）<br />对工具函数功能的描述，模型据此判断是否调用该工具。<br /><strong>function.parameters</strong>`object`（可选）<br />对工具函数入参的描述，模型据此提取所需入参。若函数无需入参，可不指定。<br />示例：

        ```json
        [
          {
            "type": "function",
            "function": {
              "name": "get_weather",
              "description": "查询指定城市的天气信息。",
              "parameters": {
                "type": "object",
                "properties": {
                  "city": {
                    "type": "string",
                    "description": "城市"
                  }
                },
                "required": [
                  "city"
                ]
              }
            }
          }
        ]
        ```
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        nls\_config.turn\_detection
      </td>

      <td style={{ verticalAlign: "top" }}>
        `string`
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        JSON 对象形式的字符串。轮次检测配置。不设置时则切换为 push-to-talk 模式（手动提交音频并触发推理）。否则启用双工对话模式。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        nls\_config.turn\_detection.type
      </td>

      <td style={{ verticalAlign: "top" }}>
        `string`
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        VAD 类型，可选值：<ul><li>`server_vad`（默认值）：基于声学特征检测语音起止，自动触发推理。</li><li>`smart_turn`：融合声学感知与语义理解的智能轮次检测，通过声学与语义双重判断轮次边界。无语义的声音（如”嗯”、”啊”）不会触发对话轮或打断模型播报。</li></ul>
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        nls\_config.turn\_detection.threshold
      </td>

      <td style={{ verticalAlign: "top" }}>
        float
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        VAD灵敏度，<strong>仅在 server\_vad 模式下生效（smart\_turn 模式下无效）</strong>。值越低，VAD越灵敏，越容易将微弱声音（包括背景噪音）识别为语音；值越高，越不灵敏，需要更清晰、音量更大的语音才能触发。<br />取值范围为\[-1.0, 1.0]，默认值为 0.5。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        nls\_config.turn\_detection.silence\_duration\_ms
      </td>

      <td style={{ verticalAlign: "top" }}>
        `int`
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        语音结束后需保持静音的最短时间（毫秒），<strong>仅在 server\_vad 模式下生效（smart\_turn 模式下无效）</strong>。超时即触发模型响应。值越低，响应越快，但可能在短暂停顿时误触发。<br />取值范围为\[200, 6000]，默认值为 800。对话场景推荐 400-800。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        nls\_config.turn\_detection.voiceprint\_audio\_urls
      </td>

      <td style={{ verticalAlign: "top" }}>
        `string`
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        array形式的字符串。<strong>仅在 smart\_turn 模式下生效。</strong>目标用户预录音频的公网可访问 URL 列表，用于说话人增强。传入后，模型将在双工对话中精准锁定目标说话人，有效忽略旁人声音与背景噪声。最多支持 5 个 URL。音频格式要求：16kHz PCM 或 WAV。
      </td>
    </tr>
  </tbody>
</table>

## 关键接口 <span id="key-interfaces" />

### NativeNui <span id="native-nui" />

#### initialize <span id="initialize" />

初始化语音对话SDK 实例。SDK 为单例模式，在调用[release](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#release)前禁止重复初始化。

此接口会引起阻塞，应在非UI 线程调用。

<strong>方法签名</strong>

```
public synchronized int initialize(final INativeNuiCallback callback,
                                   String parameters,
                                   final Constants.LogLevel level,
                                   final boolean save_log)
```

<strong>参数说明</strong>

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `callback`
      </td>

      <td>
        `INativeNuiCallback`
      </td>

      <td>
        事件和数据回调接口的实现。
      </td>
    </tr>

    <tr>
      <td>
        `parameters`
      </td>

      <td>
        `String`
      </td>

      <td>
        JSON 字符串，包含鉴权、连接和调试参数。参见[连接与控制参数](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#connection-and-control-parameters)。
      </td>
    </tr>

    <tr>
      <td>
        `level`
      </td>

      <td>
        `Constants.LogLevel`
      </td>

      <td>
        控制SDK自身日志的打印级别。
      </td>
    </tr>

    <tr>
      <td>
        `save_log`
      </td>

      <td>
        `boolean`
      </td>

      <td>
        是否保存本地日志。若为`true`，须在[连接与控制参数](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#connection-and-control-parameters)中通过`debug_path`指定路径，并可通过`max_log_file_size`设置文件大小。
      </td>
    </tr>
  </tbody>
</table>

#### setParams <span id="set-params" />

以 JSON 格式设置[语音对话效果参数](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#conversation-parameters)。在[startDialog](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#start-dialog)之前调用。

<strong>方法签名</strong>

```
public synchronized int setParams(String params)
```

<strong>参数说明</strong>

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `params`
      </td>

      <td>
        `String`
      </td>

      <td>
        [语音对话效果参数](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#conversation-parameters)。
      </td>
    </tr>
  </tbody>
</table>

#### startDialog <span id="start-dialog" />

开始对话。

<strong>方法签名</strong>

```
public synchronized int startDialog(VadMode vad_mode, String dialog_params)
```

<strong>参数说明</strong>

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `vad_mode`
      </td>

      <td>
        `VadMode`
      </td>

      <td>
        VAD模式。固定为`VadMode.TYPE_P2T`。
      </td>
    </tr>

    <tr>
      <td>
        `dialog_params`
      </td>

      <td>
        `String`
      </td>

      <td>
        如果[连接与控制参数](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#connection-and-control-parameters)的`apikey`参数使用的是[临时API Key](/zh/model-studio/generate-temporary-api-key)，当其过期时，可在此处进行更新。<br />内容为JSON 格式：

        ```json
        {
          "apikey": "st-****"
        }
        ```
      </td>
    </tr>
  </tbody>
</table>

#### stopDialog <span id="stop-dialog" />

结束对话，调用该接口后，服务端将返回最终对话结果并结束任务。

<strong>方法签名</strong>

```
public synchronized int stopDialog();
```

#### cancelDialog <span id="cancel-dialog" />

立即结束对话，调用该接口后，不等待服务端返回最终对话结果就立即结束任务。

<strong>方法签名</strong>

```
public synchronized int cancelDialog();
```

#### updateAction <span id="update-action" />

在交互过程中下发对话动作指令，用于更新对话上下文等运行时行为。

<strong>方法签名</strong>

```
public synchronized int updateAction(String params);
```

<strong>参数说明</strong>

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `params`
      </td>

      <td>
        `String`
      </td>

      <td>
        JSON 形式的字符串，用于更新对话上下文等运行时行为。
      </td>
    </tr>

    <tr>
      <td>
        `params.type`
      </td>

      <td>
        `String`
      </td>

      <td>
        固定"action"。
      </td>
    </tr>

    <tr>
      <td>
        `params.command`
      </td>

      <td>
        `String`
      </td>

      <td>
        具体的运行指令，当前支持 `function_call`、`play_start`、`play_over`。<ul><li>`function_call`：更新函数调用请求的指令。</li><li>`play_start`：当使用端侧AEC时，通过此指令通知SDK内部AEC播放器开始播放音频。</li><li>`play_over`：当使用端侧AEC时，通过此指令通知SDK内部AEC播放器已经播放结束。</li></ul>
      </td>
    </tr>

    <tr>
      <td>
        `params.context`
      </td>

      <td>
        `String`
      </td>

      <td>
        当`command`为`"function_call"`时，更新函数调用请求。
      </td>
    </tr>

    <tr>
      <td>
        `params.context.type`
      </td>

      <td>
        `String`
      </td>

      <td>
        事件类型，当`command`为`"function_call"`时必须设置。<ul><li>`conversation.item.create`：手动向对话上下文插入一条对话项。可用于注入历史上下文、补充文本信息，或写回 Function Calling 的工具执行结果。</li><li>`response.create`：在`conversation.item.create`指令发送后，通过此指令触发二轮推理。</li></ul>
      </td>
    </tr>

    <tr>
      <td>
        `params.context.item`
      </td>

      <td>
        `object`
      </td>

      <td>
        `params.context.type`为conversation.item.create时必须设置，表示要创建的对话项。详见如下。
      </td>
    </tr>

    <tr>
      <td>
        `params.context.response`
      </td>

      <td>
        `object`
      </td>

      <td>
        `params.context.type`为response.create时可设置，表示用于覆盖本轮推理的会话默认配置。不传时使用当前会话配置。详见如下。
      </td>
    </tr>
  </tbody>
</table>

`context.item`参数：

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        id
      </td>

      <td>
        `String`
      </td>

      <td>
        可选。对话项的唯一标识符。不传时由服务端自动生成。若指定的 ID 已存在于对话中，会返回错误。
      </td>
    </tr>

    <tr>
      <td>
        type
      </td>

      <td>
        `String`
      </td>

      <td>
        必选。对话项类型，可选值：<ul><li>`message`：普通对话消息。</li><li>`function_call`：函数调用请求。通常由服务端生成，客户端也可用于补充历史上下文。</li><li>`function_call_output`：工具执行结果。客户端收到  `function_call`  后执行工具，并用该类型写回结果。</li></ul>
      </td>
    </tr>

    <tr>
      <td>
        role
      </td>

      <td>
        `String`
      </td>

      <td>
        `message`  类型必选。消息角色，可选值：`system`、`user`、`assistant`。
      </td>
    </tr>

    <tr>
      <td>
        content
      </td>

      <td>
        array
      </td>

      <td>
        `message` 类型必选。消息内容列表。每个元素包含 `type` 和对应的数据字段。<br /><strong>各 role 支持的 content 类型</strong>：<br /><strong>system</strong><br />`input_text`：系统消息，必填字段 `text`。<br /><strong>user</strong><ul><li>`input_text`：用户文本输入，必填字段 `text`。</li><li>`input_audio`：用户音频输入，必填字段 `audio`（Base64 编码）。</li></ul><strong>assistant</strong><br />`output_text`：助手文本输出，必填字段 `text`。
      </td>
    </tr>

    <tr>
      <td>
        call\_id
      </td>

      <td>
        `String`
      </td>

      <td>
        （`function_call`  /  `function_call_output`  类型必选）<br />函数调用的唯一标识符，用于关联请求和结果。
      </td>
    </tr>

    <tr>
      <td>
        name
      </td>

      <td>
        `String`
      </td>

      <td>
        （`function_call`  类型必选）<br />要调用的函数名称。
      </td>
    </tr>

    <tr>
      <td>
        arguments
      </td>

      <td>
        `String`
      </td>

      <td>
        `function_call`  类型必选）<br />函数调用参数，JSON 字符串格式。
      </td>
    </tr>

    <tr>
      <td>
        output
      </td>

      <td>
        `String`
      </td>

      <td>
        （`function_call_output`  类型必选）<br />工具执行结果，JSON 字符串格式。
      </td>
    </tr>
  </tbody>
</table>

`context.response`参数：

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        modalities
      </td>

      <td>
        `array`
      </td>

      <td>
        array 格式的字符串，模型输出模态设置，可选值：<ul><li>`["text"]`：仅输出文本。</li><li>`["audio", "text"]`（默认值）：同时输出文本和音频。</li></ul>
      </td>
    </tr>

    <tr>
      <td>
        voice
      </td>

      <td>
        string
      </td>

      <td>
        覆盖本轮的 TTS 音色。
      </td>
    </tr>
  </tbody>
</table>

示例：

```
{
  "type": "action",
  "command": "function_call",
  "context": {
    "item": {
      "call_id": "call_xxxx",
      "output": "{\"city\":\"杭州\",\"condition\":\"晴\",\"temperature\":18}",
      "type": "function_call_output"
    },
    "type": "conversation.item.create"
  }
}

{
  "type": "action",
  "command": "function_call",
  "context": {
    "response": {
      "modalities": [
        "text",
        "audio"
      ]
    },
    "type": "response.create"
  }
}
```

#### updateAudio <span id="update-audio" />

参数audio\_update\_manually设置为"true"时，录音数据不再是通过onNuiNeedAudioData填入，而是用此接口主动推送。

<strong>方法签名</strong>

```
public synchronized int updateAudio(byte[ ] data, int len,
                                    boolean first_pack);
```

<strong>参数说明</strong>

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `data`
      </td>

      <td>
        `byte[]`
      </td>

      <td>
        推送的音频数据。
      </td>
    </tr>

    <tr>
      <td>
        `len`
      </td>

      <td>
        `int`
      </td>

      <td>
        推送的音频数据的字节数。
      </td>
    </tr>

    <tr>
      <td>
        `first_pack`
      </td>

      <td>
        `boolean`
      </td>

      <td>
        请忽略，无需关注此参数。
      </td>
    </tr>
  </tbody>
</table>

#### updateRefAudio <span id="update-ref-audio" />

参数audio\_update\_manually设置为"true"时，且启用了端侧AEC回声消除能力，则需要用此接口推送播放器播放的音频数据作为参考信号。

<strong>方法签名</strong>

```
public synchronized int updateRefAudio(byte[ ] data, int len,

                                       boolean first_pack);
```

<strong>参数说明</strong>

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `data`
      </td>

      <td>
        `byte[]`
      </td>

      <td>
        推送的音频数据。
      </td>
    </tr>

    <tr>
      <td>
        `len`
      </td>

      <td>
        `int`
      </td>

      <td>
        推送的音频数据的字节数。
      </td>
    </tr>

    <tr>
      <td>
        `first_pack`
      </td>

      <td>
        `boolean`
      </td>

      <td>
        请忽略，无需关注此参数。
      </td>
    </tr>
  </tbody>
</table>

#### release <span id="release" />

释放SDK所有内部资源。此方法调用后，SDK 实例将变为不可用状态，如需再次使用，必须重新调用[initialize](/zh/model-studio/android-sdk-for-qwen-audio-realtime-service#initialize)进行初始化。

<strong>方法签名</strong>

```
public synchronized int release();
```

#### GetVersion <span id="get-version" />

获得当前SDK版本信息。

<strong>方法签名</strong>

```
public synchronized String GetVersion();
```

<strong>返回值说明</strong>

当前SDK版本信息。

### INativeNuiCallback：监听回调 <span id="native-nui-callback" />

#### onNuiEventCallback：监听事件信息 <span id="on-nui-event-callback" />

<strong>方法签名</strong>

```
void onNuiEventCallback(NuiEvent event, final int resultCode, final int arg2, KwsResult kwsResult, AsrResult asrResult);
```

<strong>参数说明</strong>

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `event`
      </td>

      <td>
        `NuiEvent`
      </td>

      <td>
        回调事件。
      </td>
    </tr>

    <tr>
      <td>
        `resultCode`
      </td>

      <td>
        `int`
      </td>

      <td>
        仅在出现 `EVENT_ASR_ERROR` 事件时有效。
      </td>
    </tr>

    <tr>
      <td>
        `arg2`
      </td>

      <td>
        `int`
      </td>

      <td>
        保留参数。
      </td>
    </tr>

    <tr>
      <td>
        `asrResult`
      </td>

      <td>
        `AsrResult`
      </td>

      <td>
        语音识别结果。
      </td>
    </tr>

    <tr>
      <td>
        `kwsResult`
      </td>

      <td>
        `KwsResult`
      </td>

      <td>
        语音唤醒功能。无需关注该参数。
      </td>
    </tr>
  </tbody>
</table>

#### onNuiAudioStateChanged：监听音频状态 <span id="on-nui-audio-state-changed" />

SDK 通过此回调通知何时应该开始或停止录音。

<strong>方法签名</strong>

```
void onNuiAudioStateChanged(AudioState state);
```

<strong>AudioState状态说明</strong>

<table>
  <colgroup>
    <col style={{ width: "25%" }} />

    <col style={{ width: "75%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>状态</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `STATE_OPEN`
      </td>

      <td>
        交互启动，可以打开录音设备进行录音。
      </td>
    </tr>

    <tr>
      <td>
        `STATE_PAUSE`
      </td>

      <td>
        交互停止，可以停止录音。
      </td>
    </tr>

    <tr>
      <td>
        `STATE_CLOSE`
      </td>

      <td>
        SDK 实例已释放，可以彻底关闭录音设备。
      </td>
    </tr>
  </tbody>
</table>

#### onNuiNeedAudioData：填充待处理音频数据 <span id="on-nui-need-audio-data" />

开始对话后，该回调被连续触发，需在其中提供待处理的音频数据。参数audio\_update\_manually设置为"true"时可不关注这个回调。

<strong>方法签名</strong>

```
int onNuiNeedAudioData(byte[ ] buffer, int len);
```

<strong>参数说明</strong>

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `buffer`
      </td>

      <td>
        `byte[]`
      </td>

      <td>
        填充的音频数据。
      </td>
    </tr>

    <tr>
      <td>
        `len`
      </td>

      <td>
        `int`
      </td>

      <td>
        填充的音频数据的字节数。
      </td>
    </tr>
  </tbody>
</table>

<strong>返回值说明</strong>

实际填充的字节数。

#### onNuiAssistEventCallback：辅助数据和信息结果 <span id="on-nui-assist-event-callback" />

此回调用于接收 SDK 内部的辅助事件和相关数据。

<strong>方法签名</strong>

```
void onNuiAssistEventCallback_(int event, byte[ ] info, int info_len,
                               byte[ ] data);
```

<strong>参数说明</strong>

<table>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>参数</strong>
      </th>

      <th>
        <strong>类型</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        `event`
      </td>

      <td>
        `int`
      </td>

      <td>
        `NuiEvent`事件
      </td>
    </tr>

    <tr>
      <td>
        `info`
      </td>

      <td>
        `String`
      </td>

      <td>
        无需关注该参数。
      </td>
    </tr>

    <tr>
      <td>
        `info_len`
      </td>

      <td>
        `int`
      </td>

      <td>
        无需关注该参数。
      </td>
    </tr>

    <tr>
      <td>
        `data`
      </td>

      <td>
        `byte[]`
      </td>

      <td>
        辅助数据，比如AI返回的TTS音频。
      </td>
    </tr>
  </tbody>
</table>

#### onNuiLogTrackCallback：监听追踪日志 <span id="on-nui-log-track-callback" />

此回调用于接收 SDK 内部的详细日志，方便进行问题定位和调试。

```
default void onNuiLogTrackCallback(Constants.LogLevel level, String log)
```

### NuiEvent：事件类型 <span id="event-types" />

<table>
  <colgroup>
    <col style={{ width: "25%" }} />

    <col style={{ width: "75%" }} />
  </colgroup>

  <thead>
    <tr>
      <th>
        <strong>事件</strong>
      </th>

      <th>
        <strong>说明</strong>
      </th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td>
        EVENT\_TRANSCRIBER\_STARTED
      </td>

      <td>
        任务启动成功。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_VAD\_START
      </td>

      <td>
        任务启动后即触发该事件。不代表检测到人声起点。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_VAD\_END
      </td>

      <td>
        检测到人声终点。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_ASR\_PARTIAL\_RESULT
      </td>

      <td>
        语音识别中间结果。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_ASR\_ERROR
      </td>

      <td>
        语音对话过程中出现错误。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_MIC\_ERROR
      </td>

      <td>
        因连续2秒未收到任何音频数据而触发。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_SENTENCE\_END
      </td>

      <td>
        检测到一句话结束，此时会返回一句完整的识别结果。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_TRANSCRIBER\_COMPLETE
      </td>

      <td>
        语音对话结束。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_AUDIO\_TRANSCRIPTION
      </td>

      <td>
        音频模式下的文字字幕增量事件，流式返回字幕片段。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_AUDIO\_TRANSCRIPTION\_COMPLETED
      </td>

      <td>
        音频模式下的字幕输出完成事件。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_OTHER\_RESULT
      </td>

      <td>
        其他未归类事件信息，比如function\_call的返回结果等。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_ASR\_TTS\_START
      </td>

      <td>
        AI开始返回TTS数据。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_ASR\_TTS\_DATA
      </td>

      <td>
        AI返回的TTS数据。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_ASR\_TTS\_COMPLETE
      </td>

      <td>
        AI返回TTS数据结束。
      </td>
    </tr>

    <tr>
      <td>
        EVENT\_AEC\_DATA
      </td>

      <td>
        AEC回声消除后的音频数据。
      </td>
    </tr>
  </tbody>
</table>
