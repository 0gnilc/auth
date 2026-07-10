package com.gnilc.auth.authz.rbac.common.utils;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Preconditions;
import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import lombok.Data;

@JsonPropertyOrder({"code", "data", "error", "message"})
@Data
public class R<T> {
    private Integer code;
    private T data;
    private String error;
    private String message;

    public R(Integer businessCode, String message, T data) {
        this(businessCode, data, null, message);
    }

    public R(Integer businessCode, T data, String error, String message) {
        validateBusinessCode(businessCode);
        this.code = businessCode;
        this.data = data;
        this.error = error;
        this.message = message;
    }

    public static <T> R<T> success(Integer businessCode, String message, T data) {
        return new R<>(businessCode, data, null, message);
    }

    public static <T> R<T> success(String message, T data) {
        return success(ResponseCode.SUCCESS.getBusinessCode(), message, data);
    }

    public static <T> R<T> success(T data) {
        return success(ResponseCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> success() {
        return success(ResponseCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> error(ResponseCode code, String error) {
        return new R<>(code.getBusinessCode(), null, error, error);
    }

    public static <T> R<T> error(Integer businessCode, String error, T data) {
        return new R<>(businessCode, data, error, error);
    }

    public static <T> R<T> error(Integer businessCode, String error) {
        return error(businessCode, error, null);
    }

    public static <T> R<T> error(String error) {
        return error(ResponseCode.ERROR, error);
    }

    private static void validateBusinessCode(Integer businessCode) {
        Preconditions.checkArgument(businessCode != null, "businessCode == null");
        Preconditions.checkArgument(businessCode == 0 || businessCode >= 10000,
                "businessCode must be 0 or >= 10000");
    }
}
