package com.gnilc.common.exception;

public class UnknownErrorException extends RuntimeException {
    public UnknownErrorException() {
        this("系统异常");
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
