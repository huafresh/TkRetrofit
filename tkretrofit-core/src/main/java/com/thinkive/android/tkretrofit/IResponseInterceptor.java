package com.thinkive.android.tkretrofit;

import org.json.JSONObject;

/**
 * @author hua
 * @version 2018/10/26 18:24
 */

public interface IResponseInterceptor {
    /**
     * called when get response
     *
     * @param jsonObject request result
     * @return return new result if need
     */
    JSONObject onResponse(JSONObject jsonObject);
}
