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
        日志文件的存�