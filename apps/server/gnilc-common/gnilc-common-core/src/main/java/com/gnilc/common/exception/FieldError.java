package com.gnilc.common.exception;

/**
 * 字段校验错误响应。
 */
public record FieldError(String field, String code, String message) {
}
