package com.gnilc.authn.web.handler;

import com.gnilc.authn.web.context.ServletAuthenticationContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAuthenticationFailureHandlerTest {

    // 默认认证失败处理器返回 401，并把认证失败原因写入响应体。
    @Test
    void writeFailureReasonToUnauthorizedResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        DefaultAuthenticationFailureHandler handler = new DefaultAuthenticationFailureHandler();

        handler.handle(
                new ServletAuthenticationContext(new MockHttpServletRequest(), response),
                AuthenticationResult.failed("bad credential")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(response.getContentType()).isEqualTo("text/plain;charset=UTF-8");
        assertThat(response.getContentAsString()).isEqualTo("bad credential");
    }

    // 没有明确失败原因时，默认返回通用认证失败消息。
    @Test
    void writeDefaultMessageWhenFailureReasonIsBlank() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        DefaultAuthenticationFailureHandler handler = new DefaultAuthenticationFailureHandler();

        handler.handle(
                new ServletAuthenticationContext(new MockHttpServletRequest(), response),
                AuthenticationResult.failed(" ")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).isEqualTo("authentication failed");
    }
}
