// Nano Banana Trip — bootstrap: 填凭据 + initXf + goMain (绕过 findConfig SSL 阻塞)
Java.perform(function () {
    function log(t, s) { console.log("[" + t + "] " + s); }

    function run() {
        try {
            var SPUtils = Java.use("com.haichuang.img.utils.SPUtils");
            var Constant = Java.use("com.haichuang.img.utils.Constant");
            var App = Java.use("com.haichuang.img.App");
            SPUtils.setParam(Constant.FIRST_ENTER.value, Java.use("java.lang.Boolean").FALSE.value);
            SPUtils.setParam(Constant.xfptWAppid.value, "998ba275");
            SPUtils.setParam(Constant.xfptWAPIKey.value, "146a72cc8e229f213a70a61b4477ad1e");
            SPUtils.setParam(Constant.xfptWAPISecret.value, "NDUwODc0OWY2N2RkZTJkYjdmOWJhZDYw");
            // test account + token (host-registered, JWT valid 30d)
            SPUtils.setParam(Constant.USERNAME.value, "nbmi8vp4pv");
            SPUtils.setParam(Constant.USERPASSWORD.value, "Nb7i90rpo2TM");
            SPUtils.setParam(Constant.TOKEN.value, "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MjExNDg4NTE5MTYxMjAwMjMwNCwidXNlcm5hbWUiOiJuYm1pOHZwNHB2IiwiYXBwUGFja2FnZSI6ImNvbS5uYmFodWlodWEucXl5ZCIsImFwcGxpY2F0aW9uIjoiYWljaGF0IiwiZXhwIjoxNzkzMDcwMDk3fQ.kS5QCFC5rT_wdrQ4RT0iy6E-KcXC0-AZAf8YacZOdBxHWWhmJFF0VFwG7Pizuoz93Z3SRJ6khrY3L46rtvubapN9cM2EG-_R53TTRGCy_i41ot1OeOjlifyiOBjWPc_i21EVcEi05sSNOSBwirIyNwaRxoba7v-pU8CaZHEERNw");
            log("boot", "SP credentials set");
            var inst = App.getIntance();
            inst.initXf();
            log("boot", "initXf called");
            Java.scheduleOnMainThread(function () {
                try {
                    Java.use("com.haichuang.img.utils.JumpUtils").goMain();
                    log("boot", "goMain navigated");
                } catch (e) { log("err", "goMain: " + e); }
            });
        } catch (e) { log("err", "run: " + e); }
    }

    // StartActivity onCreate 后 SP 已可用; 延迟 3s 执行
    setTimeout(run, 3000);
});
