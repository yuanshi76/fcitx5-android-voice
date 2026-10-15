> ## Documentation Index
> Fetch the complete documentation index at: https://docs.bailian.console.aliyun.com/llms.txt
> Use this file to discover all available pages before exploring further.

# 非实时语音识别（Qwen-Audio-3.x-ASR-Flash/Fun-ASR-Flash）Android SDK

> Qwen-Audio-3.x-ASR-Flash/Fun-ASR-Flash非实时语音识别Android SDK可将语音转换为文本。

<meta property="og:title" content="非实时语音识别（Qwen-Audio-3.x-ASR-Flash/Fun-ASR-Flash）Android SDK" />

**用户指南：**[非实时语音识别](/zh/model-studio/non-realtime-speech-recognition-user-guide)。关于支持的音频格式、文件大小限制、时长限制等输入要求，请参见[音频规格](/zh/model-studio/asr-model#asr_audio_spec02)。

## 快速开始

1. **获取API Key：**[获取API Key](/zh/model-studio/get-api-key)，为安全起见，推荐将API Key配置到环境变量。
2. **下载SDK并运行示例代码：**
   - [下载最新SDK整合包](/zh/model-studio/sdk-selection-and-download)。
   - 解压 ZIP 包。在 `app/libs` 目录中获取 AAR 格式 SDK，并添加到项目依赖。
     需要 Android CPP 接入时，使用 ZIP 包内的 `android_libs` 与 `android_include` 获取动态库和头文件。
   - 用 Android Studio 打开工程。示例代码位于`DashFunAsrFlashFileTranscriberActivity.java`，替换 API Key 后体验功能。

### 调用步骤

#### 同步模式

1. 初始化 SDK

2. 按业务需求配置相关参数

3. 调用 `startFileTranscriber` 发送非实时语音识别请求，并等待结果返回。

4. 在`onFileTransEventCallback`接口中监听`EVENT_FILE_TRANS_RESULT` 事件，获取最终识别结果。

5. 调用 `release` 释放 SDK 资源

## 请求参数

### 连接与控制参数 <span id="connection-parameters" />

通过在[initialize](#initialize)接口的`parameters`参数中传入一个JSON字符串来配置。

<strong>参数示例：</strong>以下为 JSON 字符串示例，参数未完整列出。请按实际需求在编码时补充：

```
{
    "url": "wss://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation",
    "apikey": "st-****",
    "device_id": "my_device_id",
    "service_mode": "1"
}
```

<strong>参数说明</strong>

<table style={{ display: "table", width: "100%", tableLayout: "fixed", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "40%" }} />
  </colgroup>

  <thead>
    <tr><th style={{ verticalAlign: "top" }}><strong>参数</strong></th><th style={{ verticalAlign: "top" }}><strong>类型</strong></th><th style={{ verticalAlign: "top" }}><strong>是否必须</strong></th><th style={{ verticalAlign: "top" }}><strong>说明</strong></th></tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>url</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>String</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        服务地址：<br /><ul><li><code>wss\://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation</code></li><li>华北2（北京）：<code>wss\://\{WorkspaceId}.cn-beijing.maas.aliyuncs.com</code>/api/v1/services/aigc/multimodal-generation/generation</li><li>新加坡：<code>wss\://\{WorkspaceId}.ap-southeast-1.maas.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation</code></li></ul><br />调用时，请将  <code>\{WorkspaceId}</code>  替换为真实的  <a href="/zh/model-studio/regions#h2_migrate_domain">Workspace ID</a>。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>apikey</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>String</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        API Key。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>service\_mode</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>String</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        运行模式。非实时语音识别固定为 <code>"1"</code>。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>device\_id</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>String</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        用于标识终端用户的唯一字符串，可设为应用内用户ID或客户端生成的设备唯一标识符。此ID主要用于日志追踪和问题排查。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>debug\_path</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>String</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        日志文件的存储路径。 此参数仅在调用<a href="#initialize">initialize</a>接口时将<code>save\_log</code>设为true时生效。此时必须设置日志文件路径，否则将报错。 本地最多保留两个日志文件。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>max\_log\_file\_size</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>int</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        设定日志文件的最大字节数。 此参数仅在调用<a href="#initialize">initialize</a>接口时将<code>save\_log</code>设为true时生效。 默认值：104857600（100 \* 1024 \* 1024 字节, 即 100MiB）。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>log\_track\_level</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>int</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        控制通过日志回调（<code>onFileTransLogTrackCallback</code>）对外发送的日志内容的过滤级别。 默认值：2。 取值范围： - 0：LOG\_LEVEL\_VERBOSE - 1：LOG\_LEVEL\_DEBUG - 2：LOG\_LEVEL\_INFO - 3：LOG\_LEVEL\_WARNING - 4：LOG\_LEVEL\_ERROR - 5：LOG\_LEVEL\_NONE（表示关闭此功能） 注意：<code>log\_track\_level</code>与<code>level</code>（通过<a href="#initialize">initialize</a>接口设置）共同决定最终回调的日志。一条日志的级别数值必须同时大于或等于<code>log\_track\_level</code>和<code>level</code>的值，才会被回调。例如，<code>log\_track\_level</code>设为2 (INFO)，<code>level</code>设为3 (WARNING)，则只有WARNING及以上级别（数值>=3）的日志才会被回调。
      </td>
    </tr>
  </tbody>
</table>

### 语音识别效果参数 <span id="recognition-parameters" />

通过[startFileTranscriber](#startfiletranscriber)接口配置所有语音识别效果参数。

<strong>参数示例：</strong>以下为 JSON 字符串示例，参数未完整列出。请按实际需求在编码时补充：

```
{
  "apikey": "st-****",
  "messages": [
    {
      "content": [
        {
          "input_audio": {
            "data": "{YOUR_AUDIO_URL}"
          },
          "type": "input_audio"
        }
      ],
      "role": "user"
    }
  ],
  "nls_config": {
    "format": "mp3",
    "model": "qwen-audio-3.0-asr-flash"
  }
}
```

<strong>参数说明</strong>

<table style={{ display: "table", width: "100%", tableLayout: "fixed", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "40%" }} />
  </colgroup>

  <thead>
    <tr><th style={{ verticalAlign: "top" }}><strong>参数</strong></th><th style={{ verticalAlign: "top" }}><strong>类型</strong></th><th style={{ verticalAlign: "top" }}><strong>是否必须</strong></th><th style={{ verticalAlign: "top" }}><strong>说明</strong></th></tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>apikey</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>string</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        如果<a href="#connection-parameters">连接与控制参数</a>的<code>apikey</code>使用的是<a href="/zh/model-studio/generate-temporary-api-key">临时API Key</a>，可在此处进行更新，以免超时失效。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>nls\_config</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>object</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        语音识别核心配置对象，包含模型选择、识别效果控制等关键参数。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>nls\_config.model</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>string</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        指定示例调用的模型。模型信息请参见[支持的模型与地域](/zh/model-studio/non-realtime-speech-recognition-user-guide#4a43cc1bb7kxg)。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>nls\_config.format</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>string</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        音频格式。根据实际音频格式填写，支持<code>wav</code>、<code>mp3</code>、<code>opus</code>等。详情请参见<a href="/zh/model-studio/asr-model#asr_audio_spec02">音频规格</a>。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>nls\_config.sample\_rate</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>string</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        音频采样率，单位Hz。例如<code>16000</code>表示16kHz采样率。详情请参见<a href="/zh/model-studio/asr-model#asr_audio_spec02">音频规格</a>。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>nls\_config.vocabulary\_id</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>string</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        预编译热词列表 ID。 需预先调用创建热词列表接口生成，识别时传入该 ID 即可使用列表中的热词。 适用于词汇已知且相对稳定、需要跨请求复用同一词表的场景。 使用方法请参见<a href="/zh/model-studio/improve-asr-accuracy#hw_precompiled_h3">预编译热词</a>。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>nls\_config.instant\_vocabulary</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>object</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        即时热词。<br />以键值对形式传入，键为热词文本（<code>string</code>），值为热词权重（<code>integer</code>），无需预先创建热词列表。权重取值范围为 \[1, 5] 或 50：取 \[1, 5] 时值越大模型越倾向输出该词；取 50 时为超级热词，召回率大幅提升，但超级热词数量最多不超过 50 个。<br />适用于临时性、会话级别的热词优化。<br />与预编译热词同时配置时，系统会合并两类热词；合并后超过 2000 个时，随机选择 2000 个使用。使用方法请参见<a href="/zh/model-studio/improve-asr-accuracy#hw_instant_h3">即时热词</a>。<br />

        <Tip>
          即时热词的适用模型及限制请参见[即时热词](/zh/model-studio/improve-asr-accuracy#hw_instant_h3)。
        </Tip>

        <br />

        ```json
        {
          "张三": 5,
          "李四": 5
        }
        ```
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>nls\_config.language\_hints</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>array\[string]</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        设置待识别语言代码。如果无法提前确定语种，可不设置，模型会自动识别语种。<br />对于 Qwen-Audio-3.x-ASR-Flash 系列模型，最多支持设置 4 个值，即便设置超出 4 个，也仅前 4 个生效；对于 Fun-ASR-Flash 系列模型，仅支持设置 1 个值，即便设置多个，也仅第一个生效。<br /><ul><li>Qwen-Audio-3.x-ASR-Flash、fun-asr-flash-2026-06-15：</li><li>zh: 中文</li><li>en: 英文</li><li>ja: 日语</li><li>ko：韩语</li><li>vi：越南语</li><li>th：泰语</li><li>id：印尼语</li><li>ms：马来语</li><li>tl：菲律宾语</li><li>hi：印地语</li><li>ar：阿拉伯语</li><li>fr：法语</li><li>de：德语</li><li>es：西班牙语</li><li>pt：葡萄牙语</li><li>ru：俄语</li><li>it：意大利语</li><li>nl：荷兰语</li><li>sv：瑞典语</li><li>da：丹麦语</li><li>fi：芬兰语</li><li>no：挪威语</li><li>el：希腊语</li><li>pl：波兰语</li><li>cs：捷克语</li><li>hu：匈牙利语</li><li>ro：罗马尼亚语</li><li>bg：保加利亚语</li><li>hr：克罗地亚语</li><li>sk：斯洛伐克语</li></ul>
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        messages
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>array\[object]</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        消息列表。包含当前待识别的音频，以及可选的对话上下文（用于提升识别效果）。<br /><strong>详见如下说明。</strong>
      </td>
    </tr>
  </tbody>
</table>

<strong>messages参数说明：</strong>

<Tip>
  上下文功能用于提升专有词汇的识别准确率，使用方法详见[上下文增强](/zh/model-studio/improve-asr-accuracy#ctx_enhance_h2)。

  **约束**：上下文消息（`input_text`  和  `text`  类型）各最多 5 条，超出时保留最近的 5 条。每轮上下文文本总长度（`user`  和  `assistant`  的  `text`  字段长度之和）不超过 400 个字符（按字符数计算，每个字符计为 1），超出部分从末尾截断。
</Tip>

<Tip>
  携带上下文时，`messages`  中的消息顺序有要求：上下文消息必须按对话轮次排列，每轮中  `user`（`input_text`  类型）必须在对应的  `assistant`（`text`  类型）之前；包含  `input_audio`  的  `user`  消息必须放在  `messages`  数组的最后。
</Tip>

<table style={{ display: "table", width: "100%", tableLayout: "fixed", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "40%" }} />
  </colgroup>

  <thead>
    <tr><th style={{ verticalAlign: "top" }}><strong>参数</strong></th><th style={{ verticalAlign: "top" }}><strong>类型</strong></th><th style={{ verticalAlign: "top" }}><strong>是否必须</strong></th><th style={{ verticalAlign: "top" }}><strong>说明</strong></th></tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}>
        role
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>string</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        消息角色。取值范围：<br /><ul><li><code>user</code>（必选）：用户消息。type为<code>input\_audio</code>时表示当前待识别的音频；type为<code>input\_text</code>时表示前几轮的识别结果或领域相关的词表（可选，上下文）。</li><li><code>assistant</code>（可选，上下文）：前几轮大语言模型的回复内容。</li></ul>
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        content
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>array\[object]</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        消息内容列表。详细如下说明。
      </td>
    </tr>
  </tbody>
</table>

<strong>content参数说明：</strong>

<table style={{ display: "table", width: "100%", tableLayout: "fixed", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "40%" }} />
  </colgroup>

  <thead>
    <tr><th style={{ verticalAlign: "top" }}><strong>参数</strong></th><th style={{ verticalAlign: "top" }}><strong>类型</strong></th><th style={{ verticalAlign: "top" }}><strong>是否必须</strong></th><th style={{ verticalAlign: "top" }}><strong>说明</strong></th></tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}>
        type
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>string</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        内容类型。每个请求至少需要一条<code>input\_audio</code>类型的消息。取值范围：<br /><ul><li><code>input\_audio</code>（必选）：当前待识别的音频输入（role为user），需同时传入<code>input\_audio</code>对象。</li><li><code>input\_text</code>（可选，上下文）：前几轮用户语音的识别结果或领域相关的词表（role为user），需同时传入<code>text</code>字段。</li><li><code>text</code>（可选，上下文）：前几轮大语言模型的回复内容（role为assistant），需同时传入<code>text</code>字段。</li></ul>
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        input\_audio
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>object</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        当<code>type</code>为<code>input\_audio</code>时必填。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        input\_audio.data
      </td>

      <td style={{ verticalAlign: "top" }}>
        string
      </td>

      <td style={{ verticalAlign: "top" }}>
        是
      </td>

      <td style={{ verticalAlign: "top" }}>
        待识别音频数据。关于支持的音频格式、文件大小限制、时长限制等输入要求，请参见<a href="/zh/model-studio/asr-model#asr_audio_spec02">音频规格</a>。支持以下两种方式：<br /><ul><li><strong>音频文件URL</strong>：直接传入可公开访问的音频文件地址。</li><li><strong>Base64 Data URI</strong>：采用Data URI格式传入Base64编码的音频数据，值由<code>data:\{MIME\_TYPE};base64,</code>前缀与Base64编码的音频数据拼接而成。支持的MIME类型包括<code>audio/wav</code>、<code>audio/mp3</code>等。</li></ul><br />示例（URL方式）：<code>[https://example.com/audio/sample.wav](https://example.com/audio/sample.wav)</code><br />示例（Base64方式）：<code>data:audio/wav;base64,\{BASE64\_ENCODED\_DATA}</code>
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        text
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>string</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        否
      </td>

      <td style={{ verticalAlign: "top" }}>
        当<code>type</code>为<code>input\_text</code>时，填入前几轮用户语音的识别结果或领域相关的词表；当<code>type</code>为<code>text</code>时，填入前几轮大语言模型的回复内容。文本按字符数计算，每个字符计为 1。每轮上下文中所有消息的  <code>text</code>  字段长度之和不超过 400 个字符，超出部分从末尾截断。
      </td>
    </tr>
  </tbody>
</table>

## 关键接口

### NativeNui

#### initialize <span id="initialize" />

初始化语音识别SDK实例。SDK为单例模式，在调用[release](#release)前禁止重复初始化。

此接口会引起阻塞，应在非UI线程调用。

<strong>方法签名</strong>

```
public synchronized int initialize(final INativeFileTransCallback callback,
                                   String parameters,
                                   final Constants.LogLevel level,
                                   final boolean save_log)
```

<strong>参数说明</strong>

<table style={{ display: "table", width: "100%", tableLayout: "fixed", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr><th style={{ verticalAlign: "top" }}><strong>参数</strong></th><th style={{ verticalAlign: "top" }}><strong>类型</strong></th><th style={{ verticalAlign: "top" }}><strong>说明</strong></th></tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>callback</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>INativeFileTransCallback</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        事件和数据回调接口的实现。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>parameters</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>String</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        JSON字符串，包含鉴权、连接和调试参数。参见<a href="#connection-parameters">连接与控制参数</a>。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>level</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>Constants.LogLevel</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        控制SDK自身日志的打印级别。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>save\_log</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>boolean</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        是否保存本地日志。若为<code>true</code>，须在<a href="#connection-parameters">连接与控制参数</a>中通过<code>debug\_path</code>指定路径，并可通过<code>max\_log\_file\_size</code>设置文件大小。
      </td>
    </tr>
  </tbody>
</table>

#### setParams <span id="setparams" />

此接口用于独立设置或更新 `nls_config`  参数。如果所有参数都在[startFileTranscriber](#startfiletranscriber)中一次性提供，则无需调用此方法。

<strong>方法签名</strong>

```
public synchronized int setParams(String params);
```

<strong>参数说明</strong>

<table style={{ display: "table", width: "100%", tableLayout: "fixed", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr><th style={{ verticalAlign: "top" }}><strong>参数</strong></th><th style={{ verticalAlign: "top" }}><strong>类型</strong></th><th style={{ verticalAlign: "top" }}><strong>说明</strong></th></tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>params</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>String</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <a href="#recognition-parameters">语音识别效果参数</a>中的<code>nls\_config</code>和messages参数。<br /> 示例： <br />

        ```json
        {
          "messages": [
            {
              "content": [
                {
                  "input_audio": {
                    "data": "{YOUR_AUDIO_URL}"
                  },
                  "type": "input_audio"
                }
              ],
              "role": "user"
            }
          ],
          "nls_config": {
            "format": "mp3",
            "model": "qwen-audio-3.0-asr-flash"
          }
        }
        ```
      </td>
    </tr>
  </tbody>
</table>

#### startFileTranscriber <span id="startfiletranscriber" />

开始识别。

<strong>方法签名</strong>

```
public synchronized int startFileTranscriber(String params, byte[ ] task_id)
```

<strong>参数说明</strong>

<table style={{ display: "table", width: "100%", tableLayout: "fixed", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr><th style={{ verticalAlign: "top" }}><strong>参数</strong></th><th style={{ verticalAlign: "top" }}><strong>类型</strong></th><th style={{ verticalAlign: "top" }}><strong>说明</strong></th></tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>params</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>String</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <a href="#recognition-parameters">语音识别效果参数</a>。 示例： <br />

        ```json
        {
          "apikey": "st-****",
          "messages": [
            {
              "content": [
                {
                  "input_audio": {
                    "data": "{YOUR_AUDIO_URL}"
                  },
                  "type": "input_audio"
                }
              ],
              "role": "user"
            }
          ],
          "nls_config": {
            "format": "mp3",
            "model": "qwen-audio-3.0-asr-flash"
          }
        }
        ```
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>task\_id</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>byte\[ ]</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        可不关注，可填null
      </td>
    </tr>
  </tbody>
</table>

#### queryFileTranscriber

此非实时语音识别功能仅支持同步请求，无需关注此接口。

#### cancelFileTranscriber

立即取消当前任务。

<strong>方法签名</strong>

```
public synchronized int cancelFileTranscriber(String task_id)
```

<strong>参数说明</strong>

<table style={{ display: "table", width: "100%", tableLayout: "fixed", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr><th style={{ verticalAlign: "top" }}><strong>参数</strong></th><th style={{ verticalAlign: "top" }}><strong>类型</strong></th><th style={{ verticalAlign: "top" }}><strong>说明</strong></th></tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>task\_id</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>String</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        待取消的任务ID。通过EVENT\_FILE\_TRANS\_UPLOADED获取。
      </td>
    </tr>
  </tbody>
</table>

#### release <span id="release" />

释放SDK所有内部资源。此方法调用后，SDK实例将变为不可用状态，如需再次使用，必须重新调用[initialize](#initialize)进行初始化。

<strong>方法签名</strong>

```
public synchronized int release();
```

#### GetVersion

获得当前SDK版本信息。

<strong>方法签名</strong>

```
public synchronized String GetVersion();
```

<strong>返回值说明</strong>

当前SDK版本信息。

### INativeFileTransCallback：监听回调 <span id="file-trans-callback" />

#### onFileTransEventCallback：监听事件和语音识别结果

<strong>方法签名</strong>

```
void onFileTransEventCallback(NuiEvent event, final int resultCode, final int arg2, AsrResult asrResult, String taskId);
```

<strong>参数说明</strong>

<table style={{ display: "table", width: "100%", tableLayout: "fixed", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "20%" }} />

    <col style={{ width: "20%" }} />

    <col style={{ width: "60%" }} />
  </colgroup>

  <thead>
    <tr><th style={{ verticalAlign: "top" }}><strong>参数</strong></th><th style={{ verticalAlign: "top" }}><strong>类型</strong></th><th style={{ verticalAlign: "top" }}><strong>说明</strong></th></tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>event</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>NuiEvent</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        回调事件。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>resultCode</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>int</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        仅在出现 `EVENT_ASR_ERROR` 事件时有效。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>asrResult</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>AsrResult</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        语音识别结果。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>taskId</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>String</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        任务ID。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        <code>arg2</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        <code>int</code>
      </td>

      <td style={{ verticalAlign: "top" }}>
        保留参数。
      </td>
    </tr>
  </tbody>
</table>

#### onFileTransLogTrackCallback：监听追踪日志 <span id="log-callback" />

此回调用于接收 SDK 内部的详细日志，方便进行问题定位和调试。

```
default void onFileTransLogTrackCallback(Constants.LogLevel level, String log)
```

### 事件类型

<table style={{ display: "table", width: "100%", tableLayout: "fixed", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "25%" }} />

    <col style={{ width: "75%" }} />
  </colgroup>

  <thead>
    <tr><th style={{ verticalAlign: "top" }}><strong>事件</strong></th><th style={{ verticalAlign: "top" }}><strong>说明</strong></th></tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}>
        EVENT\_FILE\_TRANS\_CONNECTED
      </td>

      <td style={{ verticalAlign: "top" }}>
        连接服务成功。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        EVENT\_FILE\_TRANS\_UPLOADED
      </td>

      <td style={{ verticalAlign: "top" }}>
        上传待识别音频文件成功，此时可获得当前任务的task\_id。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        EVENT\_FILE\_TRANS\_RESULT
      </td>

      <td style={{ verticalAlign: "top" }}>
        识别最终结果。
      </td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}>
        EVENT\_ASR\_ERROR
      </td>

      <td style={{ verticalAlign: "top" }}>
        语音识别过程中出现错误。
      </td>
    </tr>
  </tbody>
</table>
