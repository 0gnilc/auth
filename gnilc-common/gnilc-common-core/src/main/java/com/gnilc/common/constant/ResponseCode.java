package com.gnilc.common.constant;

import lombok.Getter;

/**
 * 响应体业务码。
 * <p>
 * 该 code 只表示 JSON 响应体中的业务结果，不表示 HTTP Status。
 */
@Getter
public enum ResponseCode {
    SUCCESS(0, "ok"),
    ERROR(10000, "error"),
    ARGUMENT_INVALID(10001, "argument invalid"),
    ILLEGAL_CONDITION(10002, "illegal condition"),
    AUTHENTICATION_FAILED(20001, "authentication failed"),
    UNAUTHORIZED(20002, "unauthorized"),
    ACCESS_DENIED(20003, "access denied");

    ResponseCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    private final Integer code;
    private final String message;
}
