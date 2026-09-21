package com.gnilc.common.utils;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.gnilc.common.constant.ResponseCode;
import lombok.Data;

/** 统一业务响应；{@code code} 表达响应体业务结果，独立于 HTTP 状态。 */
@JsonPropertyOrder({"code", "data", "error", "message"})
@Data
public class R<T> {
    /** 业务结果码；约定值见 {@link ResponseCode}，不等同于 HTTP 状态码。 */
    private Integer code;
    /** 业务返回数据；无返回内容或通常的错误响应中为 {@code null}。 */
    private T data;
    /** 错误说明；成功响应为 {@code null}。 */
    private String error;
    /** 响应说明；错误工厂方法使用与 {@code error} 相同的内容。 */
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
