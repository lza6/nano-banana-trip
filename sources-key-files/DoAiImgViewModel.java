package com.haichuang.img.ui.viewmodel;

import android.app.Application;
import androidx.lifecycle.MutableLiveData;
import com.haichuang.img.App;
import com.haichuang.img.AppExecutors;
import com.haichuang.img.base.BaseViewModel;
import com.haichuang.img.utils.FileUtil;
import com.haichuang.img.utils.LogUtils;
import com.haichuang.img.utils.SPUtils;
import com.haichuang.img.utils.ToastUtils;
import com.iflytek.sparkchain.core.LLM;
import com.iflytek.sparkchain.core.LLMCallbacks;
import com.iflytek.sparkchain.core.LLMConfig;
import com.iflytek.sparkchain.core.LLMError;
import com.iflytek.sparkchain.core.LLMEvent;
import com.iflytek.sparkchain.core.LLMFactory;
import com.iflytek.sparkchain.core.LLMResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/* JADX INFO: loaded from: classes2.dex */
public class DoAiImgViewModel extends BaseViewModel {
    private String TAG;
    private LLM imgLlm;
    public final MutableLiveData<String> imgResultLiveData;
    public final MutableLiveData<Boolean> loadingStatusLiveData;
    public String myXFContext;

    public DoAiImgViewModel(Application application) {
        super(application);
        this.TAG = "DoAiImgViewModel";
        this.loadingStatusLiveData = new MutableLiveData<>();
        this.imgResultLiveData = new MutableLiveData<>();
        this.myXFContext = App.RANDOM_UUID;
    }

    public void doImg(final String msg, int w, int h) {
        this.loadingStatusLiveData.postValue(true);
        LLM llmImageGeneration = LLMFactory.imageGeneration(w, h, LLMConfig.builder());
        this.imgLlm = llmImageGeneration;
        llmImageGeneration.registerLLMCallbacks(new LLMCallbacks() { // from class: com.haichuang.img.ui.viewmodel.DoAiImgViewModel.1
            @Override // com.iflytek.sparkchain.core.LLMCallbacks
            public void onLLMEvent(LLMEvent llmEvent, Object o) {
            }

            @Override // com.iflytek.sparkchain.core.LLMCallbacks
            public void onLLMResult(LLMResult llmResult, Object o) {
                if (llmResult.getImage() != null) {
                    byte[] image = llmResult.getImage();
                    String str = FileUtil.getAIWorkPath() + System.currentTimeMillis() + ".jpg";
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(str);
                        fileOutputStream.write(image);
                        fileOutputStream.close();
                        LogUtils.d(DoAiImgViewModel.this.TAG, "图片文件已成功写入：" + str);
                        LogUtils.d(DoAiImgViewModel.this.TAG, "图片文件已成功写入：" + new File(str).length());
                        SPUtils.setParam("shiyong", true);
                        DoAiImgViewModel.this.imgResultLiveData.postValue(str);
                        DoAiImgViewModel.this.loadingStatusLiveData.postValue(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override // com.iflytek.sparkchain.core.LLMCallbacks
            public void onLLMError(LLMError llmError, Object o) {
                LogUtils.e(DoAiImgViewModel.this.TAG, "生成失败：" + llmError.getErrMsg());
                DoAiImgViewModel.this.loadingStatusLiveData.postValue(false);
            }
        });
        AppExecutors.runNetworkIO(new Runnable() { // from class: com.haichuang.img.ui.viewmodel.DoAiImgViewModel$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m577lambda$doImg$0$comhaichuangimguiviewmodelDoAiImgViewModel(msg);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$doImg$0$com-haichuang-img-ui-viewmodel-DoAiImgViewModel, reason: not valid java name */
    /* synthetic */ void m577lambda$doImg$0$comhaichuangimguiviewmodelDoAiImgViewModel(String str) {
        int iArun = this.imgLlm.arun(str, this.myXFContext);
        if (iArun != 0) {
            this.loadingStatusLiveData.postValue(false);
            ToastUtils.showToast("生成失败，请重试");
        }
        LogUtils.d(this.TAG, "AI绘画执行结果：" + iArun);
    }
}
