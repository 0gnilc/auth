package com.gnilc.authz.example.config;

import com.gnilc.authz.context.AccessIdentity;
import com.gnilc.authz.context.AccessIdentityResolver;
import com.gnilc.authz.denied.AccessDeniedHandler;
import com.gnilc.authz.web.context.FilterDeniedContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
@Profile({"dev", "localtest"})
public class ExampleAccessControlConfiguration {
    private static final String ACCESS_USER_ID_HEADER = "X-Access-User-Id";

    @Bean
    @Primary
    public AccessIdentityResolver<HttpServletRequest> exampleAccessIdentityResolver() {
        return request -> {
            String userId = request.getHeader(ACCESS_USER_ID_HEADER);
            if (StringUtils.isBlank(userId)) {
                return new AccessIdentity(null, Map.of("anonymous", true));
            }
            return new AccessIdentity(userId.trim(), Map.of("header", ACCESS_USER_ID_HEADER));
        };
    }

    @Bean
    @Primary
    public AccessDeniedHandler<FilterDeniedContext> exampleAccessDeniedHandler() {
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
