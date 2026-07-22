package com.gnilc.system.i18n.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 单个语种的国际化值。
 */
@Data
@AllArgsConstructor
public class I18nValueVo {
    private String locale;
    private String value;
}
