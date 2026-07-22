package com.gnilc.system.i18n.entity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 国际化消息统一保存请求。
 */
@Data
public class I18nDto {

    @NotBlank(message = "{system.i18n.key.required}")
    @Size(max = 191, message = "{system.i18n.validation.key.tooLong}")
    private String i18nKey;

    @Size(max = 191, message = "{system.i18n.validation.key.tooLong}")
    private String previousKey;

    @Valid
    private List<I18nValueDto> values;
}
