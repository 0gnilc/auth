package com.gnilc.auth.authz.provider;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessEnvironment;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DelegatingGrantedPermissionsProviderTest {

    // 默认 provider 未覆盖 supports 时仍参与授权判断，保持历史兼容。
    // TestCaseId: CORE-AUTHZ-023
    @Test
    void callProviderThatUsesDefaultSupports() {
        DelegatingGrantedPermissionsProvider provider = new DelegatingGrantedPermissionsProvider(Set.of(
                context -> List.of(new Permission("user:read"))
        ));

        List<Permission> permissions = provider.provide(accessContext(AccessEnvironment.SERVLET));

        assertThat(permissions).containsExactly(new Permission("user:read"));
    }

    // 不支持当前访问环境的 provider 不应被调用，避免跨环境权限串用。
    // TestCaseId: CORE-AUTHZ-024
    @Test
    void skipProviderThatDoesNotSupportContext() {
        AtomicBoolean unsupportedCalled = new AtomicBoolean(false);
        GrantedPermissionsProvider unsupported = new GrantedPermissionsProvider() {
            @Override
            public boolean supports(AccessContext context) {
                return false;
            }

            @Override
            public List<Permission> provide(AccessContext context) {
                unsupportedCalled.set(true);
                return List.of(new Permission("mq:send"));
            }
        };
        DelegatingGrantedPermissionsProvider provider = new DelegatingGrantedPermissionsProvider(Set.of(
                unsupported,
                context -> List.of(new Permission("web:read"))
        ));

        List<Permission> permissions = provider.provide(accessContext(AccessEnvironment.SERVLET));

        assertThat(permissions).containsExactly(new Permission("web:read"));
        assertThat(unsupportedCalled).isFalse();
    }

    // 多个支持当前访问环境的 provider 应继续合并并去重。
    // TestCaseId: CORE-AUTHZ-025
    @Test
    void aggregateAndDeduplicateSupportedProviders() {
        GrantedPermissionsProvider first = context -> List.of(new Permission("user:read"));
        GrantedPermissionsProvider second = context -> List.of(new Permission("user:read"), new Permission("menu:view"));
        DelegatingGrantedPermissionsProvider provider = new DelegatingGrantedPermissionsProvider(Set.of(first, second));

        List<Permission> permissions = provider.provide(accessContext(AccessEnvironment.SERVLET));

        assertThat(permissions).containsExactlyInAnyOrder(new Permission("user:read"), new Permission("menu:view"));
    }

    // 单个 provider 返回 null 时按空权限集合处理，避免空指针泄漏到授权流程。
    // TestCaseId: CORE-AUTHZ-026
    @Test
    void treatNullProviderResultAsEmpty() {
        GrantedPermissionsProvider empty = context -> null;
        GrantedPermissionsProvider granted = context -> List.of(new Permission("user:read"));
        DelegatingGrantedPermissionsProvider provider = new DelegatingGrantedPermissionsProvider(Set.of(empty, granted));

        List<Permission> permissions = provider.provide(accessContext(AccessEnvironment.SERVLET));

        assertThat(permissions).containsExactly(new Permission("user:read"));
    }

    // supports 与 provide 接收同一个访问上下文。
    // TestCaseId: CORE-AUTHZ-027
    @Test
    void passSameContextToSupportsAndProvide() {
        AccessContext context = accessContext(AccessEnvironment.SERVLET);
        AtomicReference<AccessContext> supportsContext = new AtomicReference<>();
        AtomicReference<AccessContext> provideContext = new AtomicReference<>();
        GrantedPermissionsProvider granted = new GrantedPermissionsProvider() {
            @Override
            public boolean supports(AccessContext candidate) {
                supportsContext.set(candidate);
                return true;
            }

            @Override
            public List<Permission> provide(AccessContext candidate) {
                provideContext.set(candidate);
                return List.of(new Permission("user:read"));
            }
        };
        DelegatingGrantedPermissionsProvider provider = new DelegatingGrantedPermissionsProvider(Set.of(granted));

        provider.provide(context);

        assertThat(supportsContext).hasValue(context);
        assertThat(provideContext).hasValue(context);
    }

    private AccessContext accessContext(AccessEnvironment environment) {
        return new AccessContext(
                environment,
                new AccessIdentity("1001", Map.of()),
                new AccessTarget("/admin/users", null, Map.of()),
                Map.of()
        );
    }
}
