package com.gnilc.auth.authn.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 默认访问主体实现。
 * <p>
 * 该类型仅保存认证后的主体标识与补充事实，不包含 Servlet 或其他运行环境语义。
 */
public final class DefaultAccessPrincipal implements AccessPrincipal {
    private final String identifier;
    private final Map<String, Object> attributes;

    private DefaultAccessPrincipal(String identifier, Map<String, Object> attributes) {
        this.identifier = Objects.requireNonNull(identifier, "identifier == null");
        this.attributes = attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    /**
     * 创建默认访问主体。
     */
    public static DefaultAccessPrincipal of(String identifier, Map<String, Object> attributes) {
        return new DefaultAccessPrincipal(identifier, attributes);
    }

    /**
     * 根据数字访问主体标识创建默认访问主体。
     */
    public static DefaultAccessPrincipal of(Long identifier, Map<String, Object> attributes) {
        return new DefaultAccessPrincipal(String.valueOf(Objects.requireNonNull(identifier, "identifier == null")), attributes);
    }

    /**
     * 创建不带补充事实的默认访问主体。
     */
    public static DefaultAccessPrincipal of(String identifier) {
        return new DefaultAccessPrincipal(identifier, Map.of());
    }

    /**
     * 根据数字访问主体标识创建不带补充事实的默认访问主体。
     */
    public static DefaultAccessPrincipal of(Long identifier) {
        return new DefaultAccessPrincipal(String.valueOf(Objects.requireNonNull(identifier, "identifier == null")), Map.of());
    }

    /**
     * @return 访问主体标识
     */
    @Override
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return 认证补充事实
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
