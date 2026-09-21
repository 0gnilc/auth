package com.gnilc.auth.authz.provider;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证只汇总支持当前上下文的已授权限来源，并消除重复权限。 */
class DelegatingGrantedPermissionsProviderTest {
    private final AccessContext context =
            new AccessContext(new AccessIdentity("9", null), new AccessTarget("/orders", "GET"));

    @Test
    void filtersUnsupportedSourcesAndDeduplicatesPermissions() {
        Permission permission = new Permission("shared");
        GrantedPermissionsProvider supported = ignored -> List.of(permission, permission);
        GrantedPermissionsProvider unsupported = new GrantedPermissionsProvider() {
            @Override
            public boolean supports(AccessContext ignored) {
                return false;
            }

            @Override
            public List<Permission> provide(AccessContext ignored) {
                return List.of(new Permission("hidden"));
            }
        };

        assertThat(new DelegatingGrantedPermissionsProvider(Set.of(supported, unsupported)).provide(context))
                .containsExactly(permission);
    }
}
