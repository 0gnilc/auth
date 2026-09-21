package com.gnilc.common.exception;

/** 当前业务状态或条件不满足操作要求时抛出的异常。 */
public class IllegalConditionException extends RuntimeException {
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
