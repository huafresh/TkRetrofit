package com.thinkive.android.network;

import com.android.thinkive.framework.network.ProtocolType;
import com.android.thinkive.framework.util.Constant;
import com.thinkive.android.tkretrofit.Params;
import com.thinkive.android.tkretrofit.SERVICE;

import org.json.JSONObject;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * @author hua
 * @version V1.0
 * @date 2018/11/20 17:13
 */

public interface ITestApi {

    @GET(value = "/") //retrofit要求一定要有，这里的value值没什么用，可以随便设置，但是不可为空
    @Headers({
            Constant.PARAM_COMPANYID + ":THINKIVE",
            Constant.PARAM_SYSTEMID + ":LCSMALL"
    })
    @SERVICE(url = "THINKIVE_SOCKET_URL",
            protocolType = ProtocolType.SOCKET,
            funcNo = "105000")
    Observable<TestBean> sendSms(@Query("phone") String phone);


    @GET(value = "/") //retrofit要求一定要有，这里的value值没什么用，可以随便设置，但是不可为空
    @SERVICE(url = "http://wanandroid.com/article/listproject/0/json",
            shouldCache = true)
    Observable<JSONObject> testCache();

    @GET(value = "/")
    @SERVICE(url = "http://info.thinkive.com:8082/servlet/json")
    @Params({
            "funcNo:200303",
            "catalogid:1"
    })
    Observable<List<NoticeInfoBean>> getNoticeInfoList(@Query(value = "curpage") String curPage);

}
