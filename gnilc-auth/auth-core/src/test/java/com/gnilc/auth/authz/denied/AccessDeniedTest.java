package com.gnilc.auth.authz.denied;

import com.gnilc.auth.authz.context.AccessContext;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AccessDeniedTest {

    @Test
    void defaultDeniedEntryInvokesEverySupportingHandlerInSpringOrder() {
        AccessContext accessContext = mock(AccessContext.class);
        AccessDeniedContext deniedContext = mock(AccessDeniedContext.class);
        List<String> calls = new ArrayList<>();
        DefaultAccessDenied accessDenied = new DefaultAccessDenied(List.of(
                new LastHandler(calls),
                new UnsupportedHandler(calls),
                new FirstHandler(calls)
        ));

        accessDenied.denied(accessContext, deniedContext);

        assertThat(calls).containsExactly("first", "last");
    }

    @Test
    void missingHandlersAreANoOp() {
        DefaultAccessDenied accessDenied = new DefaultAccessDenied(null);

        accessDenied.denied(mock(AccessContext.class), mock(AccessDeniedContext.class));
    }

    private abstract static class RecordingHandler implements AccessDeniedHandler {
        private final List<String> calls;
        private final String name;

        private RecordingHandler(List<String> calls, String name) {
            this.calls = calls;
            this.name = name;
        }

        @Override
        public boolean supports(AccessContext accessContext, AccessDeniedContext deniedContext) {
            return true;
        }

        @Override
        public void handle(AccessContext accessContext, AccessDeniedContext deniedContext) {
            calls.add(name);
        }
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    private static final class FirstHandler extends RecordingHandler {
        private FirstHandler(List<String> calls) {
            super(calls, "first");
        }
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    private static final class LastHandler extends RecordingHandler {
        private LastHandler(List<String> calls) {
            super(calls, "last");
        }
    }

    private static final class UnsupportedHandler extends RecordingHandler {
        private UnsupportedHandler(List<String> calls) {
            super(calls, "unsupported");
        }

        @Override
        public boolean supports(AccessContext accessContext, AccessDeniedContext deniedContext) {
            return false;
        }
    }
}
