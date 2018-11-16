package com.thinkive.android.tkretrofit;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 批量添加静态参数。使用方法参考：{@link retrofit2.http.Headers}
 *
 * @author hua
 * @version 2018/11/2 16:04
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Params {
    String[] value();
}
