package com.gnilc.system.i18n.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 按 key 聚合的国际化消息。
 */
@Data
@AllArgsConstructor
public class I18nMessageVo {
    private String i18nKey;
    private List<I18nMessageValueVo> values;
}
