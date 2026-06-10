package com.gnilc.authz.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.gnilc.authn.web.context.ServletAuthenticationContext;
import com.gnilc.authn.web.handler.AuthenticationHandler;
import com.gnilc.authn.web.handler.AuthenticationResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
@Import(AuthenticationBeforeAuthorizationIT.AuthenticationTestConfiguration.class)
class AuthenticationBeforeAuthorizationIT extends RbacHttpTestSupport {
    private static final String AUTHENTICATION_USER_ID_HEADER = "X-Authentication-User-Id";

    /**
     * Verifies optional authentication runs before authorization and does not replace authorization denial handling.
     */
    @Test
    void authenticationFilterRunsBeforeAuthorizationFilterAndRemainsOptional() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long protectedPermissionId = 0L;
        try {
            userId = createUser();
            roleId = createRole("it_authn_role_" + suffix, "Integration Authn Role " + suffix);
            protectedPermissionId = createPermission(
                    "it_authn_protected_" + suffix,
                    "Integration Authn Protected " + suffix,
                    "/example/protected/**",
                    false);
            updateRolePermission(roleId, List.of(protectedPermissionId));
            updateUserRole(userId, List.of(roleId));
            clearAllPermissionCache();

            JsonNode authenticated = getWithAuthenticationHeader("/example/protected/ping", String.valueOf(userId), OK.value());
            assertThat(authenticated.path("data").path("message").asText()).isEqualTo("protected pong");

            MvcResult invalidCredential = mockMvc.perform(get("/api/example/protected/ping")
                            .contextPath("/api")
                            .header(AUTHENTICATION_USER_ID_HEADER, "not-a-user-id"))
                    .andExpect(status().is(UNAUTHORIZED.value()))
                    .andReturn();
            assertThat(invalidCredential.getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain("access denied");

            JsonNode anonymous = getWithAuthenticationHeader("/example/protected/ping", null, FORBIDDEN.value());
            assertThat(anonymous.path("message").asText()).isEqualTo("access denied");
        } finally {
            cleanup(userId, roleId, protectedPermissionId, 0L);
        }
    }

    private JsonNode getWithAuthenticationHeader(String path, String userId, int expectedStatus) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request = get("/api" + path)
                .contextPath("/api");
        if (userId != null) {
            request.header(AUTHENTICATION_USER_ID_HEADER, userId);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AuthenticationTestConfiguration {
        @Bean
        AuthenticationHandler headerAuthenticationHandler() {
            return new AuthenticationHandler() {
                @Override
                public boolean supports(ServletAuthenticationContext context) {
                    return hasText(context.getRequest().getHeader(AUTHENTICATION_USER_ID_HEADER));
                }

                @Override
                public AuthenticationResult authenticate(ServletAuthenticationContext context) {
                    String userId = context.getRequest().getHeader(AUTHENTICATION_USER_ID_HEADER).trim();
                    if (!isNumeric(userId)) {
                        return AuthenticationResult.failed("invalid authentication user id");
                    }
                    return AuthenticationResult.authenticated(() -> userId);
                }
            };
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }

        private static boolean isNumeric(String value) {
            for (int i = 0; i < value.length(); i++) {
                if (!Character.isDigit(value.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }
}
