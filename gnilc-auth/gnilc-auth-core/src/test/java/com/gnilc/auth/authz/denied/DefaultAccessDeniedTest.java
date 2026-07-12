package com.gnilc.auth.authz.denied;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAccessDeniedTest {
    private final AccessContext context =
            new AccessContext(new AccessIdentity("9", null), new AccessTarget("/orders", "GET"));

    @Test
    void runsSupportedHandlersInOrder() {
        List<String> calls = new ArrayList<>();
        AccessDeniedContext deniedContext = new AccessDeniedContext() { };
        AccessDeniedHandler late = handler(20, true, "late", calls);
        AccessDeniedHandler skipped = handler(10, false, "skipped", calls);
        AccessDeniedHandler early = handler(0, true, "early", calls);

        new DefaultAccessDenied(List.of(late, skipped, early)).denied(context, deniedContext);

        assertThat(calls).containsExactly("early", "late");
        new DefaultAccessDenied(null).denied(context, deniedContext);
    }

    private AccessDeniedHandler handler(int order, boolean supports, String name, List<String> calls) {
        class OrderedHandler implements AccessDeniedHandler, Ordered {
            @Override
            public boolean supports(AccessContext context, AccessDeniedContext deniedContext) {
                return supports;
            }

            @Override
            public void handle(AccessContext context, AccessDeniedContext deniedContext) {
                calls.add(name);
            }

            @Override
            public int getOrder() {
                return order;
            }
        }
        return new OrderedHandler();
    }
}
