package com.gnilc.auth.authz.rbac.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

/**
 * 菜单类型。
 * <p>
 * 数据库和 JSON 均使用小写字符串值。
 */
@Getter
public enum MenuType {
    /** 目录。 */
    CATALOG("catalog"),
    /** 页面菜单。 */
    MENU("menu"),
    /** 内嵌 iframe。 */
    EMBEDDED("embedded"),
    /** 外链。 */
    LINK("link"),
    /** 按钮。 */
    BUTTON("button");

    @EnumValue
    @JsonValue
    private final String value;

    MenuType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static MenuType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported menu type: " + value));
    }
}
