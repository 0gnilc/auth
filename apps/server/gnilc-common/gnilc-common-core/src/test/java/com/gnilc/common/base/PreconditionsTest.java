package com.gnilc.common.base;

import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
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
