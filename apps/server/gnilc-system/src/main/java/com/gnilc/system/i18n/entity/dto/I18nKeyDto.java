package com.gnilc.system.i18n.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 国际化 key 请求。
 */
@Data
public class I18nKeyDto {

    @NotBlank(message = "{system.i18n.key.required}")
    @Size(max = 191, message = "{system.i18n.validation.key.tooLong}")
    private String i18nKey;
}
