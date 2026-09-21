package com.gnilc.core.i18n.entity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 动态消息分类、全局消息键及指定语言值的保存输入。 */
@Data
public class I18nMessageDto {

    /** 动态消息分类，取 default 或 admin；用于分组及运行时语言包范围，不改变消息键身份。 */
    @NotBlank(message = "{system.i18n.category.required}")
    private String category;

    /** 必填全局消息键，至多 191 个字符，以点分路径组织；不能与已有键形成父子路径冲突。 */
    @NotBlank(message = "{system.i18n.key.required}")
    @Size(max = 191, message = "{system.i18n.validation.key.tooLong}")
    private String messageKey;

    /** 指定语言的更新项，语言不得重复，必须显式包含非空白的 en-US；未提交语言保留原值，null 值删除对应语言。 */
    @Valid
    private List<I18nMessageValueDto> values;
}
