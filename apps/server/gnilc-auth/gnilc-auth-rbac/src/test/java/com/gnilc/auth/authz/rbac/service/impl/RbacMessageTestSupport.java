package com.gnilc.auth.authz.rbac.service.impl;

import com.gnilc.common.i18n.I18nMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

abstract class RbacMessageTestSupport {
    @BeforeEach
    void useEnglishLocale() {
        LocaleContextHolder.setLocale(Locale.US);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    I18nMessageService messages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/rbac/messages");
        source.setDefaultEncoding("UTF-8");
        return new I18nMessageService(source, "en-US");
    }
}
