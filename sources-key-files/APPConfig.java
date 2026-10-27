package com.haichuang.img;

import android.text.TextUtils;
import com.google.gson.Gson;
import com.haichuang.img.net.res.LoginRes;
import com.haichuang.img.utils.Constant;
import com.haichuang.img.utils.SPUtils;
import java.util.Map;
import java.util.Objects;

/* JADX INFO: loaded from: classes2.dex */
public class APPConfig {
    public static boolean isVip() {
        return true;
    }

    public static boolean isToll() {
        LoginRes.UserVoBean.ConfigBean config;
        if (((Boolean) SPUtils.getParam(Constant.IS_CITY_FREE, false)).booleanValue()) {
            return false;
        }
        String str = (String) SPUtils.getParam(Constant.LOGININFO, "");
        return (TextUtils.isEmpty(str) || (config = ((LoginRes) new Gson().fromJson(str, LoginRes.class)).getUserVo().getConfig()) == null || config.getIsCharge() != 1) ? false : true;
    }

    public static boolean isShowVerification() {
        return ((Boolean) SPUtils.getParam(Constant.ISSHOWVERIFICATION, false)).booleanValue();
    }

    public static String getPayPrompt() {
        Map<String, String> map;
        String str = (String) SPUtils.getParam(Constant.LOGININFO, "");
        if (!TextUtils.isEmpty(str) && (map = ((LoginRes) new Gson().fromJson(str, LoginRes.class)).getUserVo().getMap()) != null && map.containsKey(Constant.IS_PAY_PROMPT)) {
            return map.get(Constant.IS_PAY_PROMPT);
        }
        return "0";
    }

    public static boolean isShowAlipay() {
        LoginRes.UserVoBean.ConfigBean config;
        String str = (String) SPUtils.getParam(Constant.LOGININFO, "");
        return (TextUtils.isEmpty(str) || (config = ((LoginRes) new Gson().fromJson(str, LoginRes.class)).getUserVo().getConfig()) == null || config.getIsAlipay() != 1) ? false : true;
    }

    public static boolean isShowWxPay() {
        LoginRes.UserVoBean.ConfigBean config;
        String str = (String) SPUtils.getParam(Constant.LOGININFO, "");
        return (TextUtils.isEmpty(str) || (config = ((LoginRes) new Gson().fromJson(str, LoginRes.class)).getUserVo().getConfig()) == null || config.getIsWchat() != 1) ? false : true;
    }

    public static String getWXpayId() {
        LoginRes.UserVoBean.ConfigBean config;
        String str = (String) SPUtils.getParam(Constant.LOGININFO, "");
        if (TextUtils.isEmpty(str) || (config = ((LoginRes) new Gson().fromJson(str, LoginRes.class)).getUserVo().getConfig()) == null) {
            return "";
        }
        config.getWxAppid();
        return config.getWxAppid();
    }

    public static String getQQ() {
        LoginRes.UserVoBean.ConfigBean config;
        String str = (String) SPUtils.getParam(Constant.LOGININFO, "");
        if (TextUtils.isEmpty(str) || (config = ((LoginRes) new Gson().fromJson(str, LoginRes.class)).getUserVo().getConfig()) == null) {
            return "3360883083@qq.com";
        }
        String kfqq = config.getKfqq();
        return (TextUtils.isEmpty(kfqq) || "740136894".equals(kfqq)) ? "3360883083@qq.com" : kfqq;
    }

    public static boolean isLogin() {
        return !TextUtils.isEmpty((String) SPUtils.getParam(Constant.USERNAME, ""));
    }

    public static String getHlepRemarks() {
        return (String) SPUtils.getParam(Constant.HELPREMARKS, "紧急救助");
    }

    public static String getHlepPhone() {
        return (String) SPUtils.getParam(Constant.HELPPHONE, "110");
    }

    public static String getUserName() {
        return (String) SPUtils.getParam(Constant.USERNAME, "");
    }

    public static String getUserPassword() {
        return (String) SPUtils.getParam(Constant.USERPASSWORD, "");
    }

    public static String getUserSex() {
        return (String) SPUtils.getParam(Constant.USERSEX, "保密");
    }

    public static String getUserAge() {
        return (String) SPUtils.getParam(Constant.USERAGE, "18");
    }

    public static boolean isReadProtocol() {
        return ((Boolean) SPUtils.getParam(Constant.READPROTOCOL, false)).booleanValue();
    }

    public static boolean isReadFirstDialog() {
        return ((Boolean) SPUtils.getParam(Constant.FIRST_HELP, false)).booleanValue();
    }

    public static String getToken() {
        return (String) SPUtils.getParam(Constant.TOKEN, "");
    }

    public static void clear() {
        SPUtils.setParam(Constant.USERNAME, "");
        SPUtils.setParam(Constant.USERPASSWORD, "");
        SPUtils.setParam(Constant.SAVETIME, 0L);
        SPUtils.setParam(Constant.READPROTOCOL, false);
    }

    public static boolean isShowAd() {
        if (isVip()) {
            return false;
        }
        return Objects.equals(SPUtils.getParam("isAd", "0"), "1");
    }
}
