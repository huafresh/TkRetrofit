package com.thinkive.android.tkretrofit;

/**
 * @author hua
 * @version 2018/10/26 18:24
 */

public interface IRequestInterceptor {
    /**
     * called before request
     *
     * @param requestBeanHelper RequestBean helper
     * @return true abort this request, false otherwise
     */
    boolean onRequest(RequestBeanHelper requestBeanHelper);
}
