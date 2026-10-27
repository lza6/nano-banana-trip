#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Nano Banana Trip (星火4.0Ultra) 长跑+压测终局
1. 极限上下文长度探测 (token上限, 超限错误形态)
2. 账号额度长跑 (连续请求观察第几次失败/封禁)
3. 并发压测 (并发5/10/15, 429与限流形态)
4. 多账号轮换验证 (新注册号额度是否独立)
输出: longrun_result.json
"""
import json, ssl, socket, time, threading, statistics, random, string, sys
sys.stdout.reconfigure(encoding="utf-8")

AUTH = "Bearer 146a72cc8e229f213a70a61b4477ad1e:NDUwODc0OWY2N2RkZTJkYjdmOWJhZDYw"
HOST, IP, PORT = "spark-api-open.xf-yun.com", "114.118.65.91", 443
RESULTS = {"ctx": [], "longrun": [], "concurrency": {}, "accounts": []}

def chat(messages, max_tokens=100, timeout=90):
    body = json.dumps({"model": "4.0Ultra", "messages": messages, "max_tokens": max_tokens}).encode()
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT); ctx.check_hostname = False; ctx.verify_mode = ssl.CERT_NONE
    s = ctx.wrap_socket(socket.create_connection((IP, PORT), timeout=timeout), server_hostname=HOST)
    req = (f"POST /v1/chat/completions HTTP/1.1\r\nHost: {HOST}\r\n"
           f"Authorization: {AUTH}\r\nContent-Type: application/json\r\n"
           f"Connection: close\r\nContent-Length: {len(body)}\r\n\r\n").encode() + body
    s.sendall(req); resp = b""
    while True:
        d = s.recv(65536)
        if not d: break
        resp += d
    s.close()
    t = resp.decode(errors="replace")
    st = t.split("\r\n")[0]
    i = t.find('{"')
    if i < 0: return {"http": st, "raw": t[:200]}
    depth = 0; end = None
    for j in range(i, len(t)):
        if t[j] == '{': depth += 1
        elif t[j] == '}':
            depth -= 1
            if depth == 0: end = j + 1; break
    j = json.loads(t[i:end])
    j["_http"] = st
    return j

def summarize(tag, r):
    if "code" in r and r.get("code") == 0:
        u = r.get("usage", {})
        c = r["choices"][0]["message"]["content"]
        return f"{tag}: OK usage={u.get('total_tokens')}pt={u.get('prompt_tokens')}/{u.get('completion_tokens')} out={c[:30]!r}"
    return f"{tag}: FAIL http={r.get('_http','?')} body={json.dumps(r, ensure_ascii=False)[:180]}"

# ============ 1. 极限上下文 ============
print("=" * 20, "1. 上下文长度极限", "=" * 20)
for approx_chars in [100, 1000, 5000, 10000, 20000, 32000, 60000, 100000]:
    # 中文约 1.5~2 token/char (星火分词), 逐步试探
    text = "科技发展史概览。" * (approx_chars // 8)  # 每段8字
    msgs = [{"role": "user", "content": text + "\n请只回答:收到"}]
    t0 = time.time()
    try:
        r = chat(msgs, max_tokens=20, timeout=120)
        ok = r.get("code") == 0
        u = r.get("usage", {})
        RESULTS["ctx"].append({"chars": len(text), "ok": ok, "pt": u.get("prompt_tokens"),
                               "total": u.get("total_tokens"), "s": round(time.time() - t0, 1),
                               "err": r.get("code"), "msg": str(r.get("message"))[:60]})
        print(summarize(f"ctx~{len(text)}ch", r), f"lat={time.time()-t0:.1f}s")
    except Exception as e:
        RESULTS["ctx"].append({"chars": len(text), "ok": False, "err": "EXC", "msg": str(e)[:80]})
        print(f"ctx~{len(text)}ch: EXC {str(e)[:80]}")
    time.sleep(1.5)

# ============ 2. 长跑 (账号额度) ============
print("=" * 20, "2. 长跑 60 连发", "=" * 20)
fails, okn = 0, 0
for i in range(60):
    t0 = time.time()
    try:
        r = chat([{"role": "user", "content": f"数到{i},只回答数字"}], max_tokens=30)
        ok = r.get("code") == 0
        if ok: okn += 1
        else: fails += 1
        u = r.get("usage", {})
        RESULTS["longrun"].append({"i": i, "ok": ok, "code": r.get("code"),
                                   "msg": str(r.get("message"))[:60], "tt": u.get("total_tokens"),
                                   "s": round(time.time() - t0, 1)})
        print(f"#{i}: {'OK' if ok else 'FAIL ' + str(r.get('code')) + ' ' + str(r.get('message'))[:50]} {time.time()-t0:.1f}s")
    except Exception as e:
        fails += 1
        RESULTS["longrun"].append({"i": i, "ok": False, "err": str(e)[:80], "s": round(time.time() - t0, 1)})
        print(f"#{i}: EXC {str(e)[:80]}")
    time.sleep(0.8)
print(f"长跑: {okn}/60 ok, {fails} fail")

# ============ 3. 并发压测 ============
print("=" * 20, "3. 并发压测", "=" * 20)
for conc in [5, 10, 15]:
    results = []
    lock = threading.Lock()
    def w(k):
        t0 = time.time()
        try:
            r = chat([{"role": "user", "content": f"写一句关于秋天的话 {k}"}], max_tokens=60, timeout=120)
            ok = r.get("code") == 0
            lat = time.time() - t0
        except Exception as e:
            ok, lat = False, time.time() - t0
            r = {"message": str(e)[:60]}
        with lock: results.append((ok, lat, r))
    ts = [threading.Thread(target=w, args=(k,)) for k in range(conc)]
    t0 = time.time()
    [t.start() for t in ts]; [t.join() for t in ts]
    wall = time.time() - t0
    oks = [x for x in results if x[0]]
    lats = sorted(x[1] for x in results)
    fails_detail = {}
    for _, _, r in results:
        if not r.get("code", 0) == 0 if "code" in r else True:
            key = f"{r.get('code')}:{str(r.get('message'))[:30]}"
            fails_detail[key] = fails_detail.get(key, 0) + 1
    RESULTS["concurrency"][conc] = {"ok": len(oks), "total": conc, "wall": round(wall, 1),
                                    "p50": round(statistics.median(lats), 1),
                                    "max": round(max(lats), 1), "fails": fails_detail}
    print(f"并发{conc}: {len(oks)}/{conc} ok wall={wall:.1f}s p50={statistics.median(lats):.1f}s max={max(lats):.1f}s")
    if fails_detail: print(f"  失败详情: {fails_detail}")
    time.sleep(3)

# ============ 4. 多账号独立额度验证 ============
print("=" * 20, "4. 多账号注册", "=" * 20)
def api_post(path, body, auth=""):
    body = json.dumps(body).encode()
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT); ctx.check_hostname = False; ctx.verify_mode = ssl.CERT_NONE
    s = ctx.wrap_socket(socket.create_connection(("www.haichuang-tech.com", 8082), timeout=20),
                        server_hostname="www.haichuang-tech.com")
    req = (f"POST /webapi{path} HTTP/1.1\r\nHost: www.haichuang-tech.com:8082\r\n"
           f"Content-Type: application/json\r\nauthentication: {auth}\r\nConnection: close\r\n"
           f"Content-Length: {len(body)}\r\n\r\n").encode() + body
    s.sendall(req); resp = b""
    while True:
        d = s.recv(65536)
        if not d: break
        resp += d
    s.close()
    t = resp.decode(errors="replace"); i = t.find('{"')
    depth = 0; end = None
    for j in range(i, len(t)):
        if t[j] == '{': depth += 1
        elif t[j] == '}':
            depth -= 1
            if depth == 0: end = j + 1; break
    return json.loads(t[i:end]) if end else {"raw": t[:200]}

base = {"agencyChannel": "hongfu", "appMarket": "yingyongbao", "appName": "Nano Banana Trip",
        "appPackage": "com.nbahuihua.qyyd", "appVersion": "2", "appVersionName": "1.0.2",
        "application": "aichat", "deviceBrand": "google", "deviceManufacturer": "Google",
        "deviceName": "sdk_gphone_x86_64", "devicePlatform": "ANDROID"}
accs = []
for k in range(3):
    user = "nb" + "".join(random.choices(string.ascii_lowercase + string.digits, k=8))
    pw = "Nb" + "".join(random.choices(string.ascii_letters + string.digits, k=10))
    try:
        r = api_post("/user/register", dict(base, **{"username": user, "password": pw, "email": user + "@test.com",
                                                     "nickName": user, "phone": "", "stauts": 0,
                                                     "isVerification": "0", "verifyCode": ""}))
        ok = r.get("success")
        accs.append({"user": user, "pw": pw, "ok": ok, "id": r.get("data", {}).get("user", {}).get("id")})
        print(f"账号{k} {user}: {'OK' if ok else json.dumps(r, ensure_ascii=False)[:120]}")
    except Exception as e:
        accs.append({"user": user, "ok": False, "err": str(e)[:60]})
        print(f"账号{k} {user}: EXC {str(e)[:60]}")
    time.sleep(1)
RESULTS["accounts"] = accs

json.dump(RESULTS, open(r"D:/apk-analysis/nano-banana-trip/longrun_result.json", "w", encoding="utf-8"),
          ensure_ascii=False, indent=1)
print("\n=== 完成, 结果已存 longrun_result.json ===")
