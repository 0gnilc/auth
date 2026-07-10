package com.gnilc.auth.authz.context;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AbstractAccessContextAdapterTest {

    // TestCaseId: CORE-AUTHZ-006
    @Test
    void composeResolvedFactsIntoAccessContext() {
        AtomicReference<String> environmentSource = new AtomicReference<>();
        AtomicReference<String> identitySource = new AtomicReference<>();
        AtomicReference<String> targetSource = new AtomicReference<>();
        AbstractAccessContextAdapter<String> adapter = new AbstractAccessContextAdapter<>(
                source -> {
                    environmentSource.set(source);
                    return AccessEnvironment.SERVLET;
                },
                source -> {
                    identitySource.set(source);
                    return new AccessIdentity("1001", Map.of("identity", true));
                },
                source -> {
                    targetSource.set(source);
                    return new AccessTarget("/admin/users", "GET", Map.of("target", true));
                }
        ) {
        };

        AccessContext context = adapter.adapt("request");

        assertThat(environmentSource).hasValue("request");
        assertThat(identitySource).hasValue("request");
        assertThat(targetSource).hasValue("request");
        assertThat(context.getEnvironment()).isEqualTo(AccessEnvironment.SERVLET);
        assertThat(context.getIdentity().getIdentifier()).isEqualTo("1001");
        assertThat(context.getIdentity().getAttributes()).containsEntry("identity", true);
        assertThat(context.getTarget().getIdentifier()).isEqualTo("/admin/users");
        assertThat(context.getTarget().getQualifier()).isEqualTo("GET");
        assertThat(context.getTarget().getAttributes()).containsEntry("target", true);
        assertThat(context.getAttributes()).isEmpty();
    }

    // TestCaseId: CORE-AUTHZ-007
    @Test
    void defaultNullResolvedEnvironmentToUnspecifiedThroughAccessContext() {
        AbstractAccessContextAdapter<String> adapter = new MinimalAccessContextAdapter(source -> null);

        AccessContext context = adapter.adapt("request");

        assertThat(context.getEnvironment()).isEqualTo(AccessEnvironment.UNSPECIFIED);
    }

    // TestCaseId: CORE-AUTHZ-008
    @Test
    void createAccessContextWithEmptyAttributes() {
        AbstractAccessContextAdapter<String> adapter = new MinimalAccessContextAdapter(source -> AccessEnvironment.SERVLET);

        AccessContext context = adapter.adapt("request");

        assertThat(context.getAttributes()).isEmpty();
    }

    // 每次 adapt 都应重新解析访问事实，避免复用上一次请求的身份或目标。
    // TestCaseId: CORE-AUTHZ-009
    @Test
    void resolveAccessFactsForEachAdaptCall() {
        AtomicInteger sequence = new AtomicInteger();
        AbstractAccessContextAdapter<String> adapter = new AbstractAccessContextAdapter<>(
                source -> AccessEnvironment.SERVLET,
                source -> new AccessIdentity(source + "-" + sequence.incrementAndGet(), Map.of()),
                source -> new AccessTarget("/" + source + "/" + sequence.get(), "GET", Map.of())
        ) {
        };

        AccessContext first = adapter.adapt("request");
        AccessContext second = adapter.adapt("request");

        assertThat(first.getIdentity().getIdentifier()).isEqualTo("request-1");
        assertThat(first.getTarget().getIdentifier()).isEqualTo("/request/1");
        assertThat(second.getIdentity().getIdentifier()).isEqualTo("request-2");
        assertThat(second.getTarget().getIdentifier()).isEqualTo("/request/2");
    }

    // resolver 是 adapter 的行为依赖，应在构造后持续用于后续 adapt 调用。
    // TestCaseId: CORE-AUTHZ-010
    @Test
    void useConstructorResolversAcrossAdaptCalls() {
        AtomicInteger environmentCalls = new AtomicInteger();
        AtomicInteger identityCalls = new AtomicInteger();
        AtomicInteger targetCalls = new AtomicInteger();
        AbstractAccessContextAdapter<String> adapter = new AbstractAccessContextAdapter<>(
                source -> {
                    environmentCalls.incrementAndGet();
                    return AccessEnvironment.SERVLET;
                },
                source -> {
                    identityCalls.incrementAndGet();
                    return new AccessIdentity("1001", Map.of());
                },
                source -> {
                    targetCalls.incrementAndGet();
                    return new AccessTarget("/admin/users", "GET", Map.of());
                }
        ) {
        };

        adapter.adapt("first");
        adapter.adapt("second");

        assertThat(environmentCalls).hasValue(2);
        assertThat(identityCalls).hasValue(2);
        assertThat(targetCalls).hasValue(2);
    }

    // TestCaseId: CORE-AUTHZ-011
    @Test
    void requireAccessEnvironmentResolver() {
        assertThatNullPointerException()
                .isThrownBy(() -> new MinimalAccessContextAdapter(null))
                .withMessage("environmentResolver must not be null");
    }

    // TestCaseId: CORE-AUTHZ-012
    @Test
    void requireAccessIdentityResolver() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AbstractAccessContextAdapter<String>(
                        source -> AccessEnvironment.SERVLET,
                        null,
                        source -> new AccessTarget("/admin/users", null, Map.of())
                ) {
                })
                .withMessage("identityResolver must not be null");
    }

    // TestCaseId: CORE-AUTHZ-013
    @Test
    void requireAccessTargetResolver() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AbstractAccessContextAdapter<String>(
                        source -> AccessEnvironment.SERVLET,
                        source -> new AccessIdentity("1001", Map.of()),
                        null
                ) {
                })
                .withMessage("targetResolver must not be null");
    }

    private static class MinimalAccessContextAdapter extends AbstractAccessContextAdapter<String> {

        private MinimalAccessContextAdapter(AccessEnvironmentResolver<String> environmentResolver) {
            super(
                    environmentResolver,
                    source -> new AccessIdentity("1001", Map.of()),
                    source -> new AccessTarget("/admin/users", null, Map.of())
            );
        }
    }
}
