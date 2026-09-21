package com.gnilc.core.i18n.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** 动态消息分页中的消息键聚合项，携带全部已保存语言值。 */
@Data
@AllArgsConstructor
public class I18nMessageItemVo {
    /** 动态消息分类，取 default 或 admin；用于分组及运行时语言包范围，不改变消息键身份。 */
    private String category;
    /** 全局唯一的动态消息键，以点分路径表示层级，身份不随消息分类改变。 */
    private String messageKey;
    /** 按语言聚合的已保存翻译值。 */
    private List<I18nMessageValueVo> values;
}
