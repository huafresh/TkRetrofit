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

    /**
     * 是否缓存，这里的缓存不是根据http响应头来处理的。
     * 如果设为true，那么每次请求返回的数据都是上一次请求的结果。
     * PS：即使缓存命中，请求依然会发，下次请求生效。
     *
     * @return 是否缓存
     */
    boolean shouldCache() default false;
}
