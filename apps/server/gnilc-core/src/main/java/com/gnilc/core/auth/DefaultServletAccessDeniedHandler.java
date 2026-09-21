package com.gnilc.core.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.denied.AccessDeniedContext;
import com.gnilc.auth.authz.denied.AccessDeniedHandler;
import com.gnilc.common.constant.ResponseCode;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.utils.R;
import com.gnilc.auth.authz.servlet.context.ServletAccessDeniedContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 将授权拒绝转换为本地化 HTTP 403 响应；与凭据认证失败的 401 分流。
 */
@Component
public class DefaultServletAccessDeniedHandler implements AccessDeniedHandler {
    private static final String ACCESS_DENIED_MESSAGE = "system.auth.access.denied";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final I18nMessageService i18nMessageService;
    private final LocaleResolver localeResolver;

    public DefaultServletAccessDeniedHandler(I18nMessageService i18nMessageService, LocaleResolver localeResolver) {
        this.i18nMessageService = i18nMessageService;
        this.localeResolver = localeResolver;
    }

    /**
     * 仅处理尚未提交响应的 Servlet 访问拒绝上下文。
     */
    @Override
    public boolean supports(AccessContext accessContext, AccessDeniedContext deniedContext) {
        return deniedContext instanceof ServletAccessDeniedContext filterDeniedContext
                && filterDeniedContext.getRequest() instanceof HttpServletRequest
                && filterDeniedContext.getResponse() instanceof HttpServletResponse response
                && !response.isCommitted();
    }

    /**
     * 处理授权拒绝。
     */
    @Override
    public void handle(AccessContext accessContext, AccessDeniedContext deniedContext) {
        if (deniedContext instanceof ServletAccessDeniedContext filterDeniedContext
                && filterDeniedContext.getRequest() instanceof HttpServletRequest request
                && filterDeniedContext.getResponse() instanceof HttpServletResponse response) {
            try {
                writeForbiddenResponse(
                        response,
                        i18nMessageService.get(ACCESS_DENIED_MESSAGE,
                                localeResolver.resolveLocale(request)));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write access denied response", e);
            }
        }
    }

    /**
     * 写入 JSON 403 响应。
     */
    private void writeForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(JSON_CONTENT_TYPE);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(R.error(ResponseCode.ACCESS_DENIED, message)));
    }
}
