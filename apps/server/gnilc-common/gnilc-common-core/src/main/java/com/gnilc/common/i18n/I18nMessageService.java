package com.gnilc.common.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

/**
 * 读取当前请求语言对应的后端静态国际化文案。
 */
@Service
public class I18nMessageService {

    private static final Logger log = LoggerFactory.getLogger(I18nMessageService.class);
    private static final Locale FALLBACK_LOCALE = SupportedLocale.EN_US.toLocale();

    private final MessageSource messageSource;
    private final Locale defaultLocale;

    public I18nMessageService(
            MessageSource messageSource,
            @Value("${app.i18n.default-locale:en-US}") String defaultLocale) {
        this.messageSource = Objects.requireNonNull(messageSource, "messageSource");
        this.defaultLocale = toLocale(defaultLocale);
    }

    public String get(String code) {
        return get(code, LocaleContextHolder.getLocale(), new Object[0]);
    }

    public String get(String code, Object... args) {
        return get(code, LocaleContextHolder.getLocale(), args);
    }

    public String get(String code, Locale locale, Object... args) {
        String messageCode = requireCode(code);
        Object[] messageArgs = args == null ? new Object[0] : args;
        Locale targetLocale = normalize(locale);
        String message = messageSource.getMessage(messageCode, messageArgs, null, targetLocale);
        if (message == null) {
            log.warn("Missing i18n message: code={}, locale={}", messageCode, targetLocale);
            return messageCode;
        }
        return message;
    }

    public String getOrDefault(String code, String defaultMessage, Object... args) {
        String messageCode = requireCode(code);
        Object[] messageArgs = args == null ? new Object[0] : args;
        return messageSource.getMessage(
                messageCode,
                messageArgs,
                defaultMessage,
                normalize(LocaleContextHolder.getLocale()));
    }

    private Locale normalize(Locale locale) {
        return SupportedLocale.normalize(locale, defaultLocale);
    }

    private static Locale toLocale(String languageTag) {
        return SupportedLocale.supports(languageTag)
                ? Locale.forLanguageTag(languageTag)
                : FALLBACK_LOCALE;
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Internationalization code must not be blank.");
        }
        return code;
    }
}
