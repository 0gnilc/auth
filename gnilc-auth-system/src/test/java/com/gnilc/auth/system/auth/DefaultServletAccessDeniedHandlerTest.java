package com.gnilc.auth.system.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.denied.AccessDeniedContext;
import com.gnilc.auth.authz.denied.AccessDeniedHandler;
import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import com.gnilc.auth.authz.servlet.context.ServletAccessDeniedContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultServletAccessDeniedHandlerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // TestCaseId: SYS-ACCESS-DENIED-001
    @Test
    void writesSystemForbiddenJsonResponse() throws IOException {
        AccessDeniedHandler handler = new DefaultServletAccessDeniedHandler();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletAccessDeniedContext deniedContext = servletAccessDeniedContext(response);

        handler.handle(null, deniedContext);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        JsonNode body = OBJECT_MAPPER.readTree(response.getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(ResponseCode.ACCESS_DENIED.getBusinessCode());
        assertThat(body.get("data").isNull()).isTrue();
        assertThat(body.get("error").asText()).isEqualTo("access denied");
        assertThat(body.get("message").asText()).isEqualTo("access denied");
    }

    // TestCaseId: SYS-ACCESS-DENIED-002
    @Test
    void supportServletAccessDeniedContextWithHttpServletResponse() {
        DefaultServletAccessDeniedHandler handler = new DefaultServletAccessDeniedHandler();
        ServletAccessDeniedContext deniedContext = servletAccessDeniedContext(new MockHttpServletResponse());

        assertThat(handler.supports(null, deniedContext)).isTrue();
    }

    // TestCaseId: SYS-ACCESS-DENIED-003
    @Test
    void notSupportUnrelatedDeniedContext() {
        DefaultServletAccessDeniedHandler handler = new DefaultServletAccessDeniedHandler();

        assertThat(handler.supports(null, new TestAccessDeniedContext())).isFalse();
    }

    // TestCaseId: SYS-ACCESS-DENIED-004
    @Test
    void notSupportNonHttpServletResponse() {
        DefaultServletAccessDeniedHandler handler = new DefaultServletAccessDeniedHandler();
        ServletAccessDeniedContext deniedContext = servletAccessDeniedContext(new TestServletResponse());

        assertThat(handler.supports(null, deniedContext)).isFalse();
    }

    // TestCaseId: SYS-ACCESS-DENIED-005
    @Test
    void notSupportCommittedResponse() throws IOException {
        DefaultServletAccessDeniedHandler handler = new DefaultServletAccessDeniedHandler();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.getWriter().write("committed");
        response.flushBuffer();
        ServletAccessDeniedContext deniedContext = servletAccessDeniedContext(response);

        assertThat(handler.supports(null, deniedContext)).isFalse();
    }

    private ServletAccessDeniedContext servletAccessDeniedContext(ServletResponse response) {
        FilterChain chain = (request, chainResponse) -> {
        };
        return new ServletAccessDeniedContext(new MockHttpServletRequest(), response, chain);
    }

    private static class TestAccessDeniedContext implements AccessDeniedContext {
    }

    private static class TestServletResponse implements jakarta.servlet.ServletResponse {
        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public jakarta.servlet.ServletOutputStream getOutputStream() {
            return null;
        }

        @Override
        public java.io.PrintWriter getWriter() {
            return null;
        }

        @Override
        public void setCharacterEncoding(String charset) {
        }

        @Override
        public void setContentLength(int len) {
        }

        @Override
        public void setContentLengthLong(long len) {
        }

        @Override
        public void setContentType(String type) {
        }

        @Override
        public void setBufferSize(int size) {
        }

        @Override
        public int getBufferSize() {
            return 0;
        }

        @Override
        public void flushBuffer() {
        }

        @Override
        public void resetBuffer() {
        }

        @Override
        public boolean isCommitted() {
            return false;
        }

        @Override
        public void reset() {
        }

        @Override
        public void setLocale(java.util.Locale loc) {
        }

        @Override
        public java.util.Locale getLocale() {
            return null;
        }
    }
}
