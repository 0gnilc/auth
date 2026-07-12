package com.gnilc.auth.authn.servlet.handler;

import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultServletAuthenticationFailureHandlerTest {
    @Test
    void writesUnauthorizedResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletAuthenticationContext context =
                new ServletAuthenticationContext(new MockHttpServletRequest(), response);

        new DefaultServletAuthenticationFailureHandler().handle(context, AuthenticationResult.failed("expired"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("text/plain;charset=UTF-8");
        assertThat(response.getContentAsString()).isEqualTo("expired");
    }
}
