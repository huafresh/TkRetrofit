package com.thinkive.android.tkretrofit;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.thinkive.framework.network.NetWorkService;
import com.android.thinkive.framework.network.ProtocolType;
import com.android.thinkive.framework.network.RequestBean;
import com.android.thinkive.framework.network.ResponseListener;
import com.android.thinkive.framework.network.http.HttpRequestBean;
import com.android.thinkive.framework.network.http.RequestMethod;
import com.android.thinkive.framework.network.socket.SocketRequestBean;
import com.android.thinkive.framework.util.JsonParseUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
    private static final String DS_NAME = "dsName";
    private static final String ERROR_NO = "error_no";
    private static final String ERROR_INFO = "error_info";
    private static final String ERRORNO = "errorNo";
    private static final String ERRORINFO = "errorInfo";

    private Call originalCall;
    private Type responseType;
    private Annotation[] annotations;
    private List<IRequestInterceptor> requestInterceptors = new ArrayList<>();
    private List<IResponseInterceptor> responseInterceptors = new ArrayList<>();
    private ICache<JSONObject> tkCache;

    TkRequest() {
        tkCache = new TkCache();
    }

    private @Nullable
    RequestBeanExt resolveRequestBean(Request request) {
        RequestBean requestBean = null;

        boolean shouldCache = false;
        //先解析Service注解
        for (Annotation annotation : annotations) {
            if (annotation instanceof SERVICE) {
                String url = ((SERVICE) annotation).url();
                ProtocolType type = ((SERVICE) annotation).protocolType();
                String funcNo = ((SERVICE) annotation).funcNo();
                String bizCode = ((SERVICE) annotation).bizCode();
                shouldCache = ((SERVICE) annotation).shouldCache();

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
            }
        }

        //然后把Request的相关参数拷贝到RequestBean
        if (requestBean != null) {
            //请求方式
            if (requestBean instanceof HttpRequestBean) {
                String method = request.method();
                ((HttpRequestBean) requestBean).setRequestMethod(convertMethod(method));
            }

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

        RequestBeanExt requestBeanExt = new RequestBeanExt(requestBean);
        requestBeanExt.setShouldCache(shouldCache);
        return requestBeanExt;
    }

    private RequestMethod convertMethod(String method) {
        switch (method) {
            case "GET":
                return RequestMethod.GET;
            case "POST":
                return RequestMethod.POST;
            default:
                return RequestMethod.GET;
        }
    }

    private void doRequest(final Emitter emitter) {
        final Request request = originalCall.request();
        RequestBeanExt requestBeanExt = resolveRequestBean(request);
        if (requestBeanExt != null) {
            if (runRequestInterceptor(requestBeanExt.requestBean)) {
                return;
            }

            final RequestBean requestBean = requestBeanExt.requestBean;

            if (requestBeanExt.isShouldCache()) {
                JSONObject cache = this.tkCache.getCache(requestBean);
                if (cache != null) {
                    emitResult(cache, emitter);
                    //发请求获取最新数据但是不回调。
                    sendRequest(requestBean, null);
                    return;
                }
            }

            sendRequest(requestBean, emitter);
        }
    }

    private void sendRequest(final RequestBean requestBean, final Emitter emitter) {
        NetWorkService.getInstance().request(requestBean,
                new ResponseListener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        if (emitter != null) {
                            emitResult(jsonObject, emitter);
                        }
                        tkCache.putCache(requestBean, jsonObject);
                    }

                    @Override
                    public void onErrorResponse(Exception e) {

                    }
                });
    }

    private void emitResult(JSONObject origin, Emitter emitter) {
        JSONObject jsonObject = null;
        try {
            jsonObject = runResponseInterceptor(origin);
        } catch (Exception e) {
            emitter.onError(e);
            return;
        }

        if (responseType == JSONObject.class) {
            emitter.onNext(jsonObject);
        } else if (responseType == String.class) {
            emitter.onNext(jsonObject.toString());
        } else {
            String errorNo;
            String errorInfo;

            if (jsonObject.has(ERROR_NO)) {
                errorNo = jsonObject.optString(ERROR_NO);
                if ("0".equals(errorNo)) {
                    //调用接口正常
                    parseResult(jsonObject, emitter);
                } else {
                    // 调用接口其他异常
                    errorInfo = jsonObject.optString(ERROR_INFO);
                    emitter.onError(new BaseRequestException(errorNo, errorInfo));
                }
            } else if (jsonObject.has(ERRORNO)) {
                errorNo = jsonObject.optString(ERRORNO);
                if ("0".equals(errorNo)) {
                    //调用接口正常
                    parseResult(jsonObject, emitter);
                } else {
                    //调用接口异常
                    errorInfo = jsonObject.optString(ERRORINFO);
                    emitter.onError(new BaseRequestException(String.valueOf(errorNo), errorInfo));
                }
            } else {
                emitter.onError(new BaseRequestException("-120", "服务器返回数据没有error_no或者errorNo"));
            }
        }
    }

    private void parseResult(JSONObject jsonObject, Emitter emitter) {
        String errorInfo = null;
        String dsName = null;
        JSONArray dsNameArray = jsonObject.optJSONArray(DS_NAME);
        if (dsNameArray == null) {
            dsName = jsonObject.optString(DS_NAME);
        } else {
            try {
                dsName = dsNameArray.getString(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (TextUtils.isEmpty(dsName)) {
            Object o = JsonParseUtil.parseJsonToObject(jsonObject.toString(), (Class<Object>) responseType);
            if (o != null) {
                emitter.onNext(o);
            } else {
                emitter.onError(new BaseRequestException("没有dsName且也无法解析成returnType" + responseType));
            }
            return;
        }

        JSONArray dataArray = jsonObject.optJSONArray(dsName);
        JSONObject dataObject = jsonObject.optJSONObject(dsName);
        if (dataArray != null) {
            if (responseType instanceof ParameterizedType) {
                if (((ParameterizedType) responseType).getRawType() == List.class) {
                    Type modelType = ((ParameterizedType) responseType).getActualTypeArguments()[0];
                    List<Object> list = JsonParseUtil.paseJsonToList(dataArray.toString(),
                            (Class<Object>) modelType);
                    if (list != null) {
                        emitter.onNext(list);
                    } else {
                        errorInfo = "无法解析" + dsName + "的内容为" + responseType;
                    }
                } else {
                    errorInfo = "返回的" + dsName + "的内容是JSONArray，无法解析成" + responseType + "。请改成List<T>";
                }
            } else {
                errorInfo = "要解析成list，则必须指定list的泛型";
            }
        }

        if (dataObject != null) {
            Object o = JsonParseUtil.parseJsonToObject(dataObject.toString(),
                    (Class<Object>) responseType);
            if (o != null) {
                emitter.onNext(o);
            } else {
                errorInfo = "无法解析" + dsName + "的内容为" + responseType;
            }
        }

        if (!TextUtils.isEmpty(errorInfo)) {
            emitter.onError(new BaseRequestException(errorInfo));
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

    private boolean runRequestInterceptor(RequestBean requestBean) {
        for (IRequestInterceptor interceptor : requestInterceptors) {
            if (interceptor != null) {
                if (interceptor.onRequest(new RequestBeanHelper(requestBean))) {
                    return true;
                }
            }
        }
        return false;
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
                if (TkRetrofit.requestInterceptors != null) {
                    tkRequest.requestInterceptors.addAll(TkRetrofit.requestInterceptors);
                }
                if (TkRetrofit.responseInterceptors != null) {
                    tkRequest.responseInterceptors.addAll(TkRetrofit.responseInterceptors);
                }
                if (requestInterceptor != null) {
                    tkRequest.requestInterceptors.add(requestInterceptor);
                }
                if (responseInterceptor != null) {
                    tkRequest.responseInterceptors.add(responseInterceptor);
                }
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
