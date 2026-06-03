package com.gnilc.authz.rbac.common.constant;

public enum ResponseCode {
    SUCCESS(200, "success"),
    ERROR(500, "error"),
    ARGUMENT_INVALID(400, "argument invalid"),
    ILLEGAL_CONDITION(410, "illegal condition");

    ResponseCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private Integer code;
    private String message;
}
