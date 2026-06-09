package com.gnilc.authz.web.context;

import com.gnilc.authz.context.AccessContext;
import com.gnilc.authz.context.AccessIdentity;
import com.gnilc.authz.context.AccessIdentityResolver;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServletAccessContextAdapterTest {

    // Servlet adapter 只输出授权事实，不把 Servlet 对象放进 AccessContext。
    @Test
    void adaptServletRequestToAccessContextWithoutServletObjects() {
        AccessIdentityResolver<HttpServletRequest> identityResolver = request -> new AccessIdentity("1001", Map.of("anonymous", false));
        ServletAccessContextAdapter adapter = new ServletAccessContextAdapter(identityResolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/admin/users/1");
        request.setContextPath("/app");
        request.setRequestURI("/app/admin/users/1");

        AccessContext context = adapter.adapt(request);

        assertThat(context.getIdentity().getIdentifier()).isEqualTo("1001");
        assertThat(context.getTarget().getIdentifier()).isEqualTo("/admin/users/1");
        assertThat(context.getTarget().getQualifier()).isNull();
        assertThat(context.getTarget().getAttributes())
                .containsEntry("method", "GET")
                .containsEntry("rawUri", "/app/admin/users/1")
                .containsEntry("contextPath", "/app");
        assertThat(context.getAttributes()).containsEntry("source", "servlet");
    }

    // contextPath 不匹配时保留原始 URI，避免错误裁剪访问目标。
    @Test
    void keepRequestUriWhenContextPathDoesNotMatch() {
        ServletAccessContextAdapter adapter = new ServletAccessContextAdapter(request -> new AccessIdentity(null, Map.of("anonymous", true)));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/users");
        request.setContextPath("/app");
        request.setRequestURI("/admin/users");

        AccessContext context = adapter.adapt(request);

        assertThat(context.getTarget().getIdentifier()).isEqualTo("/admin/users");
        assertThat(context.getTarget().getAttributes()).containsEntry("method", "POST");
    }
}
