package com.gnilc.common.exception;

/** 请求参数不满足输入契约时抛出的异常。 */
public class InvalidArgumentException extends RuntimeException {
    public InvalidArgumentException() {
    }

    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidArgumentException(Throwable cause) {
        super(cause);
    }

    public InvalidArgumentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
