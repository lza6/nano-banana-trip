# Nano Banana Trip 协议知识库（深度版）

> 本文件是给后续维护者/渠道移植者的完整技术参考。README 是概览，这里是全部细节。

## 1. 凭据体系（唯一有效凭据）

```
appID      = 998ba275
apiKey     = 146a72cc8e229f213a70a61b4477ad1e
apiSecret  = 4508749f67dde2db7f9bad60   (传输时 base64: NDUwODc0OWY2N2RkZTJkYjdmOWJhZDYw)
```

来源：`POST https://www.haichuang-tech.com:8082/webapi/config/findConfig`（匿名可调）
响应中 `xAppid/xAPIKey/xAPISecret` 用 APK 内嵌 RSA 私钥（`sources-key-files/RsaUtils.java`）PKCS1v15 解密。

⚠️ APK 硬编码的旧凭据（34b86978）无 HTTP 权限（11200）、SDK 亦无 license，仅作对照。
⚠️ 凭据轮换风险：服务器可随时换发新凭据，渠道移植时应**运行时调 findConfig 自动获取**而非硬编码。

## 2. HTTP chat 协议（唯一可用通道）

```
POST /v1/chat/completions
Host: spark-api-open.xf-yun.com        (DNS 污染时: 114.118.65.91 + Host头 + SNI)
Authorization: Bearer {apiKey}:{apiSecret}
Content-Type: application/json

{"model":"4.0Ultra","messages":[...],"max_tokens":N,"stream":false}
```

响应：OpenAI 风格 `{"code":0,"message":"Success","choices":[{"message":{"content","role"}}],"usage":{...}}`

### 错误码表（实测）
| 错误 | 触发 | 形态 |
|---|---|---|
| 11200 AppIdNoAuthError | 旧凭据走 HTTP | 200 body code≠0 |
| 10003 ClientMsgError | prompt >150k token | **HTTP 500** + OpenAI error body |
| AppIdNoEnoughLicenceError (tti) | SDK websocket 生图 | SDK 回调 |
| AppIdNoAuthError | SDK websocket chat | SDK 回调 |
| 10013/14/19/21 | 内容审核（app 侧提示语，HTTP 侧未复现） | SDK 回调 |

## 3. 实测性能包线（2026-10-27）

| 指标 | 值 |
|---|---|
| 上下文极限 | **149,628 prompt token OK / 150k → 500**（标称 128k，溢出 17%） |
| 中文分词率 | 0.75 token/字 |
| 串行延迟 | median 1.5s / max 2.8s |
| 长跑 | 60 连发 0 失败（0.8s 间隔） |
| 并发 | 15/15 成功，无 429 |
| 延迟抖动 | 并发下 p50 波动 8x（上游排队） |

### new-api 渠道建议参数
- model: `4.0Ultra`（唯一）
- 上下文截断：保守 128k token
- 渠道限流：`{"rpm":8,"concurrency":3}`
- 计费：按 usage token（上游返回真实 usage）
- 超时：60s（大上下文最慢 6.6s，留余量）

## 4. 图像生成（封死，勿投入）

- HTTP 无 images 端点（404）
- SDK websocket `LLMFactory.imageGeneration(w,h)` 需 `tti` 付费 license，两套凭据均无
- app 真实用户的生图功能本身就是坏的

## 5. app 账号后端（与 chat 无关，仅 app UI 用）

- 注册 `/webapi/user/register` 匿名可调（isVerification=0 无需验证码）
- 登录 `/webapi/user/login` → JWT token（30 天）
- chat 不走账号体系 — 凭据级鉴权，多账号不增额度
- `saveAiLog` 仅 app 记账；`shiyong` 仅本地 SP 标记
