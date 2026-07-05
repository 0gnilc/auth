package com.gnilc.auth.authz.rbac.event;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class RbacAuthzEventTest {
    /**
     * Verifies default void events use the minimum required type.
     */
    // TestCaseId: RBAC-EVENT-001
    @Test
    void createDefaultVoidEventWithMinimumType() {
        RbacAuthzEvent<Void> event = RbacAuthzEvent.of(RbacAuthzEvent.Type.ALL);

        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.ALL);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.DEFAULT);
        assertThat(event.getData()).isNull();
        assertThat(event.getExtra()).isNull();
        assertThat(event.getResolvableType().getGeneric(0).resolve()).isEqualTo(Void.class);
    }

    /**
     * Verifies void events preserve an explicit action.
     */
    // TestCaseId: RBAC-EVENT-002
    @Test
    void createVoidEventWithExplicitAction() {
        RbacAuthzEvent<Void> event = RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ALL,
                RbacAuthzEvent.Action.CLEAR);

        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.ALL);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.CLEAR);
        assertThat(event.getData()).isNull();
        assertThat(event.getResolvableType().getGeneric(0).resolve()).isEqualTo(Void.class);
    }

    /**
     * Verifies event generic type inference from data.
     */
    // TestCaseId: RBAC-EVENT-003
    @Test
    void inferResolvableGenericFromData() {
        RbacAuthzEvent<Long> event = RbacAuthzEvent.of(
                RbacAuthzEvent.Type.USER,
                RbacAuthzEvent.Action.DELETE,
                100L);

        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.USER);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.DELETE);
        assertThat(event.getData()).isEqualTo(100L);
        assertThat(event.getResolvableType().getGeneric(0).resolve()).isEqualTo(Long.class);
    }

    /**
     * Verifies event data and extra context remain mutable.
     */
    // TestCaseId: RBAC-EVENT-004
    @Test
    void exposeMutableDataAndExtraContext() {
        RbacAuthzEvent<Long> event = RbacAuthzEvent.of(
                RbacAuthzEvent.Type.USER_ROLE,
                RbacAuthzEvent.Action.REPLACE,
                100L);

        event.setData(200L);
        event.setExtra(List.of(1L, 2L));

        assertThat(event.getData()).isEqualTo(200L);
        assertThat(event.getExtra()).isEqualTo(List.of(1L, 2L));
        assertThat(event.getResolvableType().getGeneric(0).resolve()).isEqualTo(Long.class);
    }

    /**
     * Verifies event factory methods reject null required fields.
     */
    // TestCaseId: RBAC-EVENT-005
    @Test
    void rejectNullRequiredFields() {
        assertThatNullPointerException()
                .isThrownBy(() -> RbacAuthzEvent.of(null))
                .withMessage("type must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> RbacAuthzEvent.of(RbacAuthzEvent.Type.USER, null))
                .withMessage("action must not be null");
    }
}
