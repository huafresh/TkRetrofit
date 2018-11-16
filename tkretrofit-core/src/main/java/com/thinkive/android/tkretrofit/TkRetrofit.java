package com.thinkive.android.tkretrofit;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;

/**
 * @author hua
 * @version 2018/10/26 13:50
 */

public final class TkRetrofit {

    /**
     * we don't need, just avoid crash
     */
    private static final String RETROFIT_BASE_URL = "http://www.baidu.com/";
    final Retrofit defaultRetrofit;
    static List<IRequestInterceptor> requestInterceptors;
    static List<IResponseInterceptor> responseInterceptors;

    public static TkRetrofit get() {
        return Holder.S_INSTANCE;
    }

    private static final class Holder {
        private static final TkRetrofit S_INSTANCE = new TkRetrofit();
    }

    private TkRetrofit() {
        defaultRetrofit = new Retrofit.Builder()
                .addCallAdapterFactory(new TkCallAdapter(new TkRequest()))
                .baseUrl(RETROFIT_BASE_URL)
                .build();
    }

    public <T> T create(final Class<T> service) {
        return defaultRetrofit.create(service);
    }

    public TkRequest.Builder setRequestInterceptor(IRequestInterceptor interceptor) {
        return new TkRequest.Builder(this)
                .setRequestInterceptor(interceptor);
    }

    public TkRequest.Builder setResponseInterceptor(IResponseInterceptor interceptor) {
        return new TkRequest.Builder(this)
                .setResponseInterceptor(interceptor);
    }

    public static void addGlobalRequestInterceptor(IRequestInterceptor interceptor) {
        if (requestInterceptors == null) {
            requestInterceptors = new ArrayList<>();
        }
        requestInterceptors.add(interceptor);
    }

    public static void addGlobalResponseInterceptor(IResponseInterceptor interceptor) {
        if (responseInterceptors == null) {
            responseInterceptors = new ArrayList<>();
        }
        responseInterceptors.add(interceptor);
    }

}
