package com.thinkive.android.tkretrofit;

import com.android.thinkive.framework.network.RequestBean;

import java.util.HashMap;

/**
 * @author hua
 * @version 2018/11/2 17:47
 */

public class RequestBeanHelper {

    private RequestBean requestBean;
    private HashMap<String, String> headers;
    private HashMap<String, String> params;

    public RequestBeanHelper(RequestBean requestBean) {
        this.requestBean = requestBean;
        this.headers = requestBean.getHeader();
        this.params = requestBean.getParam();
    }

    public void addHeader(String key, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(key, value);
        requestBean.setHeader(headers);
    }

    public void addParam(String key,String value){
        if (params == null) {
            params = new HashMap<>();
        }
        params.put(key, value);
        requestBean.setParam(params);
    }
}
