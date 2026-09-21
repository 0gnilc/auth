package com.gnilc.common.exception;

/** 业务执行中未能归入参数或状态错误的异常，可保留底层失败原因。 */
public class UnknownErrorException extends RuntimeException {
    public UnknownErrorException() {
        this("An unexpected error occurred.");
    }

    public UnknownErrorException(String message) {
        super(message);
    }

    public UnknownErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownErrorException(Throwable cause) {
        super(cause);
    }

    public UnknownErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
