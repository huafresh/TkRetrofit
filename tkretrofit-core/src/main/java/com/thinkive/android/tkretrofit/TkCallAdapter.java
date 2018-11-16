package com.thinkive.android.tkretrofit;

import android.support.annotation.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * @author hua
 * @version 2018/10/26 9:38
 */

public class TkCallAdapter extends CallAdapter.Factory {
    private TkRequest tkRequest;

    TkCallAdapter(TkRequest tkRequest) {
        this.tkRequest = tkRequest;
    }

    @Override
    public CallAdapter<?, ?> get(@NonNull Type returnType,
                                 @NonNull final Annotation[] annotations,
                                 @NonNull Retrofit retrofit) {
        final Type responseType = getCallResponseType(returnType);
        Class<?> rawType = getRawType(returnType);
        if (rawType == Observable.class) {
            return new CallAdapter<Object, Observable<?>>() {
                @Override
                public Type responseType() {
                    //Retrofit本身的结果转换我们不需要，这里返回默认的可以避免崩溃
                    return ResponseBody.class;
                }

                @Override
                public Observable<?> adapt(@NonNull Call<Object> call) {
                    return tkRequest.createObservable(call, responseType, annotations);
                }
            };
        } else if (rawType == Flowable.class) {
            return new CallAdapter<Object, Flowable<?>>() {
                @Override
                public Type responseType() {
                    //Retrofit本身的结果转换我们不需要，这里返回默认的可以避免崩溃
                    return ResponseBody.class;
                }

                @Override
                public Flowable<?> adapt(@NonNull Call<Object> call) {
                    return tkRequest.createFlowable(call, responseType, annotations);
                }
            };
        }

        throw new IllegalArgumentException("tkRetrofit only support Observale or Flowable, now is : " + rawType);
    }

    private static Type getCallResponseType(Type returnType) {
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                    "return type must be parameterized as Observable<Foo>");
        }
        return getParameterUpperBound(0, (ParameterizedType) returnType);
    }



}
