package com.gnilc.core.i18n.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 动态消息的一种语言更新值，null 用于删除该语言翻译。 */
@Data
public class I18nMessageValueDto {

    /** 必填动态消息语言代码，取 zh-CN 或 en-US。 */
    @NotBlank(message = "{system.i18n.locale.required}")
    private String locale;

    /** 本语言翻译原文，至多 4000 个字符；null 删除此语言，空字符串按原值保存，但 en-US 必须非空白。 */
    private String value;
}
