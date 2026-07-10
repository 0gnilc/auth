package com.gnilc.auth.authz.servlet.filter;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.servlet.context.ServletAccessContextAdapter;
import com.gnilc.auth.authz.servlet.context.ServletAccessDeniedContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServletAuthorizationFilterTest {

    // 授权通过时，Servlet filter 只负责继续执行后续链路。
    // TestCaseId: CORE-SERVLET-023
    @Test
    void continueFilterChainWhenAccessDecisionAllowsContext() throws Exception {
        AccessContext accessContext = accessContext();
        AtomicBoolean chainContinued = new AtomicBoolean(false);
        AtomicBoolean deniedHandled = new AtomicBoolean(false);
        ServletAuthorizationFilter filter = new ServletAuthorizationFilter(
                candidate -> true,
                request -> accessContext,
                (candidate, deniedContext) -> deniedHandled.set(true)
        );

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> chainContinued.set(true));

        assertThat(chainContinued).isTrue();
        assertThat(deniedHandled).isFalse();
    }

    // 授权拒绝时，Servlet filter 把授权事实和 Servlet 访问拒绝上下文交给 AccessDenied。
    // TestCaseId: CORE-SERVLET-024
    @Test
    void deniedAccessWithAccessContextAndServletAccessDeniedContext() throws Exception {
        AccessContext accessContext = accessContext();
        AtomicBoolean chainContinued = new AtomicBoolean(false);
        AtomicReference<AccessContext> deniedAccessContext = new AtomicReference<>();
        AtomicReference<ServletAccessDeniedContext> servletDeniedContext = new AtomicReference<>();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletAuthorizationFilter filter = new ServletAuthorizationFilter(
                candidate -> false,
                candidate -> accessContext,
                (candidate, deniedContext) -> {
                    deniedAccessContext.set(candidate);
                    servletDeniedContext.set((ServletAccessDeniedContext) deniedContext);
                }
        );

        filter.doFilter(request, response, (chainRequest, chainResponse) -> chainContinued.set(true));

        assertThat(chainContinued).isFalse();
        assertThat(deniedAccessContext).hasValue(accessContext);
        assertThat(servletDeniedContext.get().getRequest()).isSameAs(request);
        assertThat(servletDeniedContext.get().getResponse()).isSameAs(response);
        assertThat(servletDeniedContext.get().getChain()).isNotNull();
    }

    // 访问上下文适配器异常应直接暴露，不能误转为访问拒绝处理。
    // TestCaseId: CORE-SERVLET-025
    @Test
    void propagateAccessContextAdapterException() {
        IllegalStateException broken = new IllegalStateException("broken adapter");
        AtomicBoolean deniedHandled = new AtomicBoolean(false);
        ServletAuthorizationFilter filter = new ServletAuthorizationFilter(
                candidate -> true,
                request -> {
                    throw broken;
                },
                (candidate, deniedContext) -> deniedHandled.set(true)
        );

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
        })).isSameAs(broken);
        assertThat(deniedHandled).isFalse();
    }

    // 拒绝处理器异常应直接暴露，方便调用方和容器按失败请求处理。
    // TestCaseId: CORE-SERVLET-026
    @Test
    void propagateAccessDeniedException() {
        IllegalStateException broken = new IllegalStateException("broken denied handler");
        ServletAuthorizationFilter filter = new ServletAuthorizationFilter(
                candidate -> false,
                request -> accessContext(),
                (candidate, deniedContext) -> {
                    throw broken;
                }
        );

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
        })).isSameAs(broken);
    }

    // Servlet 授权过滤器的三段依赖都必须显式提供。
    // TestCaseId: CORE-SERVLET-027
    @Test
    void requireDecisionAdapterAndDeniedHandler() {
        ServletAccessContextAdapter adapter = request -> accessContext();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ServletAuthorizationFilter(null, adapter, (context, deniedContext) -> {
                }))
                .withMessage("accessDecision == null!");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ServletAuthorizationFilter(candidate -> true, null, (context, deniedContext) -> {
                }))
                .withMessage("accessContextAdapter == null!");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ServletAuthorizationFilter(candidate -> true, adapter, null))
                .withMessage("accessDenied == null!");
    }

    private AccessContext accessContext() {
        return new AccessContext(
                new AccessIdentity("1001", Map.of()),
                new AccessTarget("/admin/users", null, Map.of())
        );
    }
}
