package com.gnilc.system.auth;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.i18n.I18nMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessPrincipalUtilsTest {
    private AccessPrincipalUtils accessPrincipalUtils;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.US);
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/system/messages");
        source.setDefaultEncoding("UTF-8");
        accessPrincipalUtils = new AccessPrincipalUtils(new I18nMessageService(source, "en-US"));
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void getUserIdReadsTheCurrentAccessPrincipal() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of(42L));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(accessPrincipalUtils.getUserId()).isEqualTo(42L);
    }

    @Test
    void getUserIdRejectsAnUnauthenticatedRequest() {
        assertThatThrownBy(accessPrincipalUtils::getUserId)
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Your session is no longer valid. Sign in again.");
    }

    @Test
    void getUserIdUsesTheRequestLocaleForAnInvalidSession() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);

        assertThatThrownBy(accessPrincipalUtils::getUserId)
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("当前会话已失效，请重新登录。");
    }
}
