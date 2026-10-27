package com.haichuang.img.net;

import cn.wandersnail.commons.helper.FileDownloadHelper;
import com.haichuang.img.APPConfig;
import com.haichuang.img.utils.LogUtils;
import com.iflytek.sparkchain.utils.DataUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;

/* JADX INFO: loaded from: classes2.dex */
public class HttpInterceptor implements Interceptor {
    @Override // okhttp3.Interceptor
    public synchronized Response intercept(Interceptor.Chain chain) throws IOException {
        Response responseProceed;
        Charset charsetForName = Charset.forName(DataUtil.UTF8);
        Request request = chain.request();
        LogUtils.d("url:" + request.url());
        Buffer buffer = new Buffer();
        if (request.body() != null) {
            request.body().writeTo(buffer);
            LogUtils.d("body:" + buffer.readString(charsetForName));
        } else {
            LogUtils.d("body == null:");
        }
        responseProceed = chain.proceed(request.newBuilder().addHeader("authentication", APPConfig.getToken()).build());
        try {
            for (String str : responseProceed.headers().names()) {
                LogUtils.d(" header:" + str + "=" + responseProceed.header(str));
            }
            if (FileDownloadHelper.MIME_TYPE_BINARY.equals(responseProceed.header("Content-Type"))) {
                LogUtils.d("response file: " + responseProceed.header("Content-Disposition"));
            } else {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(responseProceed.peekBody(Long.MAX_VALUE).byteStream(), charsetForName));
                StringBuilder sb = new StringBuilder();
                String line = bufferedReader.readLine();
                do {
                    sb.append(line);
                    line = bufferedReader.readLine();
                } while (line != null);
                LogUtils.d("response: " + sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.d(e.getMessage());
        }
        return responseProceed;
    }
}
