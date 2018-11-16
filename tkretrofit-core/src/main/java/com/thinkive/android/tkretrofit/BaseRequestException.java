package com.thinkive.android.tkretrofit;

import android.text.TextUtils;

/**
 * @author hua
 * @version 2018/6/20 11:24
 */

public class BaseRequestException extends Exception {
    private String errorNo;
    private String errorInfo;

    public BaseRequestException(String errorInfo) {
        super(errorInfo);
        this.errorInfo = errorInfo;
    }

    public BaseRequestException(String errorNo, String errorInfo) {
        super(errorInfo);
        this.errorNo = errorNo;
        this.errorInfo = errorInfo;
    }

    public String getErrorNo() {
        return errorNo == null ? "" : errorNo;
    }

    public void setErrorNo(String errorNo) {
        this.errorNo = errorNo;
    }

    public String getErrorInfo() {
        if (TextUtils.isEmpty(errorInfo)) {
            return "未知异常";
        }
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }
}
