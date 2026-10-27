package com.haichuang.img;

import android.content.Context;
import android.text.TextUtils;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import com.alibaba.android.arouter.launcher.ARouter;
import com.haichuang.img.adcontroller.AdController;
import com.haichuang.img.utils.AppInfoUtils;
import com.haichuang.img.utils.Constant;
import com.haichuang.img.utils.LogUtils;
import com.haichuang.img.utils.SPUtils;
import com.haichuang.img.utils.ToastUtils;
import com.iflytek.sparkchain.core.SparkChain;
import com.iflytek.sparkchain.core.SparkChainConfig;
import com.umeng.commonsdk.UMConfigure;
import java.util.Locale;
import java.util.UUID;

/* JADX INFO: loaded from: classes2.dex */
public class App extends MultiDexApplication {
    public static final String RANDOM_UUID = UUID.randomUUID().toString().toLowerCase(Locale.ROOT);
    private static App app;
    private static volatile Context context;

    public static Context getContext() {
        return context;
    }

    public static App getIntance() {
        return app;
    }

    @Override // androidx.multidex.MultiDexApplication, android.content.ContextWrapper
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        app = this;
        if (context == null) {
            context = this;
        }
        AppInfoUtils.init(context);
        ARouter.init(this);
        ToastUtils.init(context);
        if (((Boolean) SPUtils.getParam(Constant.FIRST_ENTER, true)).booleanValue()) {
            return;
        }
        initThreeSDK();
    }

    public void initThreeSDK() {
        UMConfigure.preInit(this, AppInfoUtils.metadata("UMENG_KEY"), AppInfoUtils.metadata("UMENG_CHANNEL"));
        UMConfigure.init(this, AppInfoUtils.metadata("UMENG_KEY"), AppInfoUtils.metadata("UMENG_CHANNEL"), 1, "");
        if (APPConfig.isShowAd()) {
            String str = (String) SPUtils.getParam(Constant.AD_APP_ID, "");
            if (TextUtils.isEmpty(str)) {
                return;
            }
            AdController.getInstance().init(this, str);
        }
    }

    public void initXf() {
        AppExecutors.runNetworkIO(new Runnable() { // from class: com.haichuang.img.App$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m465lambda$initXf$0$comhaichuangimgApp();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$initXf$0$com-haichuang-img-App, reason: not valid java name */
    /* synthetic */ void m465lambda$initXf$0$comhaichuangimgApp() {
        LogUtils.d("初始化-》" + SparkChain.getInst().init(this, SparkChainConfig.builder().appID((String) SPUtils.getParam(Constant.xfptWAppid, "34b86978")).apiKey((String) SPUtils.getParam(Constant.xfptWAPIKey, "90a7262fadfbe6ee2de9d3e3c3188d69")).apiSecret((String) SPUtils.getParam(Constant.xfptWAPISecret, "NWVlNDIwMzIxN2VkZmU4YTVlMGQ1M2Vi"))));
    }
}
