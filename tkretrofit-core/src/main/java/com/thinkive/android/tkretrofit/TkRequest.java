package com.thinkive.android.tkretrofit;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.thinkive.framework.network.NetWorkService;
import com.android.thinkive.framework.network.ProtocolType;
import com.android.thinkive.framework.network.RequestBean;
import com.android.thinkive.framework.network.ResponseListener;
import com.android.thinkive.framework.network.http.HttpRequestBean;
import com.android.thinkive.framework.network.socket.SocketRequestBean;
import com.android.thinkive.framework.util.JsonParseUtil;

import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Retrofit;

/**
 * @author hua
 * @version 2018/10/26 10:14
 */

@SuppressWarnings("unchecked")
class TkRequest {
    private Call originalCall;
    private Type responseType;
    private Annotation[] annotations;
    private List<IRequestInterceptor> requestInterceptors;
    private List<IResponseInterceptor> responseInterceptors;

    private @Nullable
    RequestBean resolveRequestBean(Request request) {
        RequestBean requestBean = null;

        //先解析Service注解
        for (Annotation annotation : annotations) {
            if (annotation instanceof SERVICE) {
                String url = ((SERVICE) annotation).url();
                ProtocolType type = ((SERVICE) annotation).protocolType();
                String funcNo = ((SERVICE) annotation).funcNo();
                String bizCode = ((SERVICE) annotation).bizCode();
                boolean shouldCache = ((SERVICE) annotation).shouldCache();
                long cacheTimeout = ((SERVICE) annotation).cacheTimeout();

                if (TextUtils.isEmpty(url)) {
                    throw new IllegalArgumentException("url can not be empty");
                }

                if (type == ProtocolType.SOCKET) {
                    requestBean = new SocketRequestBean();
                    requestBean.setUrlName(url);
                } else {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        requestBean = new HttpRequestBean();
                        ((HttpRequestBean) requestBean).setUrlAddress(url);
                    } else {
                        requestBean = new RequestBean();
                        requestBean.setUrlName(url);
                    }
                }
                requestBean.setProtocolType(type);

                HashMap<String, String> params = new HashMap<>();
                if (!TextUtils.isEmpty(bizCode)) {
                    params.put("bizCode", bizCode);
                }
                if (!TextUtils.isEmpty(funcNo)) {
                    params.put("funcNo", funcNo);
                }
                requestBean.setParam(params);

                requestBean.setShouldCache(shouldCache);
                requestBean.setCacheTimeout(cacheTimeout);

            }
        }

        //然后把Request的相关参数拷贝到RequestBean
        if (requestBean != null) {
            //请求头
            HashMap<String, String> header = requestBean.getHeader();
            if (header == null) {
                header = new HashMap<>();
            }
            Headers headers = request.headers();
            for (int i = 0; i < headers.size(); i++) {
                String name = headers.name(i);
                String value = headers.value(i);
                header.put(name, value);
            }
            requestBean.setHeader(header);

            //请求参数
            HashMap<String, String> paramMap = requestBean.getParam();
            if (paramMap == null) {
                paramMap = new HashMap<>();
            }
            //处理Params注解
            for (Annotation annotation : annotations) {
                if (annotation instanceof Params) {
                    String[] params = ((Params) annotation).value();
                    for (String keyValue : params) {
                        String[] keyValueArray = keyValue.split(":");
                        paramMap.put(keyValueArray[0], keyValueArray[1]);
                    }
                }
            }
            HttpUrl httpUrl = request.url();
            for (int i = 0; i < httpUrl.querySize(); i++) {
                String name = httpUrl.queryParameterName(i);
                String value = httpUrl.queryParameterValue(i);
                paramMap.put(name, value);
            }
            requestBean.setParam(paramMap);

            //其他的比如requestBody需要时可以自行支持。
        }

        return requestBean;
    }

    private void doRequest(final Emitter emitter) {
        final Request request = originalCall.request();
        RequestBean requestBean = resolveRequestBean(request);
        if (requestBean != null) {
            runRequestInterceptor(requestBean);
            NetWorkService.getInstance().request(requestBean, new ResponseListener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    JSONObject newJsonObject = runResponseInterceptor(jsonObject);

                    if (responseType == JSONObject.class) {
                        emitter.onNext(newJsonObject);
                    } else if (responseType == String.class) {
                        emitter.onNext(newJsonObject.toString());
                    } else {
                        Object result = JsonParseUtil.parseJsonToObject(newJsonObject.toString(),
                                (Class<Object>) responseType);
                        if (result == null) {
                            emitter.onError(new Exception("解析" + responseType + "时出错"));
                        }
                    }
                }

                @Override
                public void onErrorResponse(Exception e) {
                    emitter.onError(e);
                }
            });
        }
    }

    private JSONObject runResponseInterceptor(JSONObject jsonObject) {
        JSONObject temp = jsonObject;
        for (IResponseInterceptor interceptor : responseInterceptors) {
            if (interceptor != null) {
                temp = interceptor.onResponse(temp);
            }
        }
        return temp;
    }

    private void runRequestInterceptor(RequestBean requestBean) {
        for (IRequestInterceptor interceptor : requestInterceptors) {
            if (interceptor != null) {
                interceptor.onRequest(new RequestBeanHelper(requestBean));
            }
        }
    }

    Observable<?> createObservable(Call<Object> call,
                                   Type responseType,
                                   Annotation[] annotations) {
        this.originalCall = call;
        this.responseType = responseType;
        this.annotations = annotations;
        return Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                doRequest(e);
            }
        }).subscribeOn(Schedulers.newThread());

    }

    Flowable<?> createFlowable(Call<Object> call,
                               Type responseType,
                               Annotation[] annotations) {
        this.originalCall = call;
        this.responseType = responseType;
        this.annotations = annotations;
        return Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(FlowableEmitter<Object> e) throws Exception {
                doRequest(e);
            }
        }, BackpressureStrategy.BUFFER).subscribeOn(Schedulers.newThread());
    }

    public static class Builder {
        private IRequestInterceptor requestInterceptor;
        private IResponseInterceptor responseInterceptor;
        private Retrofit retrofit;
        TkRetrofit tkRetrofit;

        public Builder(TkRetrofit tkRetrofit) {
            this.tkRetrofit = tkRetrofit;
        }

        public Builder setRequestInterceptor(IRequestInterceptor requestInterceptor) {
            this.requestInterceptor = requestInterceptor;
            return this;
        }

        public Builder setResponseInterceptor(IResponseInterceptor responseInterceptor) {
            this.responseInterceptor = responseInterceptor;
            return this;
        }

        public <T> T create(final Class<T> service) {
            if (retrofit == null) {
                TkRequest tkRequest = new TkRequest();
                tkRequest.requestInterceptors.addAll(TkRetrofit.requestInterceptors);
                tkRequest.responseInterceptors.addAll(TkRetrofit.responseInterceptors);
                tkRequest.requestInterceptors.add(requestInterceptor);
                tkRequest.responseInterceptors.add(responseInterceptor);
                TkCallAdapter tkCallAdapter = new TkCallAdapter(tkRequest);
                retrofit = new Retrofit.Builder()
                        .addCallAdapterFactory(tkCallAdapter)
                        .baseUrl(tkRetrofit.defaultRetrofit.baseUrl())
                        .build();
            }
            return retrofit.create(service);
        }

    }

}
