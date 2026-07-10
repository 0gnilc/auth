package com.gnilc.auth.authz.rbac.common.base;

import com.gnilc.auth.authz.rbac.exception.IllegalConditionException;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import com.gnilc.auth.authz.rbac.exception.UnknownErrorException;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Preconditions {
    private Preconditions() {
    }

    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new InvalidArgumentException();
        }
    }

    public static void checkArgument(boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw new InvalidArgumentException(String.valueOf(errorMessage));
        }
    }

    public static void checkCondition(boolean expression) {
        if (!expression) {
            throw new IllegalConditionException();
        }
    }

    public static void checkCondition(boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw new IllegalConditionException(String.valueOf(errorMessage));
        }
    }

    public static void checkError(boolean expression) {
        if (!expression) {
            throw new UnknownErrorException();
        }
    }

    public static void checkError(boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw new UnknownErrorException(String.valueOf(errorMessage));
        }
    }
}
