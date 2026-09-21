package com.gnilc.common.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 字段校验错误响应。 */
@Data
@AllArgsConstructor
public class FieldError {
    /** 校验失败的字段路径。 */
    private String field;
    /** 字段校验错误码，用于区分校验规则。 */
    private String code;
    /** 对应字段的可读校验错误说明。 */
    private String message;
}
