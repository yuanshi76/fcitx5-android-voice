> ## Documentation Index
> Fetch the complete documentation index at: https://docs.bailian.console.aliyun.com/llms.txt
> Use this file to discover all available pages before exploring further.

# 移动端语音 SDK 选型与下载

> 下载用于百炼语音服务的 NUI 移动端 SDK，按平台和版本应用号选择整合包，并查看兼容要求与更新说明。

<meta property="og:title" content="移动端语音 SDK 选型与下载" />

{/* Maintenance: upstream NLS isi/sdk-selection-and-download. Sync cn/zh, cn/en, intl/zh, intl/en together. Keep platform/application-ID/package-variant rows stable; update version, full filename, CDN URL, MD5, compatibility and release notes together. Retain Model Studio package variants and compare their on-device capabilities. CDN URLs are shared across sites. */}

## 版本选择

版本号（如 `2.7.4`）表示 SDK 的发布版本；版本应用号（如 `037`、`047`）表示整合包的能力组合。

<Note>
  V2.7.3 及以前版本不支持百炼新版工作空间域名，使用该域名会导致 SDK 初始化或连接失败。请使用以下旧版通用地址：

  - 华北2（北京）：`wss://dashscope.aliyuncs.com/api-ws/v1/inference`
  - 新加坡：`wss://dashscope-intl.aliyuncs.com/api-ws/v1/inference`

  V2.7.4 及以上版本支持新版工作空间域名。
</Note>

下列版本应用号的 V2.7.4 和 V2.7.5 整合包均支持以下百炼云端能力：

- CosyVoice 语音合成
- Sambert 语音合成
- Fun-ASR 语音识别/录音文件识别
- Gummy 语音识别/翻译
- Qwen-Livetranslate-Realtime 实时音频翻译
- Qwen-Audio-Realtime 实时语音对话

各版本应用号的端侧能力如下。

<table style={{ display: "table", tableLayout: "fixed", width: "100%", overflowWrap: "anywhere" }}>
  <colgroup>
    <col style={{ width: "16%" }} />

    <col style={{ width: "24%" }} />

    <col style={{ width: "24%" }} />

    <col style={{ width: "36%" }} />
  </colgroup>

  <thead>
    <tr>
      <th style={{ verticalAlign: "top" }}>版本应用号</th>
      <th style={{ verticalAlign: "top" }}>端侧语音合成</th>
      <th style={{ verticalAlign: "top" }}>端侧语音唤醒</th>
      <th style={{ verticalAlign: "top" }}>端侧回声消除和人声检测</th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td style={{ verticalAlign: "top" }}><code>039</code></td>
      <td style={{ verticalAlign: "top" }}>支持</td>
      <td style={{ verticalAlign: "top" }}>支持</td>
      <td style={{ verticalAlign: "top" }}>不支持</td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}><code>038</code></td>
      <td style={{ verticalAlign: "top" }}>支持</td>
      <td style={{ verticalAlign: "top" }}>不支持</td>
      <td style={{ verticalAlign: "top" }}>不支持</td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}><code>037</code></td>
      <td style={{ verticalAlign: "top" }}>不支持</td>
      <td style={{ verticalAlign: "top" }}>不支持</td>
      <td style={{ verticalAlign: "top" }}>不支持</td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}><code>049</code></td>
      <td style={{ verticalAlign: "top" }}>支持</td>
      <td style={{ verticalAlign: "top" }}>支持</td>
      <td style={{ verticalAlign: "top" }}>支持</td>
    </tr>

    <tr>
      <td style={{ verticalAlign: "top" }}><code>047</code></td>
      <td style={{ verticalAlign: "top" }}>不支持</td>
      <td style={{ verticalAlign: "top" }}>不支持</td>
      <td style={{ verticalAlign: "top" }}>支持</td>
    </tr>
  </tbody>
</table>

## SDK 下载

请按平台和所需功能选择 SDK。

<Note>
  标准整合包用于常规集成。若工程中已集成同名 SDK，可使用改名整合包解决包冲突。
</Note>

<Tabs>
  <Tab title="Android">
    平台更新时间：2026-10-10。

    包名：`com.alibaba.idst.nui`。支持 Android 5.0 及以上版本（API Level 21），架构为 `armeabi-v7a`、`arm64-v8a`、`x86_64`。

    <Tabs>
      <Tab title="标准整合包">
        <table style={{ display: "table", tableLayout: "fixed", width: "100%", overflowWrap: "anywhere" }}>
          <colgroup>
            <col style={{ width: "12%" }} />

            <col style={{ width: "10%" }} />

            <col style={{ width: "48%" }} />

            <col style={{ width: "30%" }} />
          </colgroup>

          <thead>
            <tr>
              <th style={{ verticalAlign: "top" }}>版本应用号</th>
              <th style={{ verticalAlign: "top" }}>版本</th>
              <th style={{ verticalAlign: "top" }}>下载</th>
              <th style={{ verticalAlign: "top" }}>MD5</th>
            </tr>
          </thead>

          <tbody>
            <tr>
              <td style={{ verticalAlign: "top" }}><code>039</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260907/uhqetd/V2.7.4-039-20260907_Android_TLS.tar.gz">V2.7.4-039-20260907\_Android\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>61e183ba52bbbf6f9c29e27a32fb8e93</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>038</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260831/ftapax/V2.7.4-038-20260831_Android_OpenSSL.tar.gz">V2.7.4-038-20260831\_Android\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>0c3d73bcdfcef29a1472dc8068977a2c</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>037</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.5</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20261010/dbvtqi/V2.7.5-037-20261010_Android_TLS.tar.gz">V2.7.5-037-20261010\_Android\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>c1bd9c2421bc36541811f860b0d8db6f</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>049</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260907/uvvbdm/V2.7.4-049-20260907_Android_TLS.tar.gz">V2.7.4-049-20260907\_Android\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>739777fdebcf7a31a3715da5e834f557</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>047</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260831/vdepst/V2.7.4-047-20260831_Android_OpenSSL.tar.gz">V2.7.4-047-20260831\_Android\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>02991026fdbca0cc5382e8cb3b5fc938</code></td>
            </tr>
          </tbody>
        </table>
      </Tab>

      <Tab title="改名整合包">
        <table style={{ display: "table", tableLayout: "fixed", width: "100%", overflowWrap: "anywhere" }}>
          <colgroup>
            <col style={{ width: "12%" }} />

            <col style={{ width: "10%" }} />

            <col style={{ width: "48%" }} />

            <col style={{ width: "30%" }} />
          </colgroup>

          <thead>
            <tr>
              <th style={{ verticalAlign: "top" }}>版本应用号</th>
              <th style={{ verticalAlign: "top" }}>版本</th>
              <th style={{ verticalAlign: "top" }}>下载</th>
              <th style={{ verticalAlign: "top" }}>MD5</th>
            </tr>
          </thead>

          <tbody>
            <tr>
              <td style={{ verticalAlign: "top" }}><code>039</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260909/vhbuzr/V2.7.4-039-20260909_Android_MIT_TLS.tar.gz">V2.7.4-039-20260909\_Android\_MIT\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>6c60f6d9038166d442dbef460600a26c</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>038</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260909/htqhde/V2.7.4-038-20260909_Android_MIT_TLS.tar.gz">V2.7.4-038-20260909\_Android\_MIT\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>fbe0042f835a9cab589bdd166a21f35a</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>037</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.2</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260225/ufcoym/V2.7.2-037-20260225_Android_MIT_OpenSSL.tar.gz">V2.7.2-037-20260225\_Android\_MIT\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>ee3e0f44cae2d56d444eed1a50b57588</code></td>
            </tr>
          </tbody>
        </table>
      </Tab>
    </Tabs>
  </Tab>

  <Tab title="iOS">
    平台更新时间：2026-10-10。

    最低支持 iOS 12，架构为 `arm64-device`、`arm64-simulator`、`x86_64-simulator`。

    <Tabs>
      <Tab title="标准整合包">
        <table style={{ display: "table", tableLayout: "fixed", width: "100%", overflowWrap: "anywhere" }}>
          <colgroup>
            <col style={{ width: "12%" }} />

            <col style={{ width: "10%" }} />

            <col style={{ width: "48%" }} />

            <col style={{ width: "30%" }} />
          </colgroup>

          <thead>
            <tr>
              <th style={{ verticalAlign: "top" }}>版本应用号</th>
              <th style={{ verticalAlign: "top" }}>版本</th>
              <th style={{ verticalAlign: "top" }}>下载</th>
              <th style={{ verticalAlign: "top" }}>MD5</th>
            </tr>
          </thead>

          <tbody>
            <tr>
              <td style={{ verticalAlign: "top" }}><code>039</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260904/swlcrs/V2.7.4-039-20260903_iOS_TLS.tar.gz">V2.7.4-039-20260903\_iOS\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>881f91817804333bd538317f04612921</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>038</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260904/foudvq/V2.7.4-038-20260903_iOS_TLS.tar.gz">V2.7.4-038-20260903\_iOS\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>fd7926448f3f8330fa0bee62256924a8</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>037</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.5</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20261010/plfmbi/V2.7.5-037-20261010_iOS_TLS.tar.gz">V2.7.5-037-20261010\_iOS\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>1bcc6b1e672ef759df3743e9273b1616</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>049</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260904/psyktu/V2.7.4-049-20260903_iOS_TLS.tar.gz">V2.7.4-049-20260903\_iOS\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>f6600a30d3e1e4b8b353ba8929685d8b</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>047</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260904/mtvbua/V2.7.4-047-20260903_iOS_TLS.tar.gz">V2.7.4-047-20260903\_iOS\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>8aa8169d7e943bf51ed9c3e5b446254c</code></td>
            </tr>
          </tbody>
        </table>
      </Tab>

      <Tab title="改名整合包">
        <table style={{ display: "table", tableLayout: "fixed", width: "100%", overflowWrap: "anywhere" }}>
          <colgroup>
            <col style={{ width: "12%" }} />

            <col style={{ width: "10%" }} />

            <col style={{ width: "48%" }} />

            <col style={{ width: "30%" }} />
          </colgroup>

          <thead>
            <tr>
              <th style={{ verticalAlign: "top" }}>版本应用号</th>
              <th style={{ verticalAlign: "top" }}>版本</th>
              <th style={{ verticalAlign: "top" }}>下载</th>
              <th style={{ verticalAlign: "top" }}>MD5</th>
            </tr>
          </thead>

          <tbody>
            <tr>
              <td style={{ verticalAlign: "top" }}><code>039</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.2</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260225/bcousy/V2.7.2-039-20260225_iOS_MIT_OpenSSL.tar.gz">V2.7.2-039-20260225\_iOS\_MIT\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>c1192a125b0239ff295cf9c9ff598807</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>038</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.2</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260225/luwrff/V2.7.2-038-20260225_iOS_MIT_OpenSSL.tar.gz">V2.7.2-038-20260225\_iOS\_MIT\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>d9da2453a60bc69d43b9a35bd0c9f303</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>037</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.2</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260225/lfkafa/V2.7.2-037-20260225_iOS_MIT_OpenSSL.tar.gz">V2.7.2-037-20260225\_iOS\_MIT\_TLS.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>bb3cd0a35d12e3fc0c35b016f0788fbf</code></td>
            </tr>
          </tbody>
        </table>
      </Tab>
    </Tabs>
  </Tab>

  <Tab title="HarmonyOS">
    平台更新时间：2026-09-08。

    <Tabs>
      <Tab title="标准整合包">
        <table style={{ display: "table", tableLayout: "fixed", width: "100%", overflowWrap: "anywhere" }}>
          <colgroup>
            <col style={{ width: "12%" }} />

            <col style={{ width: "10%" }} />

            <col style={{ width: "48%" }} />

            <col style={{ width: "30%" }} />
          </colgroup>

          <thead>
            <tr>
              <th style={{ verticalAlign: "top" }}>版本应用号</th>
              <th style={{ verticalAlign: "top" }}>版本</th>
              <th style={{ verticalAlign: "top" }}>下载</th>
              <th style={{ verticalAlign: "top" }}>MD5</th>
            </tr>
          </thead>

          <tbody>
            <tr>
              <td style={{ verticalAlign: "top" }}><code>047</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260908/pkfaul/2.7.4-047-20260908_Harmony.tar.gz">2.7.4-047-20260908\_Harmony.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>447f4418b8f563dba8b507da8486a28b</code></td>
            </tr>

            <tr>
              <td style={{ verticalAlign: "top" }}><code>049</code></td>
              <td style={{ verticalAlign: "top" }}>2.7.4</td>
              <td style={{ verticalAlign: "top" }}><Link href="https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20260908/rvrpcc/2.7.4-049-20260908_Harmony.tar.gz">2.7.4-049-20260908\_Harmony.tar.gz</Link></td>
              <td style={{ verticalAlign: "top" }}><code>61039f0082966b3f774ed556c7f75652</code></td>
            </tr>
          </tbody>
        </table>
      </Tab>

      <Tab title="改名整合包">
        暂未提供。
      </Tab>
    </Tabs>
  </Tab>
</Tabs>

## 更新说明

### V2.7.5

- 优化实时识别断网续传功能。
- 支持 Qwen-Audio-ASR 新模型。
- 修复万能参数设置接口失效的问题。

### V2.7.4

- 新增 Qwen-Audio-Realtime 实时语音对话。
- 支持百炼新版工作空间域名。

### V2.7.3

- 新增实时音频翻译功能。
- 新增断网续传功能和断网报错。

### V2.7.2

- 修复缺陷。

### V2.7.1

- 修复缺陷。

### V2.7.0

- 新增百炼语音识别与语音合成支持。
- 语音合成新增将 Opus 和 MP3 解码为 PCM 的能力。
- 精简 OpenSSL 以减少包大小。
