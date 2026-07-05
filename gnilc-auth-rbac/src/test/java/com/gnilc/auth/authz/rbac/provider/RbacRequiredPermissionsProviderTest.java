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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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
     * 当前 Servlet 适配场景中，TargetPermission.target 被 provider 解释为 Ant 风格路径匹配表达式；
     * 命中 /admin/users/** 时，只应返回 user:read，不应返回不匹配的 role:read。
     */
    // TestCaseId: RBAC-PROVIDER-009
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
    // TestCaseId: RBAC-PROVIDER-010
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
    // TestCaseId: RBAC-PROVIDER-011
    @Test
    void provideNoRequiredPermissionsWhenTargetIsMissing() {
        List<Permission> required = provider.provide(new AccessContext(
                AccessEnvironment.SERVLET,
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
    // TestCaseId: RBAC-PROVIDER-012
    @Test
    void treatNullCacheResultsAsEmptyPermissionLists() {
        when(cache.loadTargetPermissions()).thenReturn(null);

        List<Permission> required = provider.provide(accessContext("/admin/users/1"));

        assertThat(required).isEmpty();
    }

    /**
     * RBAC required provider 只参与 Servlet 访问环境，避免与消息、任务等环境的同名目标串用。
     */
    // TestCaseId: RBAC-PROVIDER-013
    @Test
    void supportOnlyServletAccessEnvironment() {
        assertThat(provider.supports(accessContext(AccessEnvironment.SERVLET, "/admin/users/1"))).isTrue();
        assertThat(provider.supports(accessContext(AccessEnvironment.of("message"), "/admin/users/1"))).isFalse();
        assertThat(provider.supports(accessContext(AccessEnvironment.of("task"), "/admin/users/1"))).isFalse();
        assertThat(provider.supports(accessContext(AccessEnvironment.UNSPECIFIED, "/admin/users/1"))).isFalse();
        assertThat(provider.supports(null)).isFalse();
    }

    /**
     * 非 Servlet 环境下 provider 应直接返回空集合，并且不访问 RBAC cache。
     */
    // TestCaseId: RBAC-PROVIDER-014
    @Test
    void provideNoPermissionsAndSkipCacheForNonServletEnvironment() {
        List<Permission> required = provider.provide(accessContext(AccessEnvironment.of("message"), "/admin/users/1"));

        assertThat(required).isEmpty();
        verifyNoInteractions(cache);
    }

    /**
     * blank target identifier 不能参与路径匹配，应安全返回空集合且不读取缓存。
     */
    // TestCaseId: RBAC-PROVIDER-015
    @Test
    void provideNoPermissionsForBlankTargetIdentifier() {
        List<Permission> required = provider.provide(accessContext("   "));

        assertThat(required).isEmpty();
        verifyNoInteractions(cache);
    }

    /**
     * RBAC 第一版只按 target identifier 匹配路径，HTTP method 等 qualifier 不参与权限选择。
     */
    // TestCaseId: RBAC-PROVIDER-016
    @Test
    void ignoreTargetQualifierWhenMatchingRequiredPermissions() {
        when(cache.loadTargetPermissions()).thenReturn(List.of(
                new TargetPermission("/admin/users/**", "user:read")
        ));
        AccessContext getContext = accessContext("/admin/users/1", "GET");
        AccessContext postContext = accessContext("/admin/users/1", "POST");

        assertThat(provider.provide(getContext)).containsExactly(new Permission("user:read"));
        assertThat(provider.provide(postContext)).containsExactly(new Permission("user:read"));
    }

    /**
     * 构造访问目标上下文：本测试关注 target identifier 与 environment 到 required permissions 的映射，
     * identity 只用于保持 AccessContext 结构完整。
     */
    private AccessContext accessContext(String target) {
        return accessContext(AccessEnvironment.SERVLET, target);
    }

    private AccessContext accessContext(String target, String qualifier) {
        return accessContext(AccessEnvironment.SERVLET, target, qualifier);
    }

    private AccessContext accessContext(AccessEnvironment environment, String target) {
        return accessContext(environment, target, null);
    }

    private AccessContext accessContext(AccessEnvironment environment, String target, String qualifier) {
        return new AccessContext(
                environment,
                new AccessIdentity("1001", Map.of()),
                new AccessTarget(target, qualifier, Map.of()),
                Map.of()
        );
    }
}
