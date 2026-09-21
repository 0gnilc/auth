package com.gnilc.auth.authn.servlet.context;

import com.gnilc.auth.authn.context.AccessPrincipal;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/** 只从当前 Servlet 请求读取已建立的访问主体，不接受普通 Principal 冒充框架认证身份。 */
class DefaultAccessPrincipalHolderTest {
    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void readsOnlyAccessPrincipalFromCurrentServletRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AccessPrincipal principal = DefaultAccessPrincipal.of("admin");
        request.setUserPrincipal(principal);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(DefaultAccessPrincipalHolder.getPrincipal()).isSameAs(principal);

        RequestContextHolder.resetRequestAttributes();
        assertThat(DefaultAccessPrincipalHolder.getPrincipal()).isNull();
    }
}
