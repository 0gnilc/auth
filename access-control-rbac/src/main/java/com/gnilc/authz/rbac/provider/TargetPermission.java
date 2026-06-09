package com.gnilc.authz.rbac.provider;

import lombok.Getter;

import java.util.Objects;

/**
 * RBAC 目标权限。
 * <p>
 * 表示访问目标与权限标识之间的绑定关系。
 */
@Getter
public class TargetPermission {
    private final String targetIdentifier;
    private final String code;

    /**
     * 创建目标权限。
     *
     * @param targetIdentifier 访问目标标识，具体匹配语义由 provider 决定
     * @param code             权限标识
     */
    public TargetPermission(String targetIdentifier, String code) {
        this.targetIdentifier = targetIdentifier;
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetPermission that = (TargetPermission) o;
        return Objects.equals(targetIdentifier, that.targetIdentifier) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetIdentifier, code);
    }
}
