package com.gnilc.auth.authz.context;

import java.util.Locale;
import java.util.Objects;

/**
 * 访问环境。
 * <p>
 * 访问环境是一次授权判断所属的执行环境类型。当前默认预置 Servlet 访问环境；其他环境可通过
 * {@link #of(String)} 扩展。它是 provider 判断是否参与本次授权判断的一等事实；执行环境对象仍应由
 * adapter 翻译为 {@link AccessContext} 后再进入授权核心。
 */
public final class AccessEnvironment {
    /**
     * 未指定访问环境。
     */
    public static final AccessEnvironment UNSPECIFIED = new AccessEnvironment("unspecified");
    /**
     * Servlet 访问环境。
     */
    public static final AccessEnvironment SERVLET = new AccessEnvironment("servlet");

    private final String identifier;

    private AccessEnvironment(String identifier) {
        this.identifier = identifier;
    }

    /**
     * 根据环境标识创建访问环境。
     * <p>
     * 标识会去除首尾空白并转换为小写；空标识按未指定环境处理。
     */
    public static AccessEnvironment of(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return UNSPECIFIED;
        }
        String normalized = identifier.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "servlet" -> SERVLET;
            case "unspecified" -> UNSPECIFIED;
            default -> new AccessEnvironment(normalized);
        };
    }

    /**
     * @return 访问环境标识
     */
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessEnvironment that = (AccessEnvironment) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public String toString() {
        return identifier;
    }
}
