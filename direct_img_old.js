Java.perform(function () {
    function log(t, s) { console.log("[" + t + "] " + s); }
    setTimeout(function () {
        Java.perform(function () {
            try {
                var SparkChain = Java.use("com.iflytek.sparkchain.core.SparkChain");
                var SparkChainConfig = Java.use("com.iflytek.sparkchain.core.SparkChainConfig");
                var App = Java.use("com.haichuang.img.App");
                // 用 APK 硬编码旧凭据重新 init SDK
                var cfg = SparkChainConfig.builder()
                    .appID("34b86978")
                    .apiKey("90a7262fadfbe6ee2de9d3e3c3188d69")
                    .apiSecret("NWVlNDIwMzIxN2VkZmU4YTVlMGQ1M2Vi");
                log("init", "ret=" + SparkChain.getInst().init(App.getIntance(), cfg));
                var LLMFactory = Java.use("com.iflytek.sparkchain.core.LLMFactory");
                var LLMConfig = Java.use("com.iflytek.sparkchain.core.LLMConfig");
                var LLMCallbacks = Java.use("com.iflytek.sparkchain.core.LLMCallbacks");
                var cb = Java.registerClass({
                    name: "com.nbhook.ImgCB2",
                    implements: [LLMCallbacks],
                    methods: {
                        onLLMEvent: function (e, o) {},
                        onLLMResult: function (r, o) {
                            var img = r.getImage();
                            if (img !== null) {
                                log("result", "IMAGE bytes=" + img.length);
                                var fos = Java.use("java.io.FileOutputStream").$new("/sdcard/nbhook_img2.jpg");
                                fos.write(img); fos.close();
                                log("result", "saved /sdcard/nbhook_img2.jpg");
                            } else { log("result", "no image"); }
                        },
                        onLLMError: function (e, o) { try { log("llm-error", e.getErrMsg()); } catch (e2) {} }
                    }
                });
                var llm = LLMFactory.imageGeneration(512, 512, LLMConfig.builder());
                llm.registerLLMCallbacks(cb.$new());
                log("arun", "ret=" + llm.arun("a cute corgi astronaut", "nbctx-003"));
            } catch (e) { log("err", "" + e); }
        });
    }, 5000);
});
