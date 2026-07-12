package com.gnilc.system.auth;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.denied.AccessDeniedContext;
import com.gnilc.auth.authz.servlet.context.ServletAccessDeniedContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultServletAccessDeniedHandlerTest {
    @Test
    void writesJson403OnlyForOpenServletResponse() throws Exception {
        DefaultServletAccessDeniedHandler handler = new DefaultServletAccessDeniedHandler();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletAccessDeniedContext deniedContext = new ServletAccessDeniedContext(
                new MockHttpServletRequest(), response, (req, res) -> { });
        AccessContext access = new AccessContext(
                new AccessIdentity("1", Map.of()), new AccessTarget("/private", "GET"));

        assertThat(handler.supports(access, deniedContext)).isTrue();
        handler.handle(access, deniedContext);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("\"code\":20003", "\"error\":\"access denied\"");
        assertThat(handler.supports(access, new AccessDeniedContext() { })).isFalse();
    }
}
