# Nano Banana Trip 2API — APK 逆向报告 + OpenAI 兼容网关

> 目标 APK: `Nano Banana Trip v1.0.2解锁会员.apk`（Telegram 流通 Mod 版）
> 真实包名: `com.nbahuihua.qyyd`（applicationId 被 mod 修改）／原包 `com.haichuang.img`
> 逆向路径: 静态 jadx + 动态 Frida（官方模拟器 D:/android-sdk，API 33 x86_64）

## 一、APK 概况

| 项 | 结论 |
|---|---|
| 加固 | 无壳（仅 `libSignatureKiller.so` 绕签名校验 + 穿山甲广告 SDK） |
| Mod 点 | `isVip()` 硬编码 true（解锁会员） |
| AI 能力 | 全部走 **讯飞星火 SparkChain SDK**（websocket，SDK 专属权益） |
| 账号后端 | `https://www.haichuang-tech.com:8082/webapi`（Retrofit，header `authentication=token`） |
| HTTP 日志 | `HttpInterceptor` 打印全部请求 URL + body（tag `shenzhen`） |

### 能力矩阵（静态还原）

- 聊天/写作 → `LLMFactory` domain `4.0Ultra`
- AI 绘图 → `LLMFactory.imageGeneration(w, h)`，尺寸档位 `512x512 / 640x360 / 640x480 / 640x640`
- 图生文 → `imageUnderstanding(maxToken=2048, topK=4, window=5)`

## 二、★ 核心突破：凭据是服务器下发的，且 HTTP 开放平台可用

### 凭据分发链（完全还原）

1. App 启动 → `StartActivity` → `POST /webapi/config/findConfig`（**匿名可调，无需登录**）
2. 请求体携带 `appPackage / agencyChannel=hongfu / appMarket=yingyongbao` 等设备信息
3. 响应返回 **RSA 加密** 的 `xAppid / xAPIKey / xAPISecret`（`xpptAppid/xpptAPISecret` 为 PPT 能力另一套）
4. APK 内 `RsaUtils.java` **内嵌 RSA 私钥**（PKCS1v15），直接解密得到明文凭据
5. `App.initXf()` 用解密后的凭据初始化 SparkChain SDK

### 解密出的真实凭据（2026-10-27 实测有效）

```
xAppid     = 998ba275
xAPIKey    = 146a72cc8e229f213a70a61b4477ad1e
xAPISecret = NDUwODc0OWY2N2RkZTJkYjdmOWJhZDYw  (base64)
             明文: 4508749f67dde2db7f9bad60
```

> ⚠️ APK 内还硬编码了一套旧凭据（`appID=34b86978`），该套在 HTTP 开放平台返回
> `AppIdNoAuthError 11200`（无 HTTP 权限）——**服务器下发这套才有 HTTP 权限**。

### HTTP 开放平台实测（已验证 ✅）

```
POST https://spark-api-open.xf-yun.com/v1/chat/completions
Authorization: Bearer {xAPIKey}:{xAPISecret明文}
{"model":"4.0Ultra","messages":[...]}
→ HTTP 200 {"code":0,"message":"Success","choices":[{"message":{"content":"..."}}],"usage":{...}}
```

- 本机 DNS 把 `spark-api-open.xf-yun.com` 污染到 `127.0.0.1` → 直连真实 IP
  `114.118.65.91`（DoH 解析）+ `Host` 头 + SNI 即可绕过
- 图像生成 HTTP 端点（`/v1/images/generations` 等）**均 404** —— 出图仍需 SDK websocket 通道
  （App 走 `LLMFactory.imageGeneration`，见 `hook_spark.js`）

## 三、交付物

| 文件 | 说明 |
|---|---|
| `demo_server.py` | OpenAI 兼容网关，端口 8915。`upstream`（默认，真实星火 4.0Ultra）/ `mock` 双模式。raw-socket TLS 直连 114.118.65.91 绕 DNS 污染 |
| `demo.html` | 中文 demo 网页 |
| `stress_test.py` | 压测脚本（mock 全场景 + 真实上游温和压测） |
| `hook_spark.js` | Frida 脚本：hook SparkChainConfig / LLM.arun / websocket send / LLMResult.getImage（抓 websocket 出图协议用） |
| `server_creds.json` | findConfig 解密结果快照 |
| `sources-key-files/` | 关键反编译源文件（App.java / RsaUtils.java / HttpInterceptor.java / StartActivity.java / APPConfig.java / DoAiImgViewModel.java） |

### 运行

```bash
python demo_server.py            # upstream 模式, http://127.0.0.1:8915
curl http://127.0.0.1:8915/health
curl -X POST http://127.0.0.1:8915/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"你好"}]}'
```

## 四、压测结果（2026-10-27 实测）

### mock 模式 9 场景 9/9 PASS

串行 median 1.4ms · 20 并发 × 100 请求 100/100（wall 1.09s）· 边界值（空 messages/prompt）全 400

### 真实上游（星火 4.0Ultra）

| 场景 | 结果 |
|---|---|
| 串行 ×5 | 全成功，median **1.7s** |
| 并发 5×1 | 5/5 成功，wall 5.0s，**无 429** |
| 真实 usage | prompt/completion/total_tokens 上游真实统计 |

## 五、动态环境与坑位

| 坑 | 解法 |
|---|---|
| mod 改了 applicationId | `adb install` 后包名是 `com.nbahuihua.qyyd`，frida spawn 用它；activity 仍是 `com.haichuang.img.*` |
| 模拟器有干扰 app 抢前台 | `com.blackhole.hd1763494114` 会顶掉目标 app → `pm uninstall` |
| StartActivity 卡死 | 首启有用户协议弹窗；`findConfig` 失败（SSL/网络）不跳转，只 Toast「网络错误」 |
| 模拟器内 TLS 握手被断 | `haichuang-tech.com:8082` 对部分 ClientHello 特征敏感；主机侧 raw socket TLS 可通，直接在主机调 findConfig 即可 |
| Windows GBK 终端乱码 | `python -X utf8` |
| `adb pull` 路径被 Git Bash 改写 | `MSYS_NO_PATHCONV=1` |
| 本机 DNS 污染 xfyun 域名 | 直连 `114.118.65.91` + Host/SNI |
| 改 demo_server 后端口被旧进程占 | 先 `netstat -ano` + `taskkill` 再重启 |

## 六、第三轮：图像生成 websocket 协议 — 终局定案 ❌

### 完整协议链（已还原）

```java
// DoImgViewModel.doImg(msg, w, h)
LLM llm = LLMFactory.imageGeneration(w, h, LLMConfig.builder());
llm.registerLLMCallbacks(new LLMCallbacks() {
    onLLMResult(LLMResult r, Object o) { byte[] img = r.getImage(); /* 存 jpg */ }
    onLLMError(LLMError e, Object o)   { /* getErrMsg() */ }
});
int ret = llm.arun(prompt, contextUuid);  // ret=0 = 提交成功
```

### 两套凭据 × 双通道 实测矩阵

| 凭据 | 通道 | 结果 |
|---|---|---|
| 服务器下发 `998ba275` | HTTP chat | ✅ `code:0 Success`（唯一可用通道） |
| 服务器下发 `998ba275` | SDK websocket chat (4.0Ultra) | ❌ `AppIdNoAuthError` |
| 服务器下发 `998ba275` | SDK websocket image (tti) | ❌ `AppIdNoEnoughLicenceError` |
| APK 硬编码 `34b86978` | SDK websocket image (tti) | ❌ `AppIdNoEnoughLicenceError`（同错误） |
| APK 硬编码 `34b86978` | HTTP chat | ❌ `11200`（第一轮已测） |

**结论**：`tti`（text-to-image）是讯飞**付费订阅的 SDK 专项许可**，该 app 开发者从未购买/已过期 ——
即真实用户的「AI 绘画」功能也必然失败，不存在可提取的图像生成权益。此路封死。

### 动态分析脚本（进程内直调，跳过全部 UI/登录）

| 脚本 | 用途 |
|---|---|
| `bootstrap.js` | 注入凭据 + test token 到 SP → `initXf()` → `goMain()`（绕过 findConfig SSL 阻塞） |
| `direct_img.js` | 进程内直调 `LLMFactory.imageGeneration(512,512).arun(prompt)`，回调存 `/sdcard/nbhook_img.jpg` |
| `direct_chat.js` | 进程内直调 `LLM` domain=4.0Ultra chat |
| `direct_img_old.js` | 用 APK 硬编码旧凭据重 init SDK 再出图（对照实验） |

```bash
frida -U -f com.nbahuihua.qyyd -l hook_spark.js -l bootstrap.js -l direct_img.js --timeout 90
# 或 attach（app 已运行时）: frida -U -n "Nano Banana Trip" -l direct_img.js
```

### 动态环境坑位补充（第三轮）

| 坑 | 解法 |
|---|---|
| 干扰 app `com.blackhole.hd1763494114` 反复复活抢前台 | `pm disable-user --user 0`（uninstall 会被某种机制重装） |
| Dreamerland / RemakeFace 等旧分析 app 自启顶前台 | 全部 `am force-stop` |
| 权限弹窗卡死 spawn（app 无法启动） | `am force-stop com.google.android.permissioncontroller` |
| `adb install` 后包名被神秘力量卸载 | 重装即可（出现过 3 次） |
| frida attach 找不到包名 | 用 display name：`frida -U -n "Nano Banana Trip"` |
| 首启「软件使用提示」弹窗 | tap `540,1504`（同意） |
| 关键 UI 坐标 | AI绘画入口 `361,700` · prompt 框 `554,628` · AI生成 `540,1827` |

## 七、路线图

- [x] 第一轮：静态逆向 + 协议还原 + mock demo + 压测
- [x] 第二轮：模拟器动态运行 + findConfig RSA 解密 + HTTP 真实链路打通（chat 全通）
- [x] 第三轮：图像生成 websocket 协议抓取 — **终局：两套凭据均无 tti 许可，图像功能封死**（详见第六节）
- [ ] 第四轮：移植进 new-api 渠道适配器（仅 chat 能力，type 68+，参照 `relay/channel/kilwa/` 模板）

## License

仅供安全研究与协议学习。
