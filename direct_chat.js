Java.perform(function () {
    function log(t, s) { console.log("[" + t + "] " + s); }
    setTimeout(function () {
        Java.perform(function () {
            try {
                var LLM = Java.use("com.iflytek.sparkchain.core.LLM");
                var LLMConfig = Java.use("com.iflytek.sparkchain.core.LLMConfig");
                var LLMCallbacks = Java.use("com.iflytek.sparkchain.core.LLMCallbacks");
                var JavaString = Java.use("java.lang.String");
                var Domain = null;
                try { Domain = Java.use("com.iflytek.sparkchain.core.LLMConfig$Domain"); } catch (e) {}
                var cb = Java.registerClass({
                    name: "com.nbhook.ChatCB",
                    implements: [LLMCallbacks],
                    methods: {
                        onLLMEvent: function (e, o) {},
                        onLLMResult: function (r, o) {
                            var c = r.getContent();
                            if (c) log("chat-result", JavaString.$new(c).substring(0, 200));
                        },
                        onLLMError: function (e, o) { try { log("chat-error", e.getErrMsg()); } catch (e2) {} }
                    }
                });
                // domain 4.0Ultra: builder().domain(...) or LLM API
                var cfg = LLMConfig.builder();
                try { cfg.domain("4.0Ultra"); log("cfg", "domain set"); } catch (e) { log("cfg-err", "" + e); }
                var llm = LLM.$new(cfg);
                llm.registerLLMCallbacks(cb.$new());
                var rc = llm.arun("你好", "nbctx-002");
                log("chat-arun", "ret=" + rc);
            } catch (e) { log("err", "" + e); }
        });
    }, 5000);
});
