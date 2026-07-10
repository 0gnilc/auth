package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.context.AccessTarget;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultServletAccessTargetResolverTest {

    private final DefaultServletAccessTargetResolver resolver = new DefaultServletAccessTargetResolver();

    // Servlet 目标解析只裁剪 URI 前缀处的 contextPath，保留业务路径中相同片段。
    // TestCaseId: CORE-SERVLET-011
    @Test
    void removeContextPathOnlyFromRequestUriPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/admin/app/users");
        request.setContextPath("/app");
        request.setRequestURI("/app/admin/app/users");

        AccessTarget target = resolver.resolve(servletRequestContext(request));

        assertThat(target.getIdentifier()).isEqualTo("/admin/app/users");
        assertThat(target.getQualifier()).isEqualTo("GET");
        assertThat(target.getAttributes()).isEmpty();
    }

    // 请求 URI 不属于当前 contextPath 时应保留原 URI，避免误裁剪访问目标。
    // TestCaseId: CORE-SERVLET-012
    @Test
    void keepRequestUriWhenContextPathIsNotPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/users");
        request.setContextPath("/app");
        request.setRequestURI("/admin/users");

        AccessTarget target = resolver.resolve(servletRequestContext(request));

        assertThat(target.getIdentifier()).isEqualTo("/admin/users");
        assertThat(target.getQualifier()).isEqualTo("POST");
    }

    // 访问应用根路径时，裁剪 contextPath 后仍应输出稳定的根路径标识。
    // TestCaseId: CORE-SERVLET-013
    @Test
    void resolveRootPathWhenRequestUriEqualsContextPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app");
        request.setContextPath("/app");
        request.setRequestURI("/app");

        AccessTarget target = resolver.resolve(servletRequestContext(request));

        assertThat(target.getIdentifier()).isEqualTo("/");
        assertThat(target.getQualifier()).isEqualTo("GET");
    }

    private ServletRequestContext servletRequestContext(MockHttpServletRequest request) {
        return new ServletRequestContext(request, new MockHttpServletResponse(), (chainRequest, chainResponse) -> {
        });
    }
}
