package com.gnilc.core.i18n.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** 按全局消息键聚合的动态国际化消息。 */
@Data
@AllArgsConstructor
public class I18nMessageVo {
    /** 动态消息分类，取 default 或 admin；用于分组及运行时语言包范围，不改变消息键身份。 */
    private String category;
    /** 全局唯一的动态消息键，以点分路径表示层级，身份不随消息分类改变。 */
    private String messageKey;
    /** 按语言聚合的已保存翻译值。 */
    private List<I18nMessageValueVo> values;
}
