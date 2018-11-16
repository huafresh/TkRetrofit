package com.thinkive.android.tkretrofit;

import com.android.thinkive.framework.network.ProtocolType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author hua
 * @version 2018/10/26 14:44
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SERVICE {
    /**
     * url可以是http://开头的，也可以是配置文件里配置的名称，比如：ZHYW_URL。
     */
    String url();

    ProtocolType protocolType() default ProtocolType.HTTP;

    String funcNo() default "";

    String bizCode() default "";

    boolean shouldCache() default false;

    long cacheTimeout() default 0;
}
