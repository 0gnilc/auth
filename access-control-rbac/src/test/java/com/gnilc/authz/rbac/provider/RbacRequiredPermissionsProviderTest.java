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
import static org.mockito.Mockito.when;

class RbacRequiredPermissionsProviderTest {
    private PermissionCache cache;
    private RbacRequiredPermissionsProvider provider;

    @BeforeEach
    void setUp() {
        cache = mock(PermissionCache.class);
        provider = new RbacRequiredPermissionsProvider();
        ReflectionTestUtils.setField(provider, "cache", cache);
    }

    /**
     * 访问目标命中某条目标权限时，应返回该目标权限上配置的权限标识。
     * 当前 Web 适配场景中，TargetPermission.target 被 provider 解释为 Ant 风格路径匹配表达式；
     * 命中 /admin/users/** 时，只应返回 user:read，不应返回不匹配的 role:read。
     */
    @Test
    void providePermissionsRequiredByMatchingAccessTarget() {
        when(cache.loadTargetPermissions()).thenReturn(List.of(
                new TargetPermission("/admin/users/**", "user:read"),
                new TargetPermission("/admin/roles/**", "role:read")
        ));

        List<Permission> required = provider.provide(accessContext("/admin/users/1"));

        assertThat(required).containsExactly(new Permission("user:read"));
    }

    /**
     * 多条目标权限可能同时命中同一个访问目标，也可能配置了相同的权限标识。
     * core 决策只需要知道访问目标需要哪些权限，因此重复权限应在 provider 层去重。
     */
    @Test
    void deduplicatePermissionsFromMultipleMatchingTargets() {
        when(cache.loadTargetPermissions()).thenReturn(List.of(
                new TargetPermission("/admin/**", "user:read"),
                new TargetPermission("/admin/users/**", "user:read")
        ));

        List<Permission> required = provider.provide(accessContext("/admin/users/1"));

        assertThat(required).containsExactly(new Permission("user:read"));
    }

    /**
     * 没有访问目标时，RBAC provider 无法计算 required permissions。
     * 该场景应安全返回空集合，让后续 AccessDecision 按“无所需权限”处理。
     */
    @Test
    void provideNoRequiredPermissionsWhenTargetIsMissing() {
        List<Permission> required = provider.provide(new AccessContext(
                new AccessIdentity("1001", Map.of()),
                null,
                Map.of()
        ));

        assertThat(required).isEmpty();
    }

    /**
     * cache 返回 null 时应按无目标权限处理。
     * 该用例保证 provider 不把缓存实现的异常返回形态扩散为授权流程中的空指针异常。
     */
    @Test
    void treatNullCacheResultsAsEmptyPermissionLists() {
        when(cache.loadTargetPermissions()).thenReturn(null);

        List<Permission> required = provider.provide(accessContext("/admin/users/1"));

        assertThat(required).isEmpty();
    }

    /**
     * 构造访问目标上下文：本测试关注 target identifier 到 required permissions 的映射，
     * identity 只用于保持 AccessContext 结构完整。
     */
    private AccessContext accessContext(String target) {
        return new AccessContext(
                new AccessIdentity("1001", Map.of()),
                new AccessTarget(target, null, Map.of()),
                Map.of()
        );
    }
}
