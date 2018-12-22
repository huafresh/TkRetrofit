package com.thinkive.android.tkretrofit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.thinkive.framework.network.RequestBean;

import org.json.JSONObject;

/**
 * @author hua
 * @version 1.0
 * @date 2018/12/22
 */
interface ICache<T> {

    void putCache(RequestBean requestBean, @NonNull T cache);

    @Nullable T getCache(RequestBean requestBean);

    void removeCache(RequestBean requestBean);

}
