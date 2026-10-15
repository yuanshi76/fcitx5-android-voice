# 第三方 WebSocket 实时语音识别接入说明

本文档面向第三方应用开发者：如何通过 WebSocket 连接 CapsWriter-Offline 服务端，实时推送音频流并获取识别结果。

协议与服务端官方客户端完全一致（定义于 `core/protocol.py`），**服务端与官方客户端均无需任何改动**，只需按本文实现你的客户端。

> 区分：项目另有 OpenAI 兼容 HTTP 接口（默认端口 6017，`config_server.py` → `OpenAIAPIConfig`），适合「整段音频上传 → 返回全文」的离线转录；本文的 WebSocket 接口适合**实时流式**场景。

## 1. 连接参数

| 项目 | 值 | 说明 |
|---|---|---|
| URL | `ws://<服务端IP>:6016` | 由 `config_server.py` → `ServerConfig.addr` / `ServerConfig.port` 决定，默认 `0.0.0.0:6016` |
| 子协议 | 必须请求 `binary` | 服务端 `websockets.serve(..., subprotocols=["binary"])`；不协商该子协议会被握手拒绝 |
| 帧类型 | **文本帧**，内容为 JSON | `binary` 只是协商用的子协议名，实际消息仍是 JSON 文本帧 |
| 单帧上限 | 无限制（`max_size=None`） | 一帧可以携带较大的音频块 |
| 鉴权头 | `X-CapsWriter-Key: <ws_api_key>` | 仅当 `ServerConfig.ws_api_key` 非空时必需 |

### 1.1 握手鉴权

服务端 `core/server/connection/ws_auth.py` 在 HTTP Upgrade 阶段校验：

- `ws_api_key == ''`（默认）→ 不鉴权，直接放行（向后兼容）。
- `ws_api_key` 非空 → 握手请求头必须携带完全一致的 `X-CapsWriter-Key`，否则返回 **HTTP 401**，握手失败，不进入任何业务逻辑。

向第三方开放前，建议把 `config_server.py` 的 `ws_api_key` 设为强随机值；官方自带客户端在 `config_client.py` 的 `ws_api_key` 填同一个值即可（自带客户端已支持携带该头，见 `core/client/connection/websocket_manager.py`）。

### 1.2 连接示例

Python（`websockets` 库，与官方客户端同款写法）：

```python
import websockets

kwargs = dict(
    uri="ws://127.0.0.1:6016",
    subprotocols=["binary"],
    max_size=None,     # 不限单帧大小
    max_queue=None,    # 防止结果消费不及时反压发送
)
if int(websockets.__version__.split(".")[0]) >= 14:
    kwargs["proxy"] = None            # 本地连接禁用系统代理（14+ 才引入该参数）
    kwargs["additional_headers"] = {"X-CapsWriter-Key": "your-key"}
else:                                 # legacy 13.x
    kwargs["extra_headers"] = {"X-CapsWriter-Key": "your-key"}

ws = await websockets.connect(**kwargs)
```

Node.js（`ws` 包）：

```javascript
const WebSocket = require('ws');
const ws = new WebSocket('ws://127.0.0.1:6016', ['binary'],
    { headers: { 'X-CapsWriter-Key': 'your-key' } });
```

浏览器原生 WebSocket **不能自定义请求头**：启用鉴权时需经由本地代理/Node 层转发注入 `X-CapsWriter-Key`，或在可信局域网内保持 `ws_api_key=''`。

## 2. 消息协议

所有消息均为 **JSON 文本帧**。上行用 `AudioMessage`，下行用 `RecognitionMessage`，定义见 `core/protocol.py`。

### 2.1 上行：AudioMessage（客户端 → 服务端）

```json
{
  "task_id": "demo-0001",
  "source": "mic",
  "data": "<base64 编码的 PCM 数据>",
  "is_final": false,
  "time_start": 1730000000.0,
  "seg_duration": 4.0,
  "seg_overlap": 2.0,
  "context": "",
  "language": "auto"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `task_id` | string | ✅ | 一次说话/一个任务的唯一 ID（建议 UUID）。服务端按 `task_id` 隔离会话、累积结果，结果消息中原样带回。 |
| `source` | string | ✅ | `"mic"`（实时流）或 `"file"`（文件转录）。第三方实时识别用 `"mic"`。 |
| `data` | string | ✅ | **Base64 编码的 PCM**，内部格式固定为：**float32 小端、16000 Hz、单声道**（即每秒 64000 字节）。空字符串 `""` 合法，用于发送"收尾包"。 |
| `is_final` | bool | ✅ | 本任务的最后一个包。`true` 时服务端把剩余缓冲整体提交识别，并回一条 `is_final=true` 的精修结果，同时释放该任务会话。 |
| `time_start` | float | ✅ | 录音开始的 Unix 时间戳（秒），原样回传。 |
| `seg_duration` | float | 可选（默认 15.0） | **切片时长（秒）**，控制 partial 结果节奏，见第 3 节。实时场景建议 4~6。 |
| `seg_overlap` | float | 可选（默认 2.0） | **切片重叠（秒）**，用于跨片文本去重拼接。建议取 `seg_duration / 2 ~ seg_duration * 2 / 3`，且 ≥1。 |
| `context` | string | 可选 | 提示词/上下文（透传给引擎），一般留空。 |
| `language` | string | 可选（默认 `"auto"`） | 语言提示，如 `"zh"`、`"en"`，取决于所加载的 ASR 引擎是否支持。 |

> ⚠️ **切片参数每条消息都会生效**：服务端按当前消息携带的 `seg_duration`/`seg_overlap` 决定本条消息触发的切片行为。同一 `task_id` 内请**保持一致**，中途修改会造成 offset 错位、跨片文本去重异常。

### 2.2 下行：RecognitionMessage（服务端 → 客户端）

```json
{
  "task_id": "demo-0001",
  "is_final": false,
  "duration": 4.0,
  "time_start": 1730000000.0,
  "time_submit": 1730000004.12,
  "time_complete": 1730000005.34,
  "text": "这是截止到当前已识别的累积全文",
  "text_accu": "这是基于时间戳去重的精确累积全文",
  "tokens": ["这", "是", "截", "止", "..."],
  "timestamps": [0.12, 0.36, 0.58, "..."]
}
```

| 字段 | 说明 |
|---|---|
| `task_id` | 对应上行任务。 |
| `is_final` | `false` = partial（每识别完一个切片推一条）；`true` = 本任务最终结果（带标点/数字精修），**收到后该任务结束**。 |
| `text` | 主输出：各切片文本**简单拼接的累积全文**（不是增量！第三方做展示时如需增量，自行与上一条 partial 做前缀 diff）。 |
| `text_accu` | 精确输出：基于 token 时间戳去重的累积全文，质量略高但依赖引擎时间戳能力。 |
| `tokens` / `timestamps` | 字级 token 与对应时间戳（秒，相对 `time_start`），可用于卡拉OK式高亮。 |
| `duration` | 已处理的音频总时长（秒）。 |
| `time_submit` / `time_complete` | 最后切片的提交/完成时间，用于测延迟。 |

结果按连接路由：**从哪个连接发的音频，结果就回到哪个连接**。注意服务端的音频缓冲（`AudioCache`）是**每连接一个**而非每任务一个：同一连接上请**一次只跑一个活跃任务**（上一个任务收到 `is_final=true` 后再开新 `task_id`）；顺序复用连接完全没问题，但交错发送两个 `task_id` 的音频会混进同一缓冲导致结果错乱。多任务并发请为每个任务建立独立连接。

## 3. 实时性与切片机制（重要）

本服务的识别是**分块（chunked）非流式**的：服务端每凑满一段音频提交一次离线识别，识别完推一条 partial。延迟模型：

```
seg_threshold = seg_duration + 2 × seg_overlap     # 凑满这么多秒才切第一片
首条 partial 延迟 ≈ seg_threshold 秒（攒音频） + 该片推理耗时（RTF，通常 <1×片长）
之后每 seg_duration 秒一片，逐片推进
```

- **官方客户端**用 `60 + 4`（长句上下文最好，但 partial 很慢，松键才出 final）。
- **第三方想要"边说边出字"**：建议 `seg_duration=4~6`、`seg_overlap=2`（Qwen 引擎单片上限 80s，且 60s 以上推理变慢，不要用大值）。这样约 8~10 秒出一条累积 partial，说完发 `is_final` 后 1~2 秒内拿到带标点 final。
- **做不到逐字级实时**（引擎层不分帧流式解码），partial 粒度下限就是 `seg_duration`。

### 任务生命周期契约

1. 一个 `task_id` = 一次完整说话。期间可连续发多条 `is_final=false` 的音频包。
2. **必须**在某一刻发 `is_final=true`（可以 `data=""`），否则服务端会话不释放、也永远收不到带标点的最终结果。
3. 说完的收尾包两种发法任选：
   - 把剩余音频连同 `is_final=true` 一起发；
   - 或先发完所有音频（`is_final=false`），再发一条 `data=""` 且 `is_final=true` 的空包。
4. 同一 `task_id` 在收到 `is_final=true` 结果之前再次发 final 包会重复入队——一个任务只 final 一次。
5. 连接断开时服务端会摘除该连接并清理缓冲；子进程会定期回收已断连任务的会话（未 final 的 partial 结果会丢失，属预期行为）。
6. `seg_duration + seg_overlap` 之外的尾部缓冲不足一片时，不会提前出 partial，等 final。

## 4. 完整参考客户端（Python，可直接运行）

以"把一个 WAV 文件当作实时流推送并打印结果"为例，覆盖握手、鉴权、编码、切片参数、final 收尾与结果消费：

```python
# coding: utf-8
"""第三方 WebSocket 接入参考：读取 WAV -> 推流 -> 打印 partial/final。
依赖: pip install websockets ; 要求 16kHz 单声道 WAV (可用 ffmpeg 转换)。
用法: python demo_ws_client.py your.wav [ws://127.0.0.1:6016] [seg_duration] [key]
"""
import asyncio, base64, json, struct, sys, time, uuid
import websockets

async def main():
    wav_path = sys.argv[1]
    uri      = sys.argv[2] if len(sys.argv) > 2 else "ws://127.0.0.1:6016"
    seg      = float(sys.argv[3]) if len(sys.argv) > 3 else 4.0
    key      = sys.argv[4] if len(sys.argv) > 4 else ""

    # ---- 读 WAV 取 PCM (仅支持 16bit PCM WAV) ----
    with open(wav_path, "rb") as f:
        f.read(12)  # RIFF <size> WAVE
        fmt_data = b""; data_bytes = b""
        while head := f.read(8):
            if len(head) < 8: break
            cid, csz = head[:4], struct.unpack("<I", head[4:8])[0]
            payload = f.read(csz)
            if cid == b"fmt ": fmt_data = payload
            elif cid == b"data": data_bytes = payload
        sr, ch, bps = struct.unpack("<IHH", fmt_data[:8])
        assert sr == 16000 and ch == 1 and bps == 16, "需 16kHz/单声道/16bit WAV"
        pcm = struct.unpack(f"<{len(data_bytes)//2}h", data_bytes)
        audio = struct.pack(f"<{len(pcm)}f", *pcm)  # int16 -> float32（服务端内部格式）

    # ---- 连接 ----
    kwargs = dict(uri=uri, subprotocols=["binary"], max_size=None, max_queue=None)
    if tuple(int(v) for v in websockets.__version__.split(".")) >= (14,):
        kwargs["proxy"] = None
        if key: kwargs["additional_headers"] = {"X-CapsWriter-Key": key}
    else:
        if key: kwargs["extra_headers"] = {"X-CapsWriter-Key": key}

    async with websockets.connect(**kwargs) as ws:
        task_id, t0, chunk_sec = str(uuid.uuid4()), time.time(), 0.5
        bytes_per_chunk = int(16000 * 4 * chunk_sec)

        async def send():
            for i in range(0, len(audio), bytes_per_chunk):
                msg = {"task_id": task_id, "source": "mic",
                       "data": base64.b64encode(audio[i:i+bytes_per_chunk]).decode(),
                       "is_final": False, "time_start": t0,
                       "seg_duration": seg, "seg_overlap": min(2.0, seg)}
                await ws.send(json.dumps(msg, ensure_ascii=False))
                await asyncio.sleep(chunk_sec * 0.5)   # 模拟实时推流（比音频快即可）
            await ws.send(json.dumps({"task_id": task_id, "source": "mic",
                "data": "", "is_final": True, "time_start": t0,
                "seg_duration": seg, "seg_overlap": min(2.0, seg)}))

        async def recv():
            while True:
                r = json.loads(await ws.recv())
                if r["task_id"] != task_id: continue
                tag = "FINAL " if r["is_final"] else "partial"
                print(f"[{tag}] {r['text']}")
                if r["is_final"]: break

        await asyncio.gather(send(), recv())

asyncio.run(main())
```

要点对照：`data` 为 **base64(float32 LE)**；推流速率不影响正确性（服务端自己按 `seg_duration` 切片缓冲），但**发 final 之前必须把所有音频发完**。

## 5. FAQ / 注意事项

1. **握手 401**：`ws_api_key` 已启用但没带/带错 `X-CapsWriter-Key` 头。
2. **子协议协商失败**（HTTP 400 / Unsupported Subprotocol）：客户端请求了服务端不支持的子协议。请像官方客户端一样请求 `["binary"]`。
3. **一直收不到 partial，只来一条 final**：`seg_duration` 太大或音频太短（不足 `seg_duration + 2×seg_overlap` 秒时不切第一片，全部留到 final）。
4. **发了音频但没有任何回包**：忘了发 `is_final=true`，且剩余音频不足一片——partial 只在凑满一片时产生，final 才会把尾巴提交。
5. **增量文本怎么算**：`text` 是累积全文，第三方展示增量请用 `new = text[len(prev_text):]` 自行 diff（或按需用 `tokens/timestamps`）。
6. **并发限制**：识别由单个子进程串行推理（跨任务公平轮转）。连接数协议层无上限，但多路并发会摊薄吞吐、推高延迟；对外提供服务前建议自行控制活跃任务数，或仅在本机/可信内网使用。当前 WS 侧限流能力弱于 OpenAI HTTP 侧的 `AccessGuard`，暴露公网需谨慎。
7. **同一连接多任务**：只允许**顺序**复用（一个任务 final 结束后再开下一个 `task_id`）。交错发送两个活跃任务的音频会共用同一连接级缓冲而互相污染（见 §2.2）；需要并发请开多条连接。
8. **final 之后**：会话即释放，同一连接可直接开新 `task_id`，无需重连。
9. **`source` 选 `"file"` 的差异**：`"file"` 会在结果里补字级时间戳（用于字幕），实时推流请用 `"mic"`。
10. **Qwen 系列引擎注意**：单片音频超过约 80s 会被截断，务必让 `seg_duration + seg_overlap < 80`（推荐值远小于此，正常不会踩到）。

## 6. 快速自检清单（第三方对接验收）

- [ ] 握手成功（HTTP 101），子协议 `binary`，必要时带 `X-CapsWriter-Key`
- [ ] 发送的 `data` 解码后长度为 4 的整数倍（float32），采样率 16000、单声道
- [ ] 每条消息 `task_id` 一致，结束时发 `is_final=true`
- [ ] 能收到 ≥1 条 `is_final=false` partial（长音频时）与恰好 1 条 `is_final=true` final
- [ ] final 的 `text` 带标点（格式化后处理生效）



