package com.gnilc.auth.authz.provider;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionTest {

    // 权限以 symbol 作为值语义，相同 symbol 应视为同一权限。
    // TestCaseId: CORE-AUTHZ-020
    @Test
    void permissionsWithSameSymbolAreEqual() {
        Permission first = new Permission("user:read");
        Permission second = new Permission("user:read");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }

    // 不同 symbol 必须保持区分，避免授权判断误命中。
    // TestCaseId: CORE-AUTHZ-021
    @Test
    void permissionsWithDifferentSymbolsAreDifferent() {
        assertThat(new Permission("user:read"))
                .isNotEqualTo(new Permission("user:write"));
    }

    // 集合去重依赖 Permission 的值语义，重复权限只应保留一次。
    // TestCaseId: CORE-AUTHZ-022
    @Test
    void permissionsCanDeduplicateInHashCollections() {
        Set<Permission> permissions = new HashSet<>();

        permissions.add(new Permission("menu:view"));
        permissions.add(new Permission("menu:view"));
        permissions.add(new Permission("role:read"));

        assertThat(permissions)
                .containsExactlyInAnyOrder(new Permission("menu:view"), new Permission("role:read"));
    }
}
