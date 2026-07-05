package com.gnilc.auth.authz.rbac.provider;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessEnvironment;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RbacGrantedPermissionsProviderTest {
    private PermissionCache cache;
    private RbacGrantedPermissionsProvider provider;

    /**
     * Sets up a fresh provider and cache before each test.
     */
    @BeforeEach
    void setUp() {
        cache = mock(PermissionCache.class);
        provider = new RbacGrantedPermissionsProvider();
        ReflectionTestUtils.setField(provider, "cache", cache);
    }

    /**
     * 已识别访问身份应同时获得两类已授予权限：
     * <ul>
     *     <li>用户自身通过 RBAC 角色获得的权限；</li>
     *     <li>系统公开访问给所有身份的权限。</li>
     * </ul>
     * 该用例验证 provider 对 core 暴露的是合并后的 granted permissions，
     * 而不是只返回用户权限或只返回公开访问权限。
     */
    // TestCaseId: RBAC-PROVIDER-001
    @Test
    void provideUserPermissionsAndPublicAccessPermissionsForIdentifiedAccess() {
        when(cache.loadUserPermissions(1001L)).thenReturn(List.of(new Permission("user:read")));
        when(cache.loadPublicAccessPermissions()).thenReturn(List.of(new Permission("public:access")));

        List<Permission> granted = provider.provide(accessContext("1001"));

        assertThat(granted).containsExactlyInAnyOrder(
                new Permission("user:read"),
                new Permission("public:access")
        );
    }

    /**
     * 匿名访问没有可解析的 RBAC 用户 ID，因此不应该尝试读取用户权限缓存。
     * 但公开访问权限与身份无关，匿名访问也应该获得公开访问权限。
     */
    // TestCaseId: RBAC-PROVIDER-002
    @Test
    void provideOnlyPublicAccessPermissionsForAnonymousAccess() {
        when(cache.loadPublicAccessPermissions()).thenReturn(List.of(new Permission("public:access")));

        List<Permission> granted = provider.provide(accessContext(null));

        assertThat(granted).containsExactly(new Permission("public:access"));
        verify(cache, never()).loadUserPermissions(null);
    }

    /**
     * 非数字身份不是合法 RBAC user_id，不应读取用户权限，但仍可获得公开访问权限。
     */
    // TestCaseId: RBAC-PROVIDER-003
    @Test
    void provideOnlyPublicAccessPermissionsForNonNumericIdentity() {
        when(cache.loadPublicAccessPermissions()).thenReturn(List.of(new Permission("public:access")));

        List<Permission> granted = provider.provide(accessContext("ADMIN:1001"));

        assertThat(granted).containsExactly(new Permission("public:access"));
        verify(cache, never()).loadUserPermissions(anyLong());
    }

    /**
     * cache 实现理论上应返回空集合而不是 null，但 provider 作为授权流程入口需要更稳健。
     * 该用例验证 cache 返回 null 时按空权限集合处理，避免空指针泄漏到 core 授权流程。
     */
    // TestCaseId: RBAC-PROVIDER-004
    @Test
    void treatNullCacheResultsAsEmptyPermissionLists() {
        when(cache.loadUserPermissions(1001L)).thenReturn(null);
        when(cache.loadPublicAccessPermissions()).thenReturn(null);

        List<Permission> granted = provider.provide(accessContext("1001"));

        assertThat(granted).isEmpty();
    }

    /**
     * RBAC granted provider 只参与 Servlet 访问环境，避免与消息、任务等环境的同名权限串用。
     */
    // TestCaseId: RBAC-PROVIDER-005
    @Test
    void supportOnlyServletAccessEnvironment() {
        assertThat(provider.supports(accessContext(AccessEnvironment.SERVLET, "1001"))).isTrue();
        assertThat(provider.supports(accessContext(AccessEnvironment.of("message"), "1001"))).isFalse();
        assertThat(provider.supports(accessContext(AccessEnvironment.of("task"), "1001"))).isFalse();
        assertThat(provider.supports(accessContext(AccessEnvironment.UNSPECIFIED, "1001"))).isFalse();
        assertThat(provider.supports(null)).isFalse();
    }

    /**
     * 非 Servlet 环境下 provider 应直接返回空集合，并且不访问 RBAC cache。
     */
    // TestCaseId: RBAC-PROVIDER-006
    @Test
    void provideNoPermissionsAndSkipCacheForNonServletEnvironment() {
        List<Permission> granted = provider.provide(accessContext(AccessEnvironment.of("message"), "1001"));

        assertThat(granted).isEmpty();
        verifyNoInteractions(cache);
    }

    /**
     * 用户权限和公开访问权限中出现相同权限时，provider 对 core 只暴露去重后的集合。
     */
    // TestCaseId: RBAC-PROVIDER-007
    @Test
    void deduplicateUserAndPublicAccessPermissions() {
        when(cache.loadUserPermissions(1001L)).thenReturn(List.of(
                new Permission("menu:view"),
                new Permission("menu:view")
        ));
        when(cache.loadPublicAccessPermissions()).thenReturn(List.of(
                new Permission("menu:view"),
                new Permission("public:access")
        ));

        List<Permission> granted = provider.provide(accessContext("1001"));

        assertThat(granted).containsExactlyInAnyOrder(
                new Permission("menu:view"),
                new Permission("public:access")
        );
    }

    /**
     * 空白身份标识应视为匿名访问，不读取用户权限，但仍合并公开访问权限。
     */
    // TestCaseId: RBAC-PROVIDER-008
    @Test
    void provideOnlyPublicAccessPermissionsForBlankIdentity() {
        when(cache.loadPublicAccessPermissions()).thenReturn(List.of(new Permission("public:access")));

        List<Permission> granted = provider.provide(accessContext("   "));

        assertThat(granted).containsExactly(new Permission("public:access"));
        verify(cache, never()).loadUserPermissions(anyLong());
    }

    /**
     * 构造最小访问上下文：本测试关注 identity 与 environment 对 granted permissions 的影响，
     * target 在这里仅用于保持 AccessContext 结构完整。
     */
    private AccessContext accessContext(String identity) {
        return accessContext(AccessEnvironment.SERVLET, identity);
    }

    private AccessContext accessContext(AccessEnvironment environment, String identity) {
        return new AccessContext(
                environment,
                new AccessIdentity(identity, Map.of()),
                new AccessTarget("/admin/users", null, Map.of()),
                Map.of()
        );
    }
}
