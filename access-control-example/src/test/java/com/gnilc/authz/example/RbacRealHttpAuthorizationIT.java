package com.gnilc.authz.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("localtest")
class RbacRealHttpAuthorizationIT extends RbacRealHttpTestSupport {

    /**
     * Verifies anonymous users are denied for non-public protected permissions.
     */
    @Test
    void protectedEndpointRejectsAnonymousWhenNonPublicPermissionExistsThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long permissionId = 0L;
        try {
            permissionId = createPermission("it_auth_anonymous_protected_" + suffix,
                    "Anonymous Protected " + suffix, "/example/protected/**", false);
            clearAllPermissionCache();

            assertProtectedPingDenied(null);
            recordScenario("Authorization / GET /example/protected/ping",
                    "anonymous; required permission target=/example/protected/**; publicAccess=false",
                    "GET /api/example/protected/ping",
                    "HTTP 403; code=403; message=access denied; data=null");
        } finally {
            cleanupQuietly(0L, 0L, permissionId, 0L);
        }
    }

    /**
     * Verifies a user without roles is denied protected access.
     */
    @Test
    void protectedEndpointRejectsUserWithoutRoleThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long permissionId = 0L;
        try {
            userId = createUser();
            permissionId = createPermission("it_auth_user_without_role_" + suffix,
                    "User Without Role " + suffix, "/example/protected/**", false);
            clearAllPermissionCache();

            assertProtectedPingDenied(String.valueOf(userId));
            recordScenario("Authorization / GET /example/protected/ping",
                    "X-Access-User-Id=" + userId + "; user has no roles; required permission target=/example/protected/**",
                    "GET /api/example/protected/ping",
                    "HTTP 403; code=403; message=access denied; data=null");
        } finally {
            cleanupQuietly(userId, 0L, permissionId, 0L);
        }
    }

    /**
     * Verifies a user with a matching role permission is allowed.
     */
    @Test
    void protectedEndpointAllowsUserWithRolePermissionThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long permissionId = 0L;
        try {
            userId = createUser();
            roleId = createRole("it_auth_allow_role_" + suffix, "Authorization Allow Role " + suffix);
            permissionId = createPermission("it_auth_allow_permission_" + suffix,
                    "Authorization Allow Permission " + suffix, "/example/protected/**", false);
            updateRolePermission(roleId, List.of(permissionId));
            updateUserRole(userId, List.of(roleId));
            clearAllPermissionCache();

            assertProtectedPingAllowed(userId);
            recordScenario("Authorization / GET /example/protected/ping",
                    "X-Access-User-Id=" + userId + "; roleId=" + roleId + "; permission target=/example/protected/**",
                    "GET /api/example/protected/ping",
                    "HTTP 200; code=200; data.message=protected pong");
        } finally {
            cleanupQuietly(userId, roleId, permissionId, 0L);
        }
    }

    /**
     * Verifies role-permission changes affect authorization without manual cache clearing.
     */
    @Test
    void rolePermissionGrantAndRevokeChangeAuthorizationWithoutManualCacheClearThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long permissionId = 0L;
        try {
            userId = createUser();
            roleId = createRole("it_auth_revoke_role_" + suffix, "Authorization Revoke Role " + suffix);
            permissionId = createPermission("it_auth_revoke_permission_" + suffix,
                    "Authorization Revoke Permission " + suffix, "/example/protected/**", false);
            updateUserRole(userId, List.of(roleId));
            clearAllPermissionCache();

            // Start denied before granting the role permission.
            assertProtectedPingDenied(String.valueOf(userId));
            recordScenario("Authorization cache / GET /example/protected/ping",
                    "before grant; X-Access-User-Id=" + userId + "; role has no permissions",
                    "GET /api/example/protected/ping",
                    "HTTP 403 before grant; code=403; message=access denied");

            // Granting the permission should reopen access immediately.
            updateRolePermission(roleId, List.of(permissionId));
            assertProtectedPingAllowed(userId);
            recordScenario("Authorization cache / POST /authz/role-permission/update",
                    "grant permissionId=" + permissionId + " to roleId=" + roleId + "; no manual clear-all",
                    "GET /api/example/protected/ping after grant",
                    "HTTP 200 after grant; code=200; data.message=protected pong");

            // Revoking the permission should close access immediately.
            updateRolePermission(roleId, List.of());
            assertProtectedPingDenied(String.valueOf(userId));
            recordScenario("Authorization cache / POST /authz/role-permission/update",
                    "clear permissions for roleId=" + roleId + "; no manual clear-all",
                    "GET /api/example/protected/ping after revoke",
                    "HTTP 403 after revoke; code=403; message=access denied");
        } finally {
            cleanupQuietly(userId, roleId, permissionId, 0L);
        }
    }

    /**
     * Verifies user-role changes affect authorization without manual cache clearing.
     */
    @Test
    void userRoleGrantAndRevokeChangeAuthorizationWithoutManualCacheClearThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long permissionId = 0L;
        try {
            userId = createUser();
            roleId = createRole("it_auth_user_role_" + suffix, "Authorization User Role " + suffix);
            permissionId = createPermission("it_auth_user_role_permission_" + suffix,
                    "Authorization User Role Permission " + suffix, "/example/protected/**", false);
            updateRolePermission(roleId, List.of(permissionId));
            clearAllPermissionCache();

            assertProtectedPingDenied(String.valueOf(userId));
            updateUserRole(userId, List.of(roleId));
            assertProtectedPingAllowed(userId);
            recordScenario("Authorization cache / POST /authz/user-role/update",
                    "grant roleId=" + roleId + " to userId=" + userId + "; no manual clear-all",
                    "GET /api/example/protected/ping after user-role grant",
                    "HTTP 200 after user-role grant; code=200; data.message=protected pong");

            updateUserRole(userId, List.of());
            assertProtectedPingDenied(String.valueOf(userId));
            recordScenario("Authorization cache / POST /authz/user-role/update",
                    "clear roleIds for userId=" + userId + "; no manual clear-all",
                    "GET /api/example/protected/ping after user-role revoke",
                    "HTTP 403 after user-role revoke; code=403; message=access denied");
        } finally {
            cleanupQuietly(userId, roleId, permissionId, 0L);
        }
    }

    /**
     * Verifies public-access permissions allow anonymous and authenticated users.
     */
    @Test
    void publicAccessPermissionAllowsAnonymousAndAuthenticatedUsersThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long permissionId = 0L;
        try {
            userId = createUser();
            permissionId = createPermission("it_auth_public_" + suffix,
                    "Authorization Public " + suffix, "/example/public/**", true);
            clearAllPermissionCache();

            assertPublicPingAllowed(null);
            recordScenario("Authorization / GET /example/public/ping",
                    "anonymous; publicAccess=true permission target=/example/public/**",
                    "GET /api/example/public/ping",
                    "HTTP 200; code=200; data.message=public pong");

            assertPublicPingAllowed(String.valueOf(userId));
            recordScenario("Authorization / GET /example/public/ping",
                    "X-Access-User-Id=" + userId + "; publicAccess=true permission target=/example/public/**",
                    "GET /api/example/public/ping",
                    "HTTP 200; code=200; data.message=public pong");
        } finally {
            cleanupQuietly(userId, 0L, permissionId, 0L);
        }
    }

    /**
     * Verifies irregular identity headers remain unauthorized for protected resources.
     */
    @Test
    void irregularIdentityHeadersAreAnonymousForProtectedResourceThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long permissionId = 0L;
        try {
            permissionId = createPermission("it_auth_irregular_identity_" + suffix,
                    "Authorization Irregular Identity " + suffix, "/example/protected/**", false);
            clearAllPermissionCache();

            assertProtectedPingDenied("   ");
            recordScenario("Authorization / GET /example/protected/ping",
                    "X-Access-User-Id='   '; required permission target=/example/protected/**",
                    "GET /api/example/protected/ping",
                    "HTTP 403; blank identity is treated as anonymous");

            assertProtectedPingDenied("not-a-user");
            recordScenario("Authorization / GET /example/protected/ping",
                    "X-Access-User-Id=not-a-user; required permission target=/example/protected/**",
                    "GET /api/example/protected/ping",
                    "HTTP 403; non-numeric identity has no RBAC grants");
        } finally {
            cleanupQuietly(0L, 0L, permissionId, 0L);
        }
    }

    /**
     * Verifies any matching role permission is sufficient for access.
     */
    @Test
    void multipleRolesAllowWhenAnyRoleGrantsMatchingPermissionThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long irrelevantRoleId = 0L;
        long matchingRoleId = 0L;
        long irrelevantPermissionId = 0L;
        long matchingPermissionId = 0L;
        try {
            userId = createUser();
            irrelevantRoleId = createRole("it_auth_irrelevant_role_" + suffix, "Irrelevant Role " + suffix);
            matchingRoleId = createRole("it_auth_matching_role_" + suffix, "Matching Role " + suffix);
            irrelevantPermissionId = createPermission("it_auth_irrelevant_permission_" + suffix,
                    "Irrelevant Permission " + suffix, "/irrelevant/path/" + suffix, false);
            matchingPermissionId = createPermission("it_auth_matching_permission_" + suffix,
                    "Matching Permission " + suffix, "/example/protected/**", false);
            updateRolePermission(irrelevantRoleId, List.of(irrelevantPermissionId));
            updateRolePermission(matchingRoleId, List.of(matchingPermissionId));
            updateUserRole(userId, List.of(irrelevantRoleId, matchingRoleId));
            clearAllPermissionCache();

            assertProtectedPingAllowed(userId);
            recordScenario("Authorization / GET /example/protected/ping",
                    "X-Access-User-Id=" + userId + "; user has one irrelevant role and one matching role",
                    "GET /api/example/protected/ping",
                    "HTTP 200; any role with matching permission is sufficient");
        } finally {
            cleanupQuietly(userId, List.of(irrelevantRoleId, matchingRoleId),
                    List.of(irrelevantPermissionId, matchingPermissionId), List.of());
        }
    }

    /**
     * Verifies any granted required permission can authorize access.
     */
    @Test
    void multipleRequiredPermissionsAllowWhenAnyGrantedPermissionMatchesThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long ungrantedPermissionId = 0L;
        long grantedPermissionId = 0L;
        try {
            userId = createUser();
            roleId = createRole("it_auth_any_permission_role_" + suffix, "Any Permission Role " + suffix);
            ungrantedPermissionId = createPermission("it_auth_ungranted_required_" + suffix,
                    "Ungranted Required " + suffix, "/example/protected/**", false);
            grantedPermissionId = createPermission("it_auth_granted_required_" + suffix,
                    "Granted Required " + suffix, "/example/protected/**", false);
            updateRolePermission(roleId, List.of(grantedPermissionId));
            updateUserRole(userId, List.of(roleId));
            clearAllPermissionCache();

            assertProtectedPingAllowed(userId);
            recordScenario("Authorization / GET /example/protected/ping",
                    "two required permissions match /example/protected/**; user is granted one of them",
                    "GET /api/example/protected/ping",
                    "HTTP 200; affirmative decision allows any granted required permission");
        } finally {
            cleanupQuietly(userId, roleId, grantedPermissionId, 0L);
            cleanupQuietly(0L, 0L, ungrantedPermissionId, 0L);
        }
    }

    /**
     * Verifies context paths are removed before permission matching.
     */
    @Test
    void contextPathIsStrippedBeforePermissionMatchingThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long permissionId = 0L;
        try {
            userId = createUser();
            roleId = createRole("it_auth_context_role_" + suffix, "Context Role " + suffix);
            permissionId = createPermission("it_auth_context_permission_" + suffix,
                    "Context Permission " + suffix, "/example/protected/**", false);
            updateRolePermission(roleId, List.of(permissionId));
            updateUserRole(userId, List.of(roleId));
            clearAllPermissionCache();

            assertProtectedPingAllowed(userId);
            recordScenario("Authorization / GET /example/protected/ping",
                    "stored target=/example/protected/**; real request path includes /api context path; X-Access-User-Id=" + userId,
                    "GET /api/example/protected/ping",
                    "HTTP 200; /api context path is removed before RBAC matching");
        } finally {
            cleanupQuietly(userId, roleId, permissionId, 0L);
        }
    }

    /**
     * Verifies target qualifiers do not restrict current path-based matching.
     */
    @Test
    void targetQualifierDoesNotRestrictCurrentPathBasedMatchingThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long permissionId = 0L;
        try {
            userId = createUser();
            roleId = createRole("it_auth_qualifier_role_" + suffix, "Qualifier Role " + suffix);
            permissionId = createPermission("it_auth_qualifier_permission_" + suffix,
                    "Qualifier Permission " + suffix, "/example/protected/**", "POST", false);
            updateRolePermission(roleId, List.of(permissionId));
            updateUserRole(userId, List.of(roleId));
            clearAllPermissionCache();

            assertProtectedPingAllowed(userId);
            recordScenario("Authorization / GET /example/protected/ping",
                    "permission targetQualifier=POST; request method=GET; X-Access-User-Id=" + userId,
                    "GET /api/example/protected/ping",
                    "HTTP 200; current RBAC matching is path-based and ignores targetQualifier");
        } finally {
            cleanupQuietly(userId, roleId, permissionId, 0L);
        }
    }

    /**
     * Verifies permission target updates immediately affect authorization.
     */
    @Test
    void permissionTargetUpdateChangesAuthorizationThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long userId = 0L;
        long roleId = 0L;
        long permissionId = 0L;
        long guardPermissionId = 0L;
        try {
            userId = createUser();
            roleId = createRole("it_auth_target_update_role_" + suffix, "Target Update Role " + suffix);
            String permissionCode = "it_auth_target_update_permission_" + suffix;
            permissionId = createPermission(permissionCode,
                    "Target Update Permission " + suffix, "/example/protected/**", false);
            guardPermissionId = createPermission("it_auth_target_update_guard_" + suffix,
                    "Target Update Guard " + suffix, "/example/protected/**", false);
            updateRolePermission(roleId, List.of(permissionId));
            updateUserRole(userId, List.of(roleId));
            clearAllPermissionCache();

            assertProtectedPingAllowed(userId);
            postOk("/authz/permission/update", body(
                    "id", permissionId,
                    "code", permissionCode,
                    "name", "Target Update Permission Changed " + suffix,
                    "targetIdentifier", "/example/elsewhere/**",
                    "targetQualifier", "GET",
                    "publicAccess", false
            ));
            assertProtectedPingDenied(String.valueOf(userId));
            recordScenario("Authorization cache / POST /authz/permission/update",
                    "permissionId=" + permissionId + " target changed away; ungranted guard permission still requires /example/protected/**",
                    "GET /api/example/protected/ping after permission target update",
                    "HTTP 403; updated granted permission no longer satisfies the protected endpoint requirement");
        } finally {
            cleanupQuietly(userId, roleId, permissionId, 0L);
            cleanupQuietly(0L, 0L, guardPermissionId, 0L);
        }
    }

    /**
     * Verifies toggling public access changes anonymous authorization.
     */
    @Test
    void publicAccessToggleChangesAnonymousAuthorizationThroughRealHttp() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long permissionId = 0L;
        try {
            String permissionCode = "it_auth_public_toggle_" + suffix;
            permissionId = createPermission(permissionCode,
                    "Public Toggle Permission " + suffix, "/example/public/**", false);
            clearAllPermissionCache();

            assertForbidden(getJson("/example/public/ping", null, 403));
            postOk("/authz/permission/update", body(
                    "id", permissionId,
                    "code", permissionCode,
                    "name", "Public Toggle Permission Opened " + suffix,
                    "targetIdentifier", "/example/public/**",
                    "targetQualifier", "GET",
                    "publicAccess", true
            ));
            assertPublicPingAllowed(null);
            recordScenario("Authorization cache / POST /authz/permission/update",
                    "permissionId=" + permissionId + " publicAccess toggled false -> true for /example/public/**",
                    "GET /api/example/public/ping after publicAccess update",
                    "HTTP 200; anonymous user is allowed after publicAccess=true");
        } finally {
            cleanupQuietly(0L, 0L, permissionId, 0L);
        }
    }

    /**
     * Verifies unconfigured endpoints are allowed by default.
     */
    @Test
    void unconfiguredEndpointIsAllowedByDefaultThroughRealHttp() throws Exception {
        long userId = 0L;
        try {
            userId = longValue(postOk("/example/users", Map.of()).path("data"));
            recordScenario("Authorization / POST /example/users",
                    "anonymous request; no seeded required permission for /example/users",
                    "POST /api/example/users",
                    "HTTP 200; endpoint without required RBAC permission is allowed by affirmative strategy");
        } finally {
            cleanupQuietly(userId, 0L, 0L, 0L);
        }
    }
}
