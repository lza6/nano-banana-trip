#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Nano Banana Trip demo server 压测 — 基线/边界/并发 三场景"""
import json, statistics, time, urllib.request, urllib.error
from concurrent.futures import ThreadPoolExecutor

BASE = "http://127.0.0.1:8915"
results = []

def req(method, path, body=None):
    data = json.dumps(body).encode() if body else None
    r = urllib.request.Request(BASE + path, data=data, method=method,
        headers={"Content-Type": "application/json"})
    t0 = time.time()
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            return resp.status, json.loads(resp.read()), time.time() - t0
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read() or b"{}"), time.time() - t0

def record(name, ok, extra=""):
    results.append((name, ok, extra))
    print(f"[{'PASS' if ok else 'FAIL'}] {name} {extra}")

# 场景1: 基线
s, b, t = req("GET", "/health"); record("health", s == 200, f"{t*1000:.1f}ms mode={b.get('mode')}")
lat = []
for i in range(10):
    s, b, t = req("POST", "/v1/chat/completions",
                  {"model": "4.0Ultra", "messages": [{"role": "user", "content": f"测试{i}"}]})
    lat.append(t); ok1 = (s == 200 and b["choices"][0]["message"]["content"])
record("chat 串行x10", ok1, f"median={statistics.median(lat)*1000:.1f}ms max={max(lat)*1000:.1f}ms")

# 场景2: 边界值
s, b, t = req("POST", "/v1/chat/completions", {"messages": []}); record("chat 空 messages -> 400", s == 400)
s, b, t = req("POST", "/v1/chat/completions", b"xx" and {"x": 1}); record("chat 无 messages -> 400", s == 400)
s, b, t = req("POST", "/v1/images/generations", {}); record("images 无 prompt -> 400", s == 400)
s, b, t = req("POST", "/v1/images/generations", {"prompt": "banana", "size": "640x480"})
record("images 640x480", s == 200 and b["data"][0]["b64_json"], f"{t*1000:.1f}ms")
s, b, t = req("GET", "/stats"); record("stats 一致性", s == 200 and b["chat"] >= 12)
s, b, t = req("GET", "/protocol"); record("protocol 端点", s == 200 and "SparkChain" in b["ai_upstream"])

# 场景3: 并发
def one(i):
    return req("POST", "/v1/chat/completions",
               {"messages": [{"role": "user", "content": f"并发{i}"}]})[0]
t0 = time.time()
with ThreadPoolExecutor(20) as ex:
    codes = list(ex.map(one, range(100)))
okc = sum(1 for c in codes if c == 200)
record("并发 20c x100", okc == 100, f"wall={time.time()-t0:.2f}s {okc}/100 ok")

fails = [r for r in results if not r[1]]
print(f"\n{'='*40}\n{len(results)-len(fails)}/{len(results)} PASS" + (" — ALL PASS ✔" if not fails else f" — {len(fails)} FAIL"))
