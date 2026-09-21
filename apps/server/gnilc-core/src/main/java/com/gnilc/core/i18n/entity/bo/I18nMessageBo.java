package com.gnilc.core.i18n.entity.bo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/** 动态国际化消息的单语言持久化记录，消息键在分类之间保持全局唯一。 */
@Data
@TableName("sys_i18n")
public class I18nMessageBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 动态消息分类，取 default 或 admin；用于分组及运行时语言包范围，不改变消息键身份。 */
    private String category;

    /** 全局唯一的动态消息键，以点分路径表示层级，身份不随消息分类改变。 */
    private String messageKey;

    /** 该翻译的语言代码，当前动态消息支持 zh-CN、en-US。 */
    private String locale;

    /** 该消息键在指定语言下的翻译原文。 */
    private String i18nValue;

    /** 记录创建时间，使用 UTC 时间点。 */
    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;

    /** 记录最近更新时间，使用 UTC 时间点；尚未更新时可为空。 */
    @TableField(fill = FieldFill.UPDATE)
    private Instant updateTime;
}
