package com.gnilc.core.i18n;

import com.gnilc.common.i18n.I18nMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证系统错误消息在受支持语言中可加载，业务错误无需依赖聊天中的翻译约定。 */
class SystemMessageBundleTest {

    @Test
    void loadsLocalizedSystemMessagesForSupportedLocales() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/system/messages");
        source.setDefaultEncoding("UTF-8");
        I18nMessageService messages = new I18nMessageService(source, "en-US");

        assertThat(messages.get("system.auth.session.expired", Locale.SIMPLIFIED_CHINESE))
                .isNotBlank();
        assertThat(messages.get("system.admin.login.invalidCredentials", Locale.US))
                .isNotBlank();
    }
}
