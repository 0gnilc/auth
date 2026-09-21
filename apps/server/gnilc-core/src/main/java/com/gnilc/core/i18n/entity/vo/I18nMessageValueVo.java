package com.gnilc.core.i18n.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 动态消息的一种已保存语言翻译。 */
@Data
@AllArgsConstructor
public class I18nMessageValueVo {
    /** 动态消息语言代码，取 zh-CN 或 en-US。 */
    private String locale;
    /** 该消息在此语言下已保存的翻译原文。 */
    private String value;
}
