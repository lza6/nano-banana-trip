// Nano Banana Trip (com.haichuang.img) — SparkChain SDK 协议抓取
// hook 点: SparkChain init 凭据 / websocket 连接 / LLM.arun / 图片回调
Java.perform(function () {
    var LOG = [];
    function log(tag, s) {
        var line = "[" + tag + "] " + s;
        console.log(line);
        LOG.push(line);
    }

    // 1. SparkChainConfig — 凭据与参数
    try {
        var CFG = Java.use("com.iflytek.sparkchain.core.SparkChainConfig");
        ["appID", "apiKey", "apiSecret", "domain", "url", "workDir"].forEach(function (m) {
            try {
                CFG[m].overload('java.lang.String').implementation = function (v) {
                    log("config", m + " = " + v);
                    return this[m](v);
                };
            } catch (e2) {}
        });
    } catch (e) { log("err", "config hook: " + e); }

    // 2. LLM.arun — 输入 prompt
    try {
        var LLM = Java.use("com.iflytek.sparkchain.core.LLM");
        LLM.arun.overload('java.lang.String', 'java.lang.Object').implementation = function (msg, ctx) {
            log("arun", "prompt=" + msg + " ctx=" + ctx);
            return this.arun(msg, ctx);
        };
    } catch (e) { log("err", "arun hook: " + e); }

    // 3. SDK 网络 — 星火 SDK 底层连接（hook WebSocket/OkHttp/URL）
    try {
        var URL = Java.use("java.net.URL");
        URL.$init.overload('java.lang.String').implementation = function (u) {
            if (u.indexOf("xfyun") !== -1 || u.indexOf("spark") !== -1 || u.indexOf("ws") === 0)
                log("url", u);
            return this.$init(u);
        };
    } catch (e) {}

    // OkHttp 请求全抓 (SDK 内部可能走 okhttp)
    try {
        var R = Java.use("okhttp3.Request$Builder");
        R.url.overload('java.lang.String').implementation = function (u) {
            log("okhttp-url", u);
            return this.url(u);
        };
    } catch (e) {}

    // 4. SSL 明文出口兜底 — 抓 TLS write
    try {
        var SSL = Java.use("com.android.org.conscrypt.ConscryptFileDescriptorSocket");
        SSL.getOutputStream.implementation = function () {
            log("ssl", "socket=" + this.getInetAddress().getHostName() + ":" + this.getPort());
            return this.getOutputStream();
        };
    } catch (e) {}

    // 5. 响应回调 — LLMResult 图片
    try {
        var RES = Java.use("com.iflytek.sparkchain.core.LLMResult");
        RES.getImage.implementation = function () {
            var img = this.getImage();
            if (img) log("result", "IMAGE bytes=" + img.length);
            return img;
        };
    } catch (e) {}

    // 6. 错误回调
    try {
        var ERR = Java.use("com.iflytek.sparkchain.core.LLMError");
        ERR.getErrMsg.implementation = function () {
            var m = this.getErrMsg();
            log("llm-error", m);
            return m;
        };
    } catch (e) {}

    // 7. websocket 底层消息（SDK 可用 okhttp ws）
    try {
        var WS = Java.use("okhttp3.internal.ws.RealWebSocket");
        WS.send.overload('java.lang.String').implementation = function (s) {
            log("ws-send", s.substring(0, Math.min(s.length, 2000)));
            return this.send(s);
        };
        WS.send.overload('okio.ByteString').implementation = function (b) {
            log("ws-send-bin", "len=" + b.size());
            return this.send(b);
        };
    } catch (e) { log("err", "ws hook: " + e); }

    log("init", "hooks installed");
});
