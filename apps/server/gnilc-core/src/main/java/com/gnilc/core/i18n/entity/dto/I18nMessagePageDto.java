package com.gnilc.core.i18n.entity.dto;

import com.gnilc.common.utils.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 按消息键分页检索动态国际化消息的可选条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class I18nMessagePageDto extends PageParams {
    /** 按全局消息键模糊匹配的可选关键词；空白值不筛选。 */
    private String key;
    /** 按任一翻译值模糊匹配的可选关键词；与 locale 同时提供时限定该语言。 */
    private String value;
    /** 可选的消息分类，取 default 或 admin；未提供时查询支持的全部分类。 */
    private String category;
    /** 可选的翻译语言，取 zh-CN 或 en-US；只限定筛选，响应仍携带命中消息全部已保存语言值。 */
    private String locale;
}
