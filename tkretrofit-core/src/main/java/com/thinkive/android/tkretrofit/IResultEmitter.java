package com.thinkive.android.tkretrofit;

import org.json.JSONObject;

import java.lang.reflect.Type;

import io.reactivex.Emitter;

/**
 * @author hua
 * @version V1.0
 * @date 2018/12/27 8:46
 */

public interface IResultEmitter {

    /**
     * 根据returnType使用给定的Emitter向下游发射数据。
     *
     * @param jsonObject 经过{@link IResponseInterceptor}处理后的结果
     * @param returnType 接口返回值的类型（PS：Observer或者Flowable的泛型）
     * @param emitter    use to emit result
     */
    void emitResult(JSONObject jsonObject, Type returnType, Emitter emitter);

}
