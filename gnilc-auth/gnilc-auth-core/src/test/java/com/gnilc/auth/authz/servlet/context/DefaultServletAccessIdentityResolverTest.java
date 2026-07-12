package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.context.AccessIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultServletAccessIdentityResolverTest {
    @Test
    void fallsBackToAnonymousWithoutPrincipal() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public");
        ServletRequestContext source =
                new ServletRequestContext(request, new MockHttpServletResponse(), (req, res) -> { });

        AccessIdentity identity = new DefaultServletAccessIdentityResolver(
                List.of(new DefaultServletAccessIdentityResolverHandler())).resolve(source);

        assertThat(identity.getIdentifier()).isNull();
        assertThat(identity.getAttributes()).containsEntry("anonymous", true);
    }
}
