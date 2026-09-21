package com.gnilc.core.i18n;

import java.util.List;

/** 动态国际化消息支持的分类与语言常量，与编辑内容语言集合独立。 */
public final class I18nMessageConstants {

    /** 默认动态消息分类的固定编码 default。 */
    public static final String DEFAULT_CATEGORY = "default";
    /** 管理员侧动态消息分类的固定编码 admin。 */
    public static final String ADMIN_CATEGORY = "admin";
    /** 允许保存及查询的消息分类，固定包含 default 和 admin。 */
    public static final List<String> SUPPORTED_CATEGORIES = List.of(DEFAULT_CATEGORY, ADMIN_CATEGORY);
    /** 动态消息支持的语言代码，固定为 zh-CN 和 en-US；保存时要求英语回退值。 */
    public static final List<String> SUPPORTED_LOCALES = List.of("zh-CN", "en-US");

}
