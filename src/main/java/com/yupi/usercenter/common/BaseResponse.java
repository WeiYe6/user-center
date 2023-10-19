package com.yupi.usercenter.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 *
 * @param <T>
 * @author 曾伟业
 */
@Data
public class BaseResponse<T> implements Serializable {
    private static final long serialVersionUID = 5984260444884156805L;

    private int code;           //响应状态码
    private T data;             //响应数据
    private String message;     //消息
    private String description; //详细描述

    public BaseResponse(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    public BaseResponse(int code, T data, String message) {
        this(code, data, message, "");
    }

    public BaseResponse(int code, T data) {
        this(code, data, "", "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage(), errorCode.getDescription());
    }
}
