package com.gnilc.authz.system.config;

import com.gnilc.authz.context.AccessIdentity;
import com.gnilc.authz.denied.AccessDeniedHandler;
import com.gnilc.authz.rbac.context.AccessIdentityResolverDelegate;
import com.gnilc.authz.web.context.FilterDeniedContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
public class SystemAccessControlConfiguration {
    private static final String ACCESS_USER_ID_HEADER = "X-Access-User-Id";

    @Bean
    public AccessIdentityResolverDelegate<HttpServletRequest> systemAccessIdentityResolverDelegate() {
        return new AccessIdentityResolverDelegate<>() {
            @Override
            public boolean supports(HttpServletRequest request) {
                return StringUtils.isNotBlank(request.getHeader(ACCESS_USER_ID_HEADER));
            }

            @Override
            public AccessIdentity resolve(HttpServletRequest request) {
                String userId = request.getHeader(ACCESS_USER_ID_HEADER);
                return new AccessIdentity(userId.trim(), Map.of("header", ACCESS_USER_ID_HEADER));
            }
        };
    }

    @Bean
    @Primary
    public AccessDeniedHandler<FilterDeniedContext> systemAccessDeniedHandler() {
        return (context, deniedContext) -> {
            if (deniedContext.getResponse() instanceof HttpServletResponse response) {
                try {
                    writeForbiddenResponse(response);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to write access denied response", e);
                }
            }
        };
    }

    private void writeForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"access denied\",\"data\":null}");
    }
}
