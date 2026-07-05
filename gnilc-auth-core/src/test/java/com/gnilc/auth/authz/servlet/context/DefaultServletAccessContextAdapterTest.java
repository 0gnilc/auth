package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessEnvironment;
import com.gnilc.auth.authz.context.AccessIdentity;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultServletAccessContextAdapterTest {

    // Servlet adapter 只输出授权事实，不把 Servlet 对象放进 AccessContext。
    // TestCaseId: CORE-SERVLET-001
    @Test
    void adaptServletRequestToAccessContextWithoutServletObjects() {
        ServletAccessIdentityResolver identityResolver = context -> new AccessIdentity("1001", Map.of("anonymous", false));
        ServletAccessTargetResolver targetResolver = new DefaultServletAccessTargetResolver();
        DefaultServletAccessContextAdapter adapter = new DefaultServletAccessContextAdapter(identityResolver, targetResolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/admin/users/1");
        request.setContextPath("/app");
        request.setRequestURI("/app/admin/users/1");

        AccessContext context = adapter.adapt(servletRequestContext(request));

        assertThat(context.getEnvironment()).isEqualTo(AccessEnvironment.SERVLET);
        assertThat(context.getIdentity().getIdentifier()).isEqualTo("1001");
        assertThat(context.getTarget().getIdentifier()).isEqualTo("/admin/users/1");
        assertThat(context.getTarget().getQualifier()).isEqualTo("GET");
        assertThat(context.getTarget().getAttributes()).isEmpty();
        assertThat(context.getAttributes()).isEmpty();
    }

    // contextPath 不匹配时保留原始 URI，避免错误裁剪访问目标。
    // TestCaseId: CORE-SERVLET-002
    @Test
    void keepRequestUriWhenContextPathDoesNotMatch() {
        DefaultServletAccessContextAdapter adapter = new DefaultServletAccessContextAdapter(
                context -> new AccessIdentity(null, Map.of("anonymous", true)),
                new DefaultServletAccessTargetResolver()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/users");
        request.setContextPath("/app");
        request.setRequestURI("/admin/users");

        AccessContext context = adapter.adapt(servletRequestContext(request));

        assertThat(context.getTarget().getIdentifier()).isEqualTo("/admin/users");
        assertThat(context.getTarget().getQualifier()).isEqualTo("POST");
    }

    private ServletRequestContext servletRequestContext(MockHttpServletRequest request) {
        FilterChain chain = (chainRequest, chainResponse) -> {
        };
        return new ServletRequestContext(request, new MockHttpServletResponse(), chain);
    }
}
