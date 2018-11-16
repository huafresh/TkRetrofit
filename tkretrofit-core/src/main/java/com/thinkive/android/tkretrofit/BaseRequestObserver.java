package com.thinkive.android.tkretrofit;

import android.content.Context;

import com.android.thinkive.framework.ThinkiveInitializer;

import io.reactivex.observers.DisposableObserver;

/**
 * Observer简单封装
 *
 * @author hua
 * @version 2018/6/20 11:22
 */

public abstract class BaseRequestObserver<T> extends DisposableObserver<T> {

    @Override
    public void onNext(T t) {
        onSuccess(ThinkiveInitializer.getInstance().getContext(), t);
    }

    @Override
    public void onError(Throwable t) {
        if (t instanceof BaseRequestException) {
            onFailed(ThinkiveInitializer.getInstance().getContext(), (BaseRequestException) t);
        } else {
            onFailed(ThinkiveInitializer.getInstance().getContext(), new BaseRequestException(t.getMessage()));
        }
    }

    @Override
    public void onComplete() {

    }

    protected abstract void onSuccess(Context context, T result);

    protected abstract void onFailed(Context context, BaseRequestException e);

    public static class EmptyObserver<T> extends BaseRequestObserver<T> {

        @Override
        protected void onSuccess(Context context, Object result) {

        }

        @Override
        protected void onFailed(Context context, BaseRequestException e) {

        }
    }
}
