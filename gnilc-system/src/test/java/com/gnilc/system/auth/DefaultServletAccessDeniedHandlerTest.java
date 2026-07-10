package com.gnilc.system.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.denied.AccessDeniedContext;
import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import com.gnilc.auth.authz.servlet.context.ServletAccessDeniedContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultServletAccessDeniedHandlerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultServletAccessDeniedHandler handler = new DefaultServletAccessDeniedHandler();

    @Test
    void writesHttp403WithAnIndependentAccessDeniedBusinessCode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletAccessDeniedContext deniedContext = servletContext(response);

        assertThat(handler.supports(null, deniedContext)).isTrue();
        handler.handle(null, deniedContext);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(ResponseCode.ACCESS_DENIED.getBusinessCode());
        assertThat(body.get("code").asInt()).isNotEqualTo(403);
        assertThat(body.get("error").asText()).isEqualTo("access denied");
        assertThat(body.get("message").asText()).isEqualTo("access denied");
        assertThat(body.get("data").isNull()).isTrue();
    }

    @Test
    void declinesNonServletNonHttpAndCommittedResponses() throws IOException {
        assertThat(handler.supports(null, new OtherDeniedContext())).isFalse();
        assertThat(handler.supports(null, servletContext(new NonHttpServletResponse()))).isFalse();

        MockHttpServletResponse committed = new MockHttpServletResponse();
        committed.getWriter().write("already written");
        committed.flushBuffer();
        assertThat(handler.supports(null, servletContext(committed))).isFalse();
    }

    private ServletAccessDeniedContext servletContext(ServletResponse response) {
        FilterChain chain = (request, chainResponse) -> { };
        return new ServletAccessDeniedContext(new MockHttpServletRequest(), response, chain);
    }

    private static final class OtherDeniedContext implements AccessDeniedContext {
    }

    private static final class NonHttpServletResponse implements ServletResponse {
        private final StringWriter body = new StringWriter();

        @Override
        public String getCharacterEncoding() {
            return "UTF-8";
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                }

                @Override
                public void write(int value) {
                    body.write(value);
                }
            };
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(body);
        }

        @Override
        public void setCharacterEncoding(String charset) {
        }

        @Override
        public void setContentLength(int length) {
        }

        @Override
        public void setContentLengthLong(long length) {
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
        public void setLocale(Locale locale) {
        }

        @Override
        public Locale getLocale() {
            return Locale.ROOT;
        }
    }
}
