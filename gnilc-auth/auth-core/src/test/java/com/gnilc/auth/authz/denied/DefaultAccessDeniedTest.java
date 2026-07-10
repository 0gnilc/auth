package com.gnilc.auth.authz.denied;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class DefaultAccessDeniedTest {

    // TestCaseId: CORE-AUTHZ-039
    @Test
    void noOpWhenHandlersAreEmpty() {
        DefaultAccessDenied accessDenied = new DefaultAccessDenied(List.of());

        accessDenied.denied(accessContext(), new TestAccessDeniedContext());
    }

    // TestCaseId: CORE-AUTHZ-040
    @Test
    void skipUnsupportedHandlers() {
        AtomicBoolean called = new AtomicBoolean(false);
        AccessDeniedHandler handler = new AccessDeniedHandler() {
            @Override
            public boolean supports(AccessContext accessContext, AccessDeniedContext deniedContext) {
                return false;
            }

            @Override
            public void handle(AccessContext accessContext, AccessDeniedContext deniedContext) {
                called.set(true);
            }
        };
        DefaultAccessDenied accessDenied = new DefaultAccessDenied(List.of(handler));

        accessDenied.denied(accessContext(), new TestAccessDeniedContext());

        assertThat(called).isFalse();
    }

    // TestCaseId: CORE-AUTHZ-041
    @Test
    void callAllSupportedHandlers() {
        List<String> called = new ArrayList<>();
        AccessDeniedHandler first = (context, deniedContext) -> called.add("first");
        AccessDeniedHandler second = (context, deniedContext) -> called.add("second");
        DefaultAccessDenied accessDenied = new DefaultAccessDenied(List.of(first, second));

        accessDenied.denied(accessContext(), new TestAccessDeniedContext());

        assertThat(called).containsExactly("first", "second");
    }

    // TestCaseId: CORE-AUTHZ-042
    @Test
    void callHandlersBySpringOrder() {
        List<String> called = new ArrayList<>();
        AccessDeniedHandler later = orderedHandler(20, "later", called);
        AccessDeniedHandler earlier = orderedHandler(10, "earlier", called);
        DefaultAccessDenied accessDenied = new DefaultAccessDenied(List.of(later, earlier));

        accessDenied.denied(accessContext(), new TestAccessDeniedContext());

        assertThat(called).containsExactly("earlier", "later");
    }

    // TestCaseId: CORE-AUTHZ-043
    @Test
    void passSameContextAndDeniedContextToSupportsAndHandle() {
        AccessContext accessContext = accessContext();
        AccessDeniedContext deniedContext = new TestAccessDeniedContext();
        AtomicReference<AccessContext> supportsContext = new AtomicReference<>();
        AtomicReference<AccessDeniedContext> supportsDeniedContext = new AtomicReference<>();
        AtomicReference<AccessContext> handledContext = new AtomicReference<>();
        AtomicReference<AccessDeniedContext> handledDeniedContext = new AtomicReference<>();
        AccessDeniedHandler handler = new AccessDeniedHandler() {
            @Override
            public boolean supports(AccessContext candidateAccessContext, AccessDeniedContext candidateDeniedContext) {
                supportsContext.set(candidateAccessContext);
                supportsDeniedContext.set(candidateDeniedContext);
                return true;
            }

            @Override
            public void handle(AccessContext candidateAccessContext, AccessDeniedContext candidateDeniedContext) {
                handledContext.set(candidateAccessContext);
                handledDeniedContext.set(candidateDeniedContext);
            }
        };
        DefaultAccessDenied accessDenied = new DefaultAccessDenied(List.of(handler));

        accessDenied.denied(accessContext, deniedContext);

        assertThat(supportsContext).hasValue(accessContext);
        assertThat(supportsDeniedContext).hasValue(deniedContext);
        assertThat(handledContext).hasValue(accessContext);
        assertThat(handledDeniedContext).hasValue(deniedContext);
    }

    // TestCaseId: CORE-AUTHZ-044
    @Test
    void propagateHandlerException() {
        DefaultAccessDenied accessDenied = new DefaultAccessDenied(List.of((context, deniedContext) -> {
            throw new IllegalStateException("boom");
        }));

        assertThatIllegalStateException()
                .isThrownBy(() -> accessDenied.denied(accessContext(), new TestAccessDeniedContext()))
                .withMessage("boom");
    }

    private AccessDeniedHandler orderedHandler(int order, String name, List<String> called) {
        return new OrderedAccessDeniedHandler(order, name, called);
    }

    private AccessContext accessContext() {
        return new AccessContext(
                new AccessIdentity("1001", Map.of()),
                new AccessTarget("/admin/users", null, Map.of())
        );
    }

    private static class OrderedAccessDeniedHandler implements AccessDeniedHandler, Ordered {
        private final int order;
        private final String name;
        private final List<String> called;

        private OrderedAccessDeniedHandler(int order, String name, List<String> called) {
            this.order = order;
            this.name = name;
            this.called = called;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public void handle(AccessContext accessContext, AccessDeniedContext deniedContext) {
            called.add(name);
        }
    }

    private static class TestAccessDeniedContext implements AccessDeniedContext {
    }
}
