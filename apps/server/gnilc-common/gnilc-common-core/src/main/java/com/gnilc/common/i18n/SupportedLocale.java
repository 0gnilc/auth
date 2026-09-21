package com.gnilc.common.i18n;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** 应用支持的语言。 */
public enum SupportedLocale {
    /** 简体中文（中国）。 */
    ZH_CN("zh-CN"),
    /** 英语（美国），也是未匹配语言标签时的默认语言。 */
    EN_US("en-US"),
    /** 豪萨语（尼日利亚）。 */
    HA_NG("ha-NG"),
    /** 约鲁巴语（尼日利亚）。 */
    YO_NG("yo-NG");

    /** 用于语言协商和消息资源定位的 BCP 47 语言标签。 */
    private final String code;

    SupportedLocale(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public Locale toLocale() {
        return Locale.forLanguageTag(code);
    }

    public static boolean supports(String code) {
        return Arrays.stream(values()).anyMatch(locale -> locale.code.equals(code));
    }

    public static List<String> codes() {
        return Arrays.stream(values()).map(SupportedLocale::getCode).toList();
    }

    public static List<Locale> locales() {
        return Arrays.stream(values()).map(SupportedLocale::toLocale).toList();
    }

    public static Locale fromLanguageTagOrDefault(String languageTag) {
        return Arrays.stream(values())
                .filter(locale -> locale.code.equals(languageTag))
                .findFirst()
                .orElse(EN_US)
                .toLocale();
    }

    public static Locale normalize(Locale locale, Locale fallback) {
        if (locale == null || !supports(locale.toLanguageTag())) {
            return fallback;
        }
        return locale;
    }
}
