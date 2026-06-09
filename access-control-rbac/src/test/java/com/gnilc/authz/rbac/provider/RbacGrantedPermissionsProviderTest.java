package com.gnilc.authz.rbac.provider;

import com.gnilc.authz.context.AccessContext;
import com.gnilc.authz.context.AccessIdentity;
import com.gnilc.authz.context.AccessTarget;
import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.rbac.provider.cache.PermissionCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Test
    void provideOnlyPublicAccessPermissionsForAnonymousAccess() {
        when(cache.loadPublicAccessPermissions()).thenReturn(List.of(new Permission("public:access")));

        List<Permission> granted = provider.provide(accessContext(null));

        assertThat(granted).containsExactly(new Permission("public:access"));
        verify(cache, never()).loadUserPermissions(null);
    }

    /**
     * cache 实现理论上应返回空集合而不是 null，但 provider 作为授权流程入口需要更稳健。
     * 该用例验证 cache 返回 null 时按空权限集合处理，避免空指针泄漏到 core 授权流程。
     */
    @Test
    void treatNullCacheResultsAsEmptyPermissionLists() {
        when(cache.loadUserPermissions(1001L)).thenReturn(null);
        when(cache.loadPublicAccessPermissions()).thenReturn(null);

        List<Permission> granted = provider.provide(accessContext("1001"));

        assertThat(granted).isEmpty();
    }

    /**
     * 构造最小访问上下文：本测试只关注 identity 对 granted permissions 的影响，
     * target 在这里仅用于满足 AccessContext 的完整结构。
     */
    private AccessContext accessContext(String identity) {
        return new AccessContext(
                new AccessIdentity(identity, Map.of()),
                new AccessTarget("/admin/users", null, Map.of()),
                Map.of()
        );
    }
}
