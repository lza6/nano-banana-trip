#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Nano Banana Trip (com.haichuang.img v1.0.2 mod) — 逆向还原 demo server
协议真相:
  - AI 能力全部走讯飞星火 SparkChain SDK (websocket, SDK 专属权益)
    appID=34b86978 apiKey=90a7262fadfbe6ee2de9d3e3c3188d69
    apiSecret(base64)=NWVlNDIwMzIxN2VkZmU4YTVlMGQ1M2Vi
  - 聊天/写作 domain=4.0Ultra, 绘图 LLMFactory.imageGeneration(w,h), 图生文 imageUnderstanding
  - HTTP 开放平台 (spark-api-open.xf-yun.com) 实测: 认证通过但 AppIdNoAuthError 11200 (无HTTP权限)
  - 账号/订单后端 https://www.haichuang-tech.com:8082/webapi (token header: authentication)
本 server: OpenAI 兼容网关 demo, 模式 = mock (SDK 权益不可 HTTP 复现时) / upstream (有权限时直连星火)
端口 8915
"""
import base64, json, os, socket, ssl, threading, time, uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

PORT = 8915
MODE = os.environ.get("NB_MODE", "upstream")  # mock | upstream
XF_HTTP = "https://spark-api-open.xf-yun.com/v1/chat/completions"  # DNS异常时用 XF_HOST
# 服务器下发凭据 (findConfig RSA 解密, 已实测 code:0 Success)
XF_AUTH = "Bearer 146a72cc8e229f213a70a61b4477ad1e:NDUwODc0OWY2N2RkZTJkYjdmOWJhZDYw"
XF_HOST = "114.118.65.91"  # spark-api-open.xf-yun.com 真实IP (本机DNS被污染时直连)
START = time.time()
STATS = {"requests": 0, "chat": 0, "images": 0, "errors": 0, "upstream_errors": 0}
LOCK = threading.Lock()

def bump(k):
    with LOCK:
        STATS[k] += 1

def xf_chat(messages, max_tokens=1024, stream=False):
    """直连星火 HTTP 开放平台 (服务器下发凭据, 已实测 Success). DNS污染时直连IP"""
    body = json.dumps({"model": "4.0Ultra", "messages": messages,
                       "max_tokens": max_tokens, "stream": False}).encode()
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    try:
        raw = socket.create_connection((XF_HOST, 443), timeout=60)
        conn = ctx.wrap_socket(raw, server_hostname="spark-api-open.xf-yun.com")
        reqhdr = ("POST /v1/chat/completions HTTP/1.1\r\n"
                  "Host: spark-api-open.xf-yun.com\r\n"
                  "Authorization: " + XF_AUTH + "\r\n"
                  "Content-Type: application/json\r\n"
                  "Connection: close\r\n"
                  "Content-Length: " + str(len(body)) + "\r\n\r\n")
        conn.sendall(reqhdr.encode() + body)
        resp = b""
        while True:
            d = conn.recv(65536)
            if not d:
                break
            resp += d
        conn.close()
        txt = resp.decode(errors="replace")
        i = txt.find('{"')
        return json.loads(txt[i:]) if i >= 0 else None
    except Exception as e:
        bump("upstream_errors")
        print("[upstream-error]", e)
        return None

MOCK_REPLY = ("[mock·4.0Ultra] 这是 Nano Banana Trip 逆向 demo 的模拟回复。"
              "真实链路: 讯飞星火 SparkChain SDK (websocket, SDK专属权益), "
              "HTTP 开放平台该 appID 无权限 (AppIdNoAuthError 11200, 已实测)。")

MOCK_IMG_B64 = base64.b64encode(
    b"\x89PNG\r\n\x1a\n" + b"mock-nano-banana-image-placeholder" * 8).decode()

class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *a):
        pass

    def _send(self, code, obj, ctype="application/json"):
        data = json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", ctype + "; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    # ---- OpenAI 兼容: /v1/chat/completions ----
    def do_POST(self):
        bump("requests")
        n = int(self.headers.get("Content-Length") or 0)
        try:
            body = json.loads(self.rfile.read(n) or b"{}")
        except Exception:
            bump("errors")
            return self._send(400, {"error": {"message": "invalid json"}})

        if self.path == "/v1/chat/completions":
            bump("chat")
            msgs = body.get("messages") or []
            if not msgs or not isinstance(msgs, list):
                bump("errors")
                return self._send(400, {"error": {"message": "messages required"}})
            if MODE == "upstream":
                up = xf_chat(msgs, body.get("max_tokens", 1024))
                if up and "choices" in up:
                    return self._send(200, {
                        "id": "chatcmpl-" + uuid.uuid4().hex[:12],
                        "object": "chat.completion", "created": int(time.time()),
                        "model": body.get("model", "4.0Ultra"),
                        "choices": [{"index": 0,
                                     "message": up["choices"][0]["message"],
                                     "finish_reason": "stop"}],
                        "usage": up.get("usage", {})})
            reply = MOCK_REPLY
            content = msgs[-1].get("content", "") if msgs else ""
            if isinstance(content, str) and content:
                reply = f"[mock·4.0Ultra] 收到: {content[:80]}。{MOCK_REPLY[14:]}"
            return self._send(200, {
                "id": "chatcmpl-" + uuid.uuid4().hex[:12],
                "object": "chat.completion", "created": int(time.time()),
                "model": body.get("model", "4.0Ultra"),
                "choices": [{"index": 0,
                             "message": {"role": "assistant", "content": reply},
                             "finish_reason": "stop"}],
                "usage": {"prompt_tokens": 10, "completion_tokens": 20, "total_tokens": 30},
                "x-source": "nano-banana-trip-demo/mock"})

        if self.path == "/v1/images/generations":
            bump("images")
            prompt = body.get("prompt")
            if not prompt:
                bump("errors")
                return self._send(400, {"error": {"message": "prompt required"}})
            # 还原 App: 尺寸档位 512x512 / 640x360 / 640x480 / 640x640
            size = body.get("size", "512x512")
            return self._send(200, {
                "created": int(time.time()),
                "data": [{"b64_json": MOCK_IMG_B64,
                          "revised_prompt": prompt[:100]}],
                "x-source": "nano-banana-trip-demo/mock",
                "x-upstream": "iflytek SparkChain LLMFactory.imageGeneration (SDK-only)"})

        bump("errors")
        self._send(404, {"error": {"message": "not found: " + self.path}})

    # ---- 运维端点 ----
    def do_GET(self):
        bump("requests")
        if self.path in ("/health", "/"):
            return self._send(200, {"status": "ok" if MODE == "upstream" else "ok(mock)",
                                    "mode": MODE, "uptime_s": round(time.time() - START, 1)})
        if self.path == "/stats":
            with LOCK:
                s = dict(STATS)
            return self._send(200, {**s, "uptime_s": round(time.time() - START, 1)})
        if self.path == "/protocol":
            return self._send(200, {
                "app": "Nano Banana Trip v1.0.2 (com.haichuang.img, mod: isVip()=true)",
                "ai_upstream": "iFlytek SparkChain SDK",
                "credentials": {"appID": "34b86978",
                                 "apiKey": "90a7262fadfbe6ee2de9d3e3c3188d69",
                                 "apiSecret_b64": "NWVlNDIwMzIxN2VkZmU4YTVlMGQ1M2Vi"},
                "models": {"chat": "4.0Ultra", "img": "LLMFactory.imageGeneration",
                            "vision": "imageUnderstanding maxToken=2048 topK=4 windowMemory=5"},
                "sizes": ["512x512", "640x360", "640x480", "640x640"],
                "account_backend": "https://www.haichuang-tech.com:8082/webapi",
                "http_api_test": "AppIdNoAuthError 11200 — SDK 专属权益, HTTP 不可复现"})
        self._send(404, {"error": "not found"})

if __name__ == "__main__":
    print(f"Nano Banana Trip demo server @ http://127.0.0.1:{PORT}  mode={MODE}")
    ThreadingHTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
