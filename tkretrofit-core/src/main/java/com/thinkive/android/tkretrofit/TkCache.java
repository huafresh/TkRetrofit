package com.thinkive.android.tkretrofit;

import android.support.annotation.NonNull;

import com.android.thinkive.framework.cache.Cache;
import com.android.thinkive.framework.config.ConfigManager;
import com.android.thinkive.framework.network.CacheType;
import com.android.thinkive.framework.network.NetWorkService;
import com.android.thinkive.framework.network.RequestBean;
import com.android.thinkive.framework.network.http.HttpRequestBean;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * @author hua
 * @version 1.0
 * @date 2018/12/22
 */
class TkCache implements ICache<JSONObject> {

    @Override
    public void putCache(RequestBean bean, @NonNull JSONObject cache) {
        String key = buildKey(bean);
        Cache.Entry entry = new Cache.Entry();
        entry.data = cache.toString().getBytes();
        entry.ttl = Long.MAX_VALUE;
        NetWorkService.getInstance().putCache(key, entry, CacheType.DISK);
    }

    private static String buildKey(RequestBean bean) {
        String urlName = bean.getUrlName();
        String urlAddress;
        if (bean instanceof HttpRequestBean) {
            urlAddress = ((HttpRequestBean) bean).getUrlAddress();
            if (!urlAddress.contains("?")) {
                urlAddress = urlAddress + "?";
            }
        } else {
            urlAddress = ConfigManager.getInstance().getAddressConfigValue(urlName);
        }
        HashMap<String, String> param = bean.getParam();
        return NetWorkService.getInstance().buildCacheKey(urlAddress, param);
    }

    @Override
    public JSONObject getCache(RequestBean requestBean) {
        return NetWorkService.getInstance().getCacheData(requestBean, CacheType.DISK);
    }

    @Override
    public void removeCache(RequestBean requestBean) {
        NetWorkService.getInstance().removeCache(buildKey(requestBean), CacheType.DISK);
    }
}
