package com.gnilc.auth.authz.rbac.provider;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessEnvironment;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证所需权限按路径模式和请求方法匹配，非 Servlet 环境不进入此提供者。 */
class RbacRequiredPermissionsProviderTest {
    private final PermissionCacheService cacheService = mock(PermissionCacheService.class);
    private final RbacRequiredPermissionsProvider provider = new RbacRequiredPermissionsProvider();

    @BeforeEach
    void injectCache() {
        ReflectionTestUtils.setField(provider, "cacheService", cacheService);
    }

    @Test
    void usesAntPathMatchingAndDeduplicatesCodes() {
        when(cacheService.loadTargetPermissions()).thenReturn(List.of(
                new TargetPermission("/sys/**", null, "admin"),
                new TargetPermission("/sys/admin/*", null, "admin"),
                new TargetPermission("/sys/**", "", "empty-qualifier"),
                new TargetPermission("/sys/**", "   ", "whitespace-qualifier"),
                new TargetPermission("/public/**", null, "public")));

        assertThat(provider.provide(servletContext("/sys/admin/7")))
                .containsExactly(new Permission("admin"));
        assertThat(provider.provide(servletContext("/unknown"))).isEmpty();
    }

    @Test
    void matchesOnlyPermissionForRequestQualifier() {
        when(cacheService.loadTargetPermissions()).thenReturn(List.of(
                new TargetPermission("/provider-callback/{transactionNo}", "GET", "callback:get"),
                new TargetPermission("/provider-callback/{transactionNo}", "POST", "callback:post")));

        assertThat(provider.provide(servletContext(
                "/provider-callback/12345678901234567890123456789012", "GET")))
                .containsExactly(new Permission("callback:get"));
        assertThat(provider.provide(servletContext(
                "/provider-callback/12345678901234567890123456789012", "POST")))
                .containsExactly(new Permission("callback:post"));
    }

    @Test
    void ignoresNonServletEnvironment() {
        AccessContext context = new AccessContext(AccessEnvironment.of("worker"),
                new AccessIdentity("1", Map.of()), new AccessTarget("/sys", "GET"));

        assertThat(provider.supports(context)).isFalse();
        assertThat(provider.provide(context)).isEmpty();
    }

    private AccessContext servletContext(String path) {
        return servletContext(path, "GET");
    }

    private AccessContext servletContext(String path, String qualifier) {
        return new AccessContext(AccessEnvironment.SERVLET,
                new AccessIdentity("1", Map.of()), new AccessTarget(path, qualifier));
    }
}
