package com.haichuang.img.ui.activity.login;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import com.haichuang.img.APPConfig;
import com.haichuang.img.App;
import com.haichuang.img.R;
import com.haichuang.img.adcontroller.AdController;
import com.haichuang.img.adcontroller.SplashLoadListener;
import com.haichuang.img.base.BaseActivity;
import com.haichuang.img.databinding.ActivityStartBinding;
import com.haichuang.img.net.data.DataResponse;
import com.haichuang.img.net.res.ConfigRes;
import com.haichuang.img.ui.viewmodel.LoginViewModel;
import com.haichuang.img.utils.Constant;
import com.haichuang.img.utils.JumpUtils;
import com.haichuang.img.utils.RsaUtils;
import com.haichuang.img.utils.SPUtils;
import com.haichuang.img.utils.ToastUtils;
import com.kongzue.dialog.v2.CustomDialog;
import java.util.Map;

/* JADX INFO: loaded from: classes2.dex */
public class StartActivity extends BaseActivity<LoginViewModel, ActivityStartBinding> {
    private CustomDialog customDialog;
    private boolean isGoSetting;
    private String[] permissions;

    @Override // com.haichuang.img.base.BaseActivity
    protected void initData() {
    }

    @Override // com.haichuang.img.base.BaseActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override // com.haichuang.img.base.BaseActivity
    protected void initView(Bundle savedInstanceState) {
        this.permissions = new String[]{"android.permission.ACCESS_FINE_LOCATION"};
        if (((Boolean) SPUtils.getParam(Constant.FIRST_ENTER, true)).booleanValue()) {
            showUserXieyiDialog();
        } else {
            initPermissions(this.permissions);
        }
        this.isGoSetting = false;
    }

    private void showUserXieyiDialog() {
        CustomDialog customDialogShow = CustomDialog.show(this, R.layout.dialog_user_xieyi, new CustomDialog.BindView() { // from class: com.haichuang.img.ui.activity.login.StartActivity$$ExternalSyntheticLambda4
            @Override // com.kongzue.dialog.v2.CustomDialog.BindView
            public final void onBind(CustomDialog customDialog, View view) {
                this.f$0.m510x462b8510(customDialog, view);
            }
        });
        this.customDialog = customDialogShow;
        customDialogShow.setCanCancel(false);
    }

    /* JADX INFO: renamed from: lambda$showUserXieyiDialog$2$com-haichuang-img-ui-activity-login-StartActivity, reason: not valid java name */
    /* synthetic */ void m510x462b8510(CustomDialog customDialog, View view) {
        TextView textView = (TextView) view.findViewById(R.id.tv_prompt);
        String string = getResources().getString(R.string.app_user_xieyi);
        textView.setText(string);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        string.length();
        spannableStringBuilder.append((CharSequence) string);
        ClickableSpan clickableSpan = new ClickableSpan() { // from class: com.haichuang.img.ui.activity.login.StartActivity.1
            @Override // android.text.style.ClickableSpan
            public void onClick(View widget) {
                JumpUtils.goWeb(0, false);
            }
        };
        int iIndexOf = string.indexOf("《用户协议》");
        int i = iIndexOf + 6;
        spannableStringBuilder.setSpan(clickableSpan, iIndexOf, i, 33);
        textView.setText(spannableStringBuilder);
        spannableStringBuilder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.color653636)), iIndexOf, i, 33);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(spannableStringBuilder);
        ClickableSpan clickableSpan2 = new ClickableSpan() { // from class: com.haichuang.img.ui.activity.login.StartActivity.2
            @Override // android.text.style.ClickableSpan
            public void onClick(View widget) {
                JumpUtils.goWeb(1, false);
            }
        };
        int iIndexOf2 = string.indexOf("《隐私政策》");
        int i2 = iIndexOf2 + 6;
        spannableStringBuilder.setSpan(clickableSpan2, iIndexOf2, i2, 33);
        textView.setText(spannableStringBuilder);
        spannableStringBuilder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.color653636)), iIndexOf2, i2, 33);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(spannableStringBuilder);
        view.findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() { // from class: com.haichuang.img.ui.activity.login.StartActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                this.f$0.m508xbf15498e(view2);
            }
        });
        view.findViewById(R.id.tv_comfirm).setOnClickListener(new View.OnClickListener() { // from class: com.haichuang.img.ui.activity.login.StartActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                this.f$0.m509x2a0674f(view2);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$showUserXieyiDialog$0$com-haichuang-img-ui-activity-login-StartActivity, reason: not valid java name */
    /* synthetic */ void m508xbf15498e(View view) {
        this.customDialog.doDismiss();
        this.activity.finish();
    }

    /* JADX INFO: renamed from: lambda$showUserXieyiDialog$1$com-haichuang-img-ui-activity-login-StartActivity, reason: not valid java name */
    /* synthetic */ void m509x2a0674f(View view) {
        this.customDialog.doDismiss();
        App.getIntance().initThreeSDK();
        SPUtils.setParam(Constant.FIRST_ENTER, false);
        initPermissions(this.permissions);
        if (APPConfig.isShowAd()) {
            String str = (String) SPUtils.getParam(Constant.AD_APP_ID, "");
            if (TextUtils.isEmpty(str)) {
                return;
            }
            AdController.getInstance().init(this, str);
        }
    }

    private void initPermissions(String... permissions) {
        getConfig();
    }

    private /* synthetic */ void lambda$initPermissions$3(Boolean bool) throws Exception {
        if (bool.booleanValue()) {
            getConfig();
        } else {
            getConfig();
        }
    }

    private void getConfig() {
        ((LoginViewModel) this.viewModel).findConfig();
    }

    @Override // com.haichuang.img.base.BaseActivity
    protected void initViewModel() {
        ((LoginViewModel) this.viewModel).configLiveData.observe(this, new Observer() { // from class: com.haichuang.img.ui.activity.login.StartActivity$$ExternalSyntheticLambda2
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                this.f$0.m506x6cdce0a2((DataResponse) obj);
            }
        });
        ((LoginViewModel) this.viewModel).loginLiveData.observe(this, new Observer() { // from class: com.haichuang.img.ui.activity.login.StartActivity$$ExternalSyntheticLambda3
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                this.f$0.m507xb067fe63((DataResponse) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$initViewModel$4$com-haichuang-img-ui-activity-login-StartActivity, reason: not valid java name */
    /* synthetic */ void m506x6cdce0a2(DataResponse dataResponse) {
        if (dataResponse.isSuccess()) {
            Map<String, String> resultMap = ((ConfigRes) dataResponse.getData()).getResultMap();
            SPUtils.setParam(Constant.ISSHOWVERIFICATION, false);
            if (resultMap.containsKey("isVerification")) {
                String str = resultMap.get("isVerification");
                if (!TextUtils.isEmpty(str) && "1".equals(str)) {
                    SPUtils.setParam(Constant.ISSHOWVERIFICATION, true);
                }
            }
            if (resultMap.containsKey("isCharge")) {
                String str2 = resultMap.get("isCharge");
                if (!TextUtils.isEmpty(str2)) {
                    SPUtils.setParam(Constant.IS_CITY_FREE, Boolean.valueOf("0".equals(str2)));
                }
            }
            if (resultMap.containsKey("xAppid")) {
                String str3 = resultMap.get("xAppid");
                if (!TextUtils.isEmpty(str3) && str3 != null) {
                    try {
                        SPUtils.setParam(Constant.xfptWAppid, RsaUtils.decrypt(str3));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if (resultMap.containsKey("xAPIKey")) {
                String str4 = resultMap.get("xAPIKey");
                if (!TextUtils.isEmpty(str4) && str4 != null) {
                    try {
                        SPUtils.setParam(Constant.xfptWAPIKey, RsaUtils.decrypt(str4));
                    } catch (Exception e2) {
                        throw new RuntimeException(e2);
                    }
                }
            }
            if (resultMap.containsKey("xAPISecret")) {
                String str5 = resultMap.get("xAPISecret");
                if (!TextUtils.isEmpty(str5) && str5 != null) {
                    try {
                        SPUtils.setParam(Constant.xfptWAPISecret, RsaUtils.decrypt(str5));
                    } catch (Exception e3) {
                        throw new RuntimeException(e3);
                    }
                }
            }
            App.getIntance().initXf();
            if (resultMap.containsKey("isAd")) {
                SPUtils.setParam("isAd", resultMap.get("isAd"));
            }
            if (resultMap.containsKey(Constant.AD_APP_ID)) {
                SPUtils.setParam(Constant.AD_APP_ID, resultMap.get(Constant.AD_APP_ID));
            }
            if (resultMap.containsKey(Constant.AD_FULL_ID)) {
                SPUtils.setParam(Constant.AD_FULL_ID, resultMap.get(Constant.AD_FULL_ID));
            }
            if (resultMap.containsKey(Constant.AD_BANNER_ID)) {
                SPUtils.setParam(Constant.AD_BANNER_ID, resultMap.get(Constant.AD_BANNER_ID));
            }
            if (resultMap.containsKey(Constant.AD_SPLASH_ID)) {
                SPUtils.setParam(Constant.AD_SPLASH_ID, resultMap.get(Constant.AD_SPLASH_ID));
            }
            String str6 = (String) SPUtils.getParam(Constant.AD_SPLASH_ID, "");
            if (APPConfig.isShowAd() && !TextUtils.isEmpty(str6)) {
                AdController.getInstance().showSplashAd(this, str6, ((ActivityStartBinding) this.binding).llRoot, new SplashLoadListener() { // from class: com.haichuang.img.ui.activity.login.StartActivity.3
                    @Override // com.haichuang.img.adcontroller.SplashLoadListener
                    public void onLoadFinish() {
                        StartActivity.this.goMain();
                    }

                    @Override // com.haichuang.img.adcontroller.SplashLoadListener
                    public void onLoadError() {
                        StartActivity.this.goMain();
                    }
                });
                return;
            } else {
                goMain();
                return;
            }
        }
        ToastUtils.showToast("网络错误");
    }

    /* JADX INFO: renamed from: lambda$initViewModel$5$com-haichuang-img-ui-activity-login-StartActivity, reason: not valid java name */
    /* synthetic */ void m507xb067fe63(DataResponse dataResponse) {
        if (dataResponse.isSuccess()) {
            JumpUtils.goMain();
            finish();
        } else {
            ToastUtils.showToast(dataResponse.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void goMain() {
        String userName = APPConfig.getUserName();
        String userPassword = APPConfig.getUserPassword();
        if (!TextUtils.isEmpty(userName) && !TextUtils.isEmpty(userPassword)) {
            ((LoginViewModel) this.viewModel).login(userName, userPassword);
        } else {
            JumpUtils.goMain();
            finish();
        }
    }

    @Override // com.haichuang.img.base.BaseActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
    }

    public static Intent getAppDetailSettingIntent(Context context) {
        Intent intent = new Intent();
        intent.addFlags(268435456);
        intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        return intent;
    }
}
