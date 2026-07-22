package com.gnilc.system.i18n.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 国际化消息分页项。
 */
@Data
@AllArgsConstructor
public class I18nItemVo {
    private String client;
    private String i18nKey;
    private List<I18nValueVo> values;
}
