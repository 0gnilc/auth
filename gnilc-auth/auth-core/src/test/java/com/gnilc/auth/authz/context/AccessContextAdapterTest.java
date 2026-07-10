package com.gnilc.auth.authz.context;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessContextAdapterTest {

    @Test
    void accessFactsNormalizeEnvironmentAndKeepContextAttributesIndependent() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("tenant", "north");

        AccessContext context = new AccessContext(
                AccessEnvironment.of("  MESSAGE  "),
                new AccessIdentity("worker-1", Map.of("kind", "service")),
                new AccessTarget("orders", "consume"),
                attributes
        );
        attributes.put("tenant", "south");

        assertThat(context.getEnvironment()).isEqualTo(AccessEnvironment.of("message"));
        assertThat(context.getEnvironment().getIdentifier()).isEqualTo("message");
        assertThat(context.getAttributes()).containsExactly(Map.entry("tenant", "north"));
        assertThat(new AccessContext((AccessEnvironment) null, null, null).getEnvironment())
                .isSameAs(AccessEnvironment.UNSPECIFIED);
        assertThat(AccessEnvironment.of(" servlet ")).isSameAs(AccessEnvironment.SERVLET);
        assertThat(AccessEnvironment.of(" ")).isSameAs(AccessEnvironment.UNSPECIFIED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void adaptsSourceWithAllResolvers() {
        String source = "request";
        AccessEnvironment environment = AccessEnvironment.of("message");
        AccessIdentity identity = new AccessIdentity("user-1", null);
        AccessTarget target = new AccessTarget("orders", "consume");
        AccessEnvironmentResolver<String> environmentResolver = mock(AccessEnvironmentResolver.class);
        AccessIdentityResolver<String> identityResolver = mock(AccessIdentityResolver.class);
        AccessTargetResolver<String> targetResolver = mock(AccessTargetResolver.class);
        when(environmentResolver.resolve(source)).thenReturn(environment);
        when(identityResolver.resolve(source)).thenReturn(identity);
        when(targetResolver.resolve(source)).thenReturn(target);
        AbstractAccessContextAdapter<String> adapter = new TestAccessContextAdapter(
                environmentResolver,
                identityResolver,
                targetResolver
        );

        AccessContext context = adapter.adapt(source);

        assertThat(context.getEnvironment()).isSameAs(environment);
        assertThat(context.getIdentity()).isSameAs(identity);
        assertThat(context.getTarget()).isSameAs(target);
        assertThat(context.getAttributes()).isEmpty();
        verify(environmentResolver).resolve(source);
        verify(identityResolver).resolve(source);
        verify(targetResolver).resolve(source);
    }

    @Test
    void constructorRequiresEveryResolver() {
        AccessEnvironmentResolver<String> environment = source -> AccessEnvironment.UNSPECIFIED;
        AccessIdentityResolver<String> identity = source -> null;
        AccessTargetResolver<String> target = source -> null;

        assertThatNullPointerException()
                .isThrownBy(() -> new TestAccessContextAdapter(null, identity, target))
                .withMessage("environmentResolver must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> new TestAccessContextAdapter(environment, null, target))
                .withMessage("identityResolver must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> new TestAccessContextAdapter(environment, identity, null))
                .withMessage("targetResolver must not be null");
    }

    private static final class TestAccessContextAdapter extends AbstractAccessContextAdapter<String> {
        private TestAccessContextAdapter(AccessEnvironmentResolver<String> environmentResolver,
                                         AccessIdentityResolver<String> identityResolver,
                                         AccessTargetResolver<String> targetResolver) {
            super(environmentResolver, identityResolver, targetResolver);
        }
    }
}
