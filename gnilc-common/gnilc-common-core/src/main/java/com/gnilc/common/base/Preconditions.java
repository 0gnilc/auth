package com.gnilc.common.base;

import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.exception.UnknownErrorException;

public class Preconditions {
    private Preconditions() {
    }

    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new InvalidArgumentException();
        }
    }

    public static void checkArgument(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new InvalidArgumentException(String.valueOf(errorMessage));
        }
    }

    public static void checkCondition(boolean expression) {
        if (!expression) {
            throw new IllegalConditionException();
        }
    }

    public static void checkCondition(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalConditionException(String.valueOf(errorMessage));
        }
    }

    public static void checkError(boolean expression) {
        if (!expression) {
            throw new UnknownErrorException();
        }
    }

    public static void checkError(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new UnknownErrorException(String.valueOf(errorMessage));
        }
    }
}
