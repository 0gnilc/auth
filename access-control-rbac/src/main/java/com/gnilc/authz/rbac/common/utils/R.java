package com.gnilc.authz.rbac.common.utils;


import com.gnilc.authz.rbac.common.constant.ResponseCode;
import lombok.Data;

@Data
public class R<T> {
    private Integer code;
    private String message;
    private T data;

    public R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> success(Integer code, String message, T data) {
        return new R<>(code, message, data);
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

    public static <T> R<T> error(Integer code, String message, T data) {
        return new R<>(code, message, data);
    }

    public static <T> R<T> error(Integer code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> error(String message) {
        return error(ResponseCode.ERROR.getCode(), message);
    }
}
