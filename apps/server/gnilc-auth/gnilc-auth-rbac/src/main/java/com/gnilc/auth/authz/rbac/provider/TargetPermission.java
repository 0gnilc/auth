package com.gnilc.auth.authz.rbac.provider;

import lombok.Getter;

import java.util.Objects;

/**
 * RBAC 目标权限。
 *  <p>
 *  表示访问目标与权限标识之间的绑定关系。
 */
@Getter
public class TargetPermission {
    /** 权限所绑定的访问目标标识，匹配语义由权限提供方约定。 */
    private final String targetIdentifier;
    /** 目标限定符；为空时不限定同一目标的变体。 */
    private final String targetQualifier;
    /** 目标所需的权限标识，用于与已授予权限比较。 */
    private final String code;

    /**
     * 创建目标权限。
     *
     * @param targetIdentifier 访问目标标识，具体匹配语义由 provider 决定
     * @param targetQualifier  访问目标限定符，为空时不限定目标变体
     * @param code             权限标识
     */
    public TargetPermission(
            String targetIdentifier,
            String targetQualifier,
            String code) {
        this.targetIdentifier = targetIdentifier;
        this.targetQualifier = targetQualifier;
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetPermission that = (TargetPermission) o;
        return Objects.equals(targetIdentifier, that.targetIdentifier)
                && Objects.equals(targetQualifier, that.targetQualifier)
                && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetIdentifier, targetQualifier, code);
    }
}
