package com.thinkive.android.tkretrofit;

import com.android.thinkive.framework.network.RequestBean;

/**
 * @author hua
 * @version 1.0
 * @date 2018/12/22
 */
class RequestBeanExt {
    RequestBean requestBean;
    private boolean shouldCache;

    RequestBeanExt(RequestBean requestBean) {
        this.requestBean = requestBean;
    }

    boolean isShouldCache() {
        return shouldCache;
    }

    void setShouldCache(boolean shouldCache) {
        this.shouldCache = shouldCache;
    }
}
