package com.gnilc.authz.rbac.context;

import com.gnilc.authz.context.AccessIdentity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeAccessIdentityResolverTest {

    /**
     * 没有任何委托时，RBAC 身份解析应回退为匿名访问。
     */
    @Test
    void resolveAnonymousIdentityWhenNoDelegateExists() {
        CompositeAccessIdentityResolver<String> resolver = new CompositeAccessIdentityResolver<>(List.of());

        AccessIdentity identity = resolver.resolve("/admin/users");

        assertAnonymous(identity);
    }

    /**
     * 组合解析器只使用第一个支持当前来源的委托，避免跨用户体系误 fallback。
     */
    @Test
    void useFirstSupportedDelegate() {
        AccessIdentityResolverDelegate<String> unsupported = new TestDelegate(false, new AccessIdentity("1001", Map.of("source", "unsupported")));
        AccessIdentityResolverDelegate<String> first = new TestDelegate(true, new AccessIdentity("1002", Map.of("source", "first")));
        AccessIdentityResolverDelegate<String> second = new TestDelegate(true, new AccessIdentity("1003", Map.of("source", "second")));
        CompositeAccessIdentityResolver<String> resolver = new CompositeAccessIdentityResolver<>(List.of(unsupported, first, second));

        AccessIdentity identity = resolver.resolve("/admin/users");

        assertThat(identity.getIdentifier()).isEqualTo("1002");
        assertThat(identity.getAttributes()).containsEntry("source", "first");
    }

    /**
     * 委托返回全局数字 user_id 时，应原样暴露给 RBAC 授权流程。
     */
    @Test
    void resolveNumericGlobalUserId() {
        CompositeAccessIdentityResolver<String> resolver = new CompositeAccessIdentityResolver<>(List.of(
                new TestDelegate(true, new AccessIdentity("1001", Map.of("source", "admin")))
        ));

        AccessIdentity identity = resolver.resolve("/admin/users");

        assertThat(identity.getIdentifier()).isEqualTo("1001");
        assertThat(identity.getAttributes()).containsEntry("source", "admin");
    }

    /**
     * 委托返回空标识时，组合解析器按匿名身份处理。
     */
    @Test
    void treatBlankIdentifierAsAnonymous() {
        CompositeAccessIdentityResolver<String> resolver = new CompositeAccessIdentityResolver<>(List.of(
                new TestDelegate(true, new AccessIdentity(" ", Map.of("source", "admin")))
        ));

        AccessIdentity identity = resolver.resolve("/admin/users");

        assertAnonymous(identity);
    }

    /**
     * RBAC 身份标识必须是全局数字 user_id，非数字委托结果按匿名身份处理。
     */
    @Test
    void treatNonNumericIdentifierAsAnonymous() {
        CompositeAccessIdentityResolver<String> resolver = new CompositeAccessIdentityResolver<>(List.of(
                new TestDelegate(true, new AccessIdentity("ADMIN:1001", Map.of("source", "admin")))
        ));

        AccessIdentity identity = resolver.resolve("/admin/users");

        assertAnonymous(identity);
    }

    /**
     * 委托返回带空白的数字标识时，组合解析器会裁剪为标准 user_id 字符串。
     */
    @Test
    void trimNumericIdentifier() {
        CompositeAccessIdentityResolver<String> resolver = new CompositeAccessIdentityResolver<>(List.of(
                new TestDelegate(true, new AccessIdentity(" 1001 ", Map.of("source", "header")))
        ));

        AccessIdentity identity = resolver.resolve("/admin/users");

        assertThat(identity.getIdentifier()).isEqualTo("1001");
        assertThat(identity.getAttributes()).containsEntry("source", "header");
    }

    private void assertAnonymous(AccessIdentity identity) {
        assertThat(identity.getIdentifier()).isNull();
        assertThat(identity.getAttributes()).containsEntry("anonymous", true);
    }

    private static class TestDelegate implements AccessIdentityResolverDelegate<String> {
        private final boolean supports;
        private final AccessIdentity identity;

        private TestDelegate(boolean supports, AccessIdentity identity) {
            this.supports = supports;
            this.identity = identity;
        }

        @Override
        public boolean supports(String source) {
            return supports;
        }

        @Override
        public AccessIdentity resolve(String source) {
            return identity;
        }
    }
}
