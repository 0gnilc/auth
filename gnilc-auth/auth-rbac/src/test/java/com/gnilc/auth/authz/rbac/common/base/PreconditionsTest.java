package com.gnilc.auth.authz.rbac.common.base;

import com.gnilc.auth.authz.rbac.exception.IllegalConditionException;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreconditionsTest {
    @Test
    void usesDistinctExceptionTypes() {
        assertThatThrownBy(() -> Preconditions.checkArgument(false, "argument"))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("argument");
        assertThatThrownBy(() -> Preconditions.checkCondition(false, "state"))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("state");
    }
}
