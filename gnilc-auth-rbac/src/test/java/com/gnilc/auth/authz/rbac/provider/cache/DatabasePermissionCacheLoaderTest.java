package com.gnilc.auth.authz.rbac.provider.cache;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.bo.UserBo;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabasePermissionCacheLoaderTest {
    private PermissionService permissionService;
    private UserServiceImpl userService;
    private DatabasePermissionCacheLoader loader;

    /**
     * Sets up a fresh cache loader with mocked services before each test.
     */
    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        userService = mock(UserServiceImpl.class);
        loader = new DatabasePermissionCacheLoader();
        ReflectionTestUtils.setField(loader, "permissionService", permissionService);
        ReflectionTestUtils.setField(loader, "userService", userService);
    }

    /**
     * Verifies target permissions use target identifiers and codes.
     */
    // TestCaseId: RBAC-CACHE-004
    @Test
    void targetPermissionsUseTargetIdentifierAndCode() {
        PermissionBo permission = permission(1L, "user:read", "/users/**", false);
        when(permissionService.list()).thenReturn(List.of(permission));

        List<TargetPermission> targetPermissions = loader.loadTargetPermissions();

        assertThat(targetPermissions).containsExactly(new TargetPermission("/users/**", "user:read"));
    }

    /**
     * Verifies missing users return no granted permissions.
     */
    // TestCaseId: RBAC-CACHE-005
    @Test
    void userPermissionsAreEmptyWhenUserDoesNotExist() {
        when(userService.geUser(100L)).thenReturn(null);

        List<Permission> permissions = loader.loadUserPermissions(100L);

        assertThat(permissions).isEmpty();
        verify(permissionService, never()).getPermissions(100L);
    }

    /**
     * Verifies existing users receive permission-code grants.
     */
    // TestCaseId: RBAC-CACHE-006
    @Test
    void userPermissionsUsePermissionCodesWhenUserExists() {
        when(userService.geUser(100L)).thenReturn(new UserBo());
        when(permissionService.getPermissions(100L)).thenReturn(List.of(
                permission(1L, "user:read", "/users/**", false),
                permission(2L, "role:read", "/roles/**", false)
        ));

        List<Permission> permissions = loader.loadUserPermissions(100L);

        assertThat(permissions).containsExactly(new Permission("user:read"), new Permission("role:read"));
    }

    /**
     * Verifies public-access permissions expose only query result codes.
     */
    // TestCaseId: RBAC-CACHE-007
    @Test
    void publicAccessPermissionsUseOnlyQueryResultCodes() {
        when(permissionService.list(ArgumentMatchers.<Wrapper<PermissionBo>>any())).thenReturn(List.of(
                permission(1L, "public:read", "/public/**", true)
        ));

        List<Permission> permissions = loader.loadPublicAccessPermissions();

        assertThat(permissions).containsExactly(new Permission("public:read"));
    }

    private PermissionBo permission(Long id, String code, String targetIdentifier, boolean publicAccess) {
        PermissionBo permission = new PermissionBo();
        permission.setId(id);
        permission.setCode(code);
        permission.setName(code);
        permission.setTargetIdentifier(targetIdentifier);
        permission.setPublicAccess(publicAccess);
        return permission;
    }
}
