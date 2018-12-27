package com.thinkive.android.tkretrofit;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.thinkive.framework.util.JsonParseUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import io.reactivex.Emitter;

/**
 * @author hua
 * @version V1.0
 * @date 2018/12/27 8:49
 */
@SuppressWarnings("unchecked")
public class DefaultResultEmitter implements IResultEmitter {

    private static final String DS_NAME = "dsName";
    private static final String ERROR_NO = "error_no";
    private static final String ERROR_INFO = "error_info";
    private static final String ERRORNO = "errorNo";
    private static final String ERRORINFO = "errorInfo";

    @Override
    public void emitResult(JSONObject jsonObject, Type returnType, Emitter emitter) {
        if (returnType == JSONObject.class) {
            emitter.onNext(jsonObject);
        } else if (returnType == String.class) {
            emitter.onNext(jsonObject.toString());
        } else {
            String errorNo;
            String errorInfo;

            if (jsonObject.has(ERROR_NO)) {
                errorNo = jsonObject.optString(ERROR_NO);
                if ("0".equals(errorNo)) {
                    //调用接口正常
                    parseResult(jsonObject, returnType, emitter);
                } else {
                    // 调用接口其他异常
                    errorInfo = jsonObject.optString(ERROR_INFO);
                    emitter.onError(new BaseRequestException(errorNo, errorInfo));
                }
            } else if (jsonObject.has(ERRORNO)) {
                errorNo = jsonObject.optString(ERRORNO);
                if ("0".equals(errorNo)) {
                    //调用接口正常
                    parseResult(jsonObject, returnType, emitter);
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

    @SuppressWarnings("ConstantConditions")
    private void parseResult(JSONObject jsonObject, Type returnType, Emitter emitter) {
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
            Object o = parseToObject(returnType, jsonObject);
            if (o != null) {
                emitter.onNext(o);
            } else {
                emitter.onError(new BaseRequestException("没有dsName且也无法解析成" + returnType));
            }
            return;
        }

        JSONArray dataArray = jsonObject.optJSONArray(dsName);
        JSONObject dataObject = jsonObject.optJSONObject(dsName);
        if (dataArray != null) {
            if (returnType instanceof ParameterizedType) {
                if (((ParameterizedType) returnType).getRawType() == List.class) {
                    Type modelType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
                    List<Object> list = parseToList(dataArray, modelType);
                    if (list != null) {
                        emitter.onNext(list);
                    } else {
                        errorInfo = "无法解析\"" + dsName + "\"的内容为" + returnType;
                    }
                } else {
                    errorInfo = String.format("结果集中\"" + dsName + "\"的内容是JSONArray，无法解析成" + returnType +
                            "。请改成List<%s>", returnType);
                }
            } else {
                errorInfo = String.format("结果集中\"" + dsName + "\"的内容是JSONArray，" +
                        "请指定返回值为Observable<List<%s>>或者Flowable<List<%s>>", returnType, returnType);
            }
        } else if (dataObject != null) {
            Object o = parseToObject(returnType, dataObject);
            if (o != null) {
                emitter.onNext(o);
            } else {
                errorInfo = "无法解析\"" + dsName + "\"的内容为" + returnType;
            }
        }

        if (!TextUtils.isEmpty(errorInfo)) {
            emitter.onError(new BaseRequestException(errorInfo));
        }
    }

    @Nullable
    private List<Object> parseToList(JSONArray dataArray, Type modelType) {
        List<Object> list = null;
        try {
            list = JsonParseUtil.paseJsonToList(dataArray.toString(),
                    (Class<Object>) modelType);
        } catch (Exception e) {
            //ignore
        }
        return list;
    }

    private @Nullable
    Object parseToObject(Type type, JSONObject jsonObject) {
        Object o = null;
        try {
            o = JsonParseUtil.parseJsonToObject(jsonObject, (Class<Object>) type);
        } catch (Exception e) {
            //ignore
        }
        return o;
    }
}
