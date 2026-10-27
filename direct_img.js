// Nano Banana — 进程内直调 SparkChain imageGeneration (绕过全部 UI/登录流程)
Java.perform(function () {
    function log(t, s) { console.log("[" + t + "] " + s); }

    setTimeout(function () {
        Java.perform(function () {
            try {
                var App = Java.use("com.haichuang.img.App");
                var appInst = App.getIntance();
                appInst.initXf();  // 确保用注入的凭据初始化

                var LLMFactory = Java.use("com.iflytek.sparkchain.core.LLMFactory");
                var LLMConfig = Java.use("com.iflytek.sparkchain.core.LLMConfig");
                var LLMCallbacks = Java.use("com.iflytek.sparkchain.core.LLMCallbacks");
                var JavaString = Java.use("java.lang.String");

                var cb = Java.registerClass({
                    name: "com.nbhook.ImgCB",
                    implements: [LLMCallbacks],
                    methods: {
                        onLLMEvent: function (e, o) { log("evt", "" + e); },
                        onLLMResult: function (r, o) {
                            try {
                                var img = r.getImage();
                                if (img !== null) {
                                    log("result", "IMAGE bytes=" + img.length);
                                    // 存文件
                                    var fos = Java.use("java.io.FileOutputStream").$new("/sdcard/nbhook_img.jpg");
                                    fos.write(img);
                                    fos.close();
                                    log("result", "saved /sdcard/nbhook_img.jpg");
                                } else {
                                    var content = r.getContent();
                                    log("result", "content=" + (content ? JavaString.$new(content).substring(0, Math.min(300, content.length)) : "null"));
                                }
                            } catch (e) { log("err", "onResult: " + e); }
                        },
                        onLLMError: function (e, o) {
                            try { log("llm-error", e.getErrMsg()); } catch (e2) { log("err", "" + e2); }
                        }
                    }
                });

                var llm = LLMFactory.imageGeneration(512, 512, LLMConfig.builder());
                llm.registerLLMCallbacks(cb.$new());
                var rc = llm.arun("a cute corgi astronaut in space, digital art", "nbctx-001");
                log("arun", "ret=" + rc);
            } catch (e) { log("err", "setup: " + e); }
        });
    }, 8000);
});
