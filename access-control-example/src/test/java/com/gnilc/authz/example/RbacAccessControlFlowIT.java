package com.gnilc.authz.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
class RbacAccessControlFlowIT extends RbacHttpTestSupport {

    /**
     * Verifies protected RBAC grants, public access, and revoke behavior.
     */
    @Test
    void protectedEndpointRequiresUserPermissionAndPublicEndpointCanBeOpenedToAnonymousAccess() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long protectedPermissionId = 0L;
        long publicPermissionId = 0L;
        try {
            // Create RBAC fixtures through the management API.
            userId = createUser();
            roleId = createRole("it_flow_role_" + suffix, "Integration Flow Role " + suffix);
            protectedPermissionId = createPermission(
                    "it_flow_protected_" + suffix,
                    "Integration Flow Protected " + suffix,
                    "/example/protected/**",
                    false);
            publicPermissionId = createPermission(
                    "it_flow_public_" + suffix,
                    "Integration Flow Public " + suffix,
                    "/example/public/**",
                    true);
            updateRolePermission(roleId, List.of(protectedPermissionId));
            updateUserRole(userId, List.of(roleId));
            clearAllPermissionCache();

            // Verify anonymous, authorized, and public access decisions.
            JsonNode anonymousProtected = getJson("/example/protected/ping", null, FORBIDDEN.value());
            assertThat(anonymousProtected.path("message").asText()).isEqualTo("access denied");

            JsonNode authorizedProtected = getJson("/example/protected/ping", String.valueOf(userId), OK.value());
            assertThat(authorizedProtected.path("data").path("message").asText()).isEqualTo("protected pong");

            JsonNode anonymousPublic = getJson("/example/public/ping", null, OK.value());
            assertThat(anonymousPublic.path("data").path("message").asText()).isEqualTo("public pong");

            // Revoke the grant and confirm access closes again.
            updateRolePermission(roleId, List.of());
            clearAllPermissionCache();

            JsonNode revokedProtected = getJson("/example/protected/ping", String.valueOf(userId), FORBIDDEN.value());
            assertThat(revokedProtected.path("message").asText()).isEqualTo("access denied");
        } finally {
            cleanup(userId, roleId, protectedPermissionId, 0L);
            if (publicPermissionId > 0) {
                postOk("/authz/permission/remove/" + publicPermissionId, java.util.Map.of());
            }
        }
    }
}
