package com.paratera.sgri.http;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * http请求返回的数据， 异常， 数据， 状态等
 */
public class HttpResponse {

    public HttpResponse() {
    }

    public HttpResponse(String data) {
        this.data = data;
    }

    /**
     * 请求地址
     */
    private String url;

    /**
     * 发送的参数
     */
    private String params;

    /**
     * 请求是否发生异常
     */
    private boolean success = false;

    /**
     * http 响应码
     */
    private int statusCode;

    /**
     * 错误原因
     */
    private String errorMsg;

    /**
     * 字符串类型的数据
     */
    private String data;

    /**
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url 要设置的 url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return params
     */
    public String getParams() {
        return params;
    }

    /**
     * @param params 要设置的 params
     */
    public void setParams(String params) {
        this.params = params;
    }

    /**
     * 发现网络异常和响应码不为空
     */
    public boolean isSuccess() {
        if (errorMsg == null) {
            success = true;
        }

        if (statusCode == 400 || statusCode == 404) {
            success = false;
        }
        return success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        isSuccess();
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
