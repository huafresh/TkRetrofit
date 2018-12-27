package com.thinkive.android.network;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.android.thinkive.framework.ThinkiveInitializer;
import com.android.thinkive.framework.config.ConfigManager;
import com.android.thinkive.framework.network.ProtocolType;
import com.android.thinkive.framework.network.RequestBean;
import com.thinkive.android.tkretrofit.BaseRequestException;
import com.thinkive.android.tkretrofit.BaseRequestObserver;
import com.thinkive.android.tkretrofit.IRequestInterceptor;
import com.thinkive.android.tkretrofit.IResponseInterceptor;
import com.thinkive.android.tkretrofit.RequestBeanHelper;
import com.thinkive.android.tkretrofit.TkRetrofit;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ThinkiveInitializer.getInstance().initialze(this.getApplication());

        findViewById(R.id.request).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doRequest();
            }
        });

        findViewById(R.id.request_notice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TkRetrofit.get()
                        .create(ITestApi.class)
                        .getNoticeInfoList("1")
//                        .subscribe(new BaseRequestObserver<List<NoticeInfoBean>>() {
//                            @Override
//                            protected void onSuccess(Context context, List<NoticeInfoBean> result) {
//                                Log.e("@@@hua", "notice success, num = " + result.size());
//                            }
//
//                            @Override
//                            protected void onFailed(Context context, BaseRequestException e) {
//                                Log.e("@@@hua", "notice failed, msg = " + e.getErrorInfo());
//                            }
//                        });
                .subscribe(new BaseRequestObserver<NoticeInfoBean>() {
                    @Override
                    protected void onSuccess(Context context, NoticeInfoBean result) {
                        Log.e("@@@hua", "notice success, result = " + result);
                    }

                    @Override
                    protected void onFailed(Context context, BaseRequestException e) {
                        Log.e("@@@hua", "notice failed, msg = " + e.getErrorInfo());
                    }
                });
            }
        });

        findViewById(R.id.test_cahe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testCache();
            }
        });
    }

    private void testCache() {
        TkRetrofit.get()
                .create(ITestApi.class)
                .testCache()
                .subscribe(new BaseRequestObserver<JSONObject>() {
                    @Override
                    protected void onSuccess(Context context, JSONObject result) {
                        Log.e("@@@hua", "test cache result = " + result.toString());
                    }

                    @Override
                    protected void onFailed(Context context, BaseRequestException e) {
                        Log.e("@@@hua", "test cache failed");
                    }
                });
    }

    private void doRequest() {
        TkRetrofit.get()
                .setRequestInterceptor(new IRequestInterceptor() {
                    @Override
                    public boolean onRequest(RequestBeanHelper requestBeanHelper) {
                        //请求拦截，只针对本次请求有效
                        //返回true，则终止本次请求
                        Log.e("@@@hua", "request = " + printLogForRequestUrl(requestBeanHelper.getRequestBean()));
                        return false;
                    }
                })
                .setResponseInterceptor(new IResponseInterceptor() {
                    @Override
                    public JSONObject onResponse(JSONObject jsonObject) {
                        //响应拦截，只针对本次请求有效
                        //传进来的是框架返回的原始数据
                        //这里返回的JSONObject会传到后面做bean解析。
                        Log.e("@@@hua", "onResponse = " + jsonObject.toString());
                        return null;
                    }
                })
                .create(ITestApi.class)
                .sendSms("1383838438")
                .subscribe(new BaseRequestObserver<TestBean>() {
                    @Override
                    protected void onSuccess(Context context, TestBean result) {
                        //成功
                        //关于bean结果的解析：
                        //默认情况下，tkRetrofit首先会尝试整体解析成接口里面定义的bean，
                        // 如果失败，则再尝试把results里面的内容解析成bean，还是失败就走error。
                        //如果结果结构比较复杂，可以在上面通过setResponseInterceptor设置响应拦截，
                        // 然后修改JSONObject的结构，然后传到下游解析即可。
                        //当然，retrofit本身支持的ConverterAdapt也是支持的。
                        Log.e("@@@hua", "onSuccess");
                    }

                    @Override
                    protected void onFailed(Context context, BaseRequestException e) {
                        //失败
                        Log.e("@@@hua", "onFailed");
                    }
                });
    }

    public static String printLogForRequestUrl(RequestBean requestBean) {
        //获取本次请求的完整Url
        String urlPre = ConfigManager.getInstance().getAddressConfigValue(requestBean.getUrlName());
        if (!urlPre.endsWith("?")) {
            urlPre += "?";
        }
        // 取出本次请求的入参
        HashMap<String, String> params = requestBean.getParam();
        // 定义拼接入参的StringBuilder
        StringBuilder paramsStringBuilder = new StringBuilder();
        // 循环取出入参map中的数据，拼接到一起
        for (Map.Entry entry : params.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            paramsStringBuilder.append(key);
            paramsStringBuilder.append("=");
            paramsStringBuilder.append(value);
            paramsStringBuilder.append("&");
        }
        // 删除最后一次循环时多余拼接的"&"
        paramsStringBuilder.deleteCharAt(paramsStringBuilder.length() - 1);
        return urlPre + paramsStringBuilder.toString();
    }

}
