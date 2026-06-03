package com.gnilc.authz.rbac.exception;

public class IllegalConditionException extends RuntimeException{
    public IllegalConditionException() {
    }

    public IllegalConditionException(String message) {
        super(message);
    }

    public IllegalConditionException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalConditionException(Throwable cause) {
        super(cause);
    }

    public IllegalConditionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
