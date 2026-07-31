package com.gnilc.common.base;

import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreconditionsTest {
    @Test
    void usesDistinctExceptionTypes() {
        Object argument = null;
        Object state = null;

        assertThatThrownBy(() -> Preconditions.checkArgument(argument != null, "argument"))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("argument");
        assertThatThrownBy(() -> Preconditions.checkCondition(state != null, "state"))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("state");
    }
}
