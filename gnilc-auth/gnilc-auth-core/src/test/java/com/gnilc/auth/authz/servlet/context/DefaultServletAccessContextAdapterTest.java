package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessEnvironment;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultServletAccessContextAdapterTest {
    @Test
    void extractsPrincipalAndContextRelativeTarget() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders/7");
        request.setContextPath("/api");
        request.setUserPrincipal(DefaultAccessPrincipal.of("42", Map.of("tenant", "north")));
        ServletRequestContext source =
                new ServletRequestContext(request, new MockHttpServletResponse(), (req, res) -> { });
        DefaultServletAccessIdentityResolver identities = new DefaultServletAccessIdentityResolver(
                List.of(new DefaultServletAccessIdentityResolverHandler()));
        DefaultServletAccessContextAdapter adapter = new DefaultServletAccessContextAdapter(
                identities, new DefaultServletAccessTargetResolver());

        AccessContext context = adapter.adapt(source);

        assertThat(context.getEnvironment()).isSameAs(AccessEnvironment.SERVLET);
        assertThat(context.getIdentity().getIdentifier()).isEqualTo("42");
        assertThat(context.getIdentity().getAttributes())
                .containsEntry("tenant", "north")
                .containsEntry("principal", true);
        assertThat(context.getTarget().getIdentifier()).isEqualTo("/orders/7");
        assertThat(context.getTarget().getQualifier()).isEqualTo("POST");
    }
}
