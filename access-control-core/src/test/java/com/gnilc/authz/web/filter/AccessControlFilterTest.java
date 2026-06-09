package com.gnilc.authz.web.filter;

import com.gnilc.authz.context.AccessContext;
import com.gnilc.authz.context.AccessIdentity;
import com.gnilc.authz.context.AccessTarget;
import com.gnilc.authz.web.context.FilterDeniedContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AccessControlFilterTest {

    // 授权通过时，filter 只负责继续执行后续链路。
    @Test
    void continueFilterChainWhenAccessDecisionAllowsContext() throws Exception {
        AccessContext context = accessContext();
        AtomicBoolean chainContinued = new AtomicBoolean(false);
        AtomicBoolean deniedHandled = new AtomicBoolean(false);
        AccessControlFilter filter = new AccessControlFilter(
                request -> context,
                candidate -> true,
                (candidate, deniedContext) -> deniedHandled.set(true)
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> chainContinued.set(true));

        assertThat(chainContinued).isTrue();
        assertThat(deniedHandled).isFalse();
    }

    // 授权拒绝时，filter 把授权事实和 Web 拒绝上下文交给 handler。
    @Test
    void handleDeniedAccessWithAccessContextAndFilterContext() throws Exception {
        AccessContext context = accessContext();
        AtomicBoolean chainContinued = new AtomicBoolean(false);
        AtomicReference<AccessContext> handledContext = new AtomicReference<>();
        AtomicReference<FilterDeniedContext> handledDeniedContext = new AtomicReference<>();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessControlFilter filter = new AccessControlFilter(
                candidate -> context,
                candidate -> false,
                (candidate, deniedContext) -> {
                    handledContext.set(candidate);
                    handledDeniedContext.set(deniedContext);
                }
        );

        filter.doFilter(request, response, (chainRequest, chainResponse) -> chainContinued.set(true));

        assertThat(chainContinued).isFalse();
        assertThat(handledContext).hasValue(context);
        assertThat(handledDeniedContext.get().getRequest()).isSameAs(request);
        assertThat(handledDeniedContext.get().getResponse()).isSameAs(response);
        assertThat(handledDeniedContext.get().getChain()).isNotNull();
    }

    private AccessContext accessContext() {
        return new AccessContext(
                new AccessIdentity("1001", Map.of()),
                new AccessTarget("/admin/users", null, Map.of()),
                Map.of()
        );
    }
}
