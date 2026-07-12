package com.gnilc.system.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.denied.AccessDeniedContext;
import com.gnilc.auth.authz.denied.AccessDeniedHandler;
import com.gnilc.common.constant.ResponseCode;
import com.gnilc.common.utils.R;
import com.gnilc.auth.authz.servlet.context.ServletAccessDeniedContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 403 响应。
 */
@Component
public class DefaultServletAccessDeniedHandler implements AccessDeniedHandler {
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 仅处理尚未提交响应的 Servlet 访问拒绝上下文。
     */
    @Override
    public boolean supports(AccessContext accessContext, AccessDeniedContext deniedContext) {
        return deniedContext instanceof ServletAccessDeniedContext filterDeniedContext
                && filterDeniedContext.getResponse() instanceof HttpServletResponse response
                && !response.isCommitted();
    }

    /**
     * 处理授权拒绝。
     */
    @Override
    public void handle(AccessContext accessContext, AccessDeniedContext deniedContext) {
        if (deniedContext instanceof ServletAccessDeniedContext filterDeniedContext
                && filterDeniedContext.getResponse() instanceof HttpServletResponse response) {
            try {
                writeForbiddenResponse(response);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write access denied response", e);
            }
        }
    }

    /**
     * 写入 JSON 403 响应。
     */
    private void writeForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(JSON_CONTENT_TYPE);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(R.error(ResponseCode.ACCESS_DENIED, "access denied")));
    }
}
