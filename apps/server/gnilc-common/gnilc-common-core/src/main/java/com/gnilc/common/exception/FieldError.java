package com.gnilc.common.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 字段校验错误响应。
 */
@Data
@AllArgsConstructor
public class FieldError {
    private String field;
    private String code;
    private String message;
}
