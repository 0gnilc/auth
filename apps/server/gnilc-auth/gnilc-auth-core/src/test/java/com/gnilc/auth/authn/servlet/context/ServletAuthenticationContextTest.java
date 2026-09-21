package com.gnilc.auth.authn.servlet.context;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 验证认证上下文不能缺少请求或响应，防止不完整 Servlet 边界进入处理器。 */
class ServletAuthenticationContextTest {
    @Test
    void rejectsMissingRequestOrResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> new ServletAuthenticationContext(null, response))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ServletAuthenticationContext(request, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
