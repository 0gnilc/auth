package com.gnilc.auth.authn.servlet.context;

import com.gnilc.auth.authn.context.AccessPrincipal;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import static org.assertj.core.api.Assertions.assertThat;

class DefaultAccessPrincipalHolderTest {

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    // TestCaseId: CORE-AUTHN-011
    @Test
    void returnsCurrentRequestPrincipal() {
        AccessPrincipal expected = DefaultAccessPrincipal.of("1001");
        setRequestWithPrincipal(expected);

        AccessPrincipal principal = DefaultAccessPrincipalHolder.getPrincipal();

        assertThat(principal).isSameAs(expected);
    }

    // TestCaseId: CORE-AUTHN-012
    @Test
    void returnsNullWhenRequestHasNoPrincipal() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        AccessPrincipal principal = DefaultAccessPrincipalHolder.getPrincipal();

        assertThat(principal).isNull();
    }

    // TestCaseId: CORE-AUTHN-013
    @Test
    void returnsNullOutsideRequestContext() {
        AccessPrincipal principal = DefaultAccessPrincipalHolder.getPrincipal();

        assertThat(principal).isNull();
    }

    // TestCaseId: CORE-AUTHN-014
    @Test
    void returnsNullWhenRequestPrincipalIsNotAccessPrincipal() {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public java.security.Principal getUserPrincipal() {
                return () -> "1001";
            }
        };
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AccessPrincipal principal = DefaultAccessPrincipalHolder.getPrincipal();

        assertThat(principal).isNull();
    }

    private void setRequestWithPrincipal(AccessPrincipal principal) {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public java.security.Principal getUserPrincipal() {
                return principal;
            }
        };
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
