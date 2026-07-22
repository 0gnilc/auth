package com.gnilc.system.i18n.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 单个语种的国际化值。
 */
@Data
public class I18nValueDto {

    @NotBlank(message = "{system.i18n.locale.required}")
    private String locale;

    @Size(max = 4000, message = "{system.i18n.validation.value.tooLong}")
    private String value;
}
