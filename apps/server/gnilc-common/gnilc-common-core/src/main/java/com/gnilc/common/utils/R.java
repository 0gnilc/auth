package com.gnilc.common.utils;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.gnilc.common.constant.ResponseCode;
import lombok.Data;

@JsonPropertyOrder({"code", "data", "error", "message"})
@Data
public class R<T> {
    private Integer code;
    private T data;
    private String error;
    private String message;

    public R(Integer code, String message, T data) {
        this(code, data, null, message);
    }

    public R(Integer code, T data, String error, String message) {
        this.code = code;
        this.data = data;
        this.error = error;
        this.message = message;
    }

    public static <T> R<T> success(Integer code, String message, T data) {
        return new R<>(code, data, null, message);
    }

    public static <T> R<T> success(String message, T data) {
        return success(ResponseCode.SUCCESS.getCode(), message, data);
    }

    public static <T> R<T> success(T data) {
        return success(ResponseCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> success() {
        return success(ResponseCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> error(ResponseCode code, String error) {
        return new R<>(code.getCode(), null, error, error);
    }

    public static <T> R<T> error(Integer code, String error, T data) {
        return new R<>(code, data, error, error);
    }

    public static <T> R<T> error(Integer code, String error) {
        return error(code, error, null);
    }

    public static <T> R<T> error(String error) {
        return error(ResponseCode.ERROR, error);
    }
}
