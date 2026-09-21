package com.gnilc.core.context;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.i18n.I18nMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 从受信任 Servlet 主体读取 RBAC 用户 ID，区分必需身份与允许匿名的读取入口。 */
class UserContextServiceTest {
    private final UserContextService userContextService = new UserContextService(messages());

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void exposesTheTrustedUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of(42L));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(userContextService.getUserId()).isEqualTo(42L);
    }

    @Test
    void rejectsAnUnauthenticatedRequest() {
        assertThatThrownBy(userContextService::getUserId)
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Your login has expired. Please sign in again.");
    }

    @Test
    void optionalLookupReturnsNullForAnUnauthenticatedRequest() {
        assertThat(userContextService.findUserId()).isNull();
    }

    private static I18nMessageService messages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/system/messages");
        source.setDefaultEncoding("UTF-8");
        return new I18nMessageService(source, "en-US");
    }
}
