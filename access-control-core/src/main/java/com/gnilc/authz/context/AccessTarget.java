package com.gnilc.authz.context;

import java.util.Collections;
import java.util.Map;

/**
 * 一次访问指向的受保护目标。
 * <p>
 * 目标可以是 Web 路由、消息主题、任务入口或业务访问点。限定符用于区分同一目标下的变体，
 * 例如 HTTP method 或消息消费方向。
 */
public class AccessTarget {
    private final String identifier;
    private final String qualifier;
    private final Map<String, Object> attributes;

    /**
     * 创建访问目标。
     *
     * @param identifier 目标标识
     * @param qualifier  目标限定符，可为空
     * @param attributes 目标补充事实，传入 {@code null} 时按空 Map 处理
     */
    public AccessTarget(String identifier, String qualifier, Map<String, Object> attributes) {
        this.identifier = identifier;
        this.qualifier = qualifier;
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
    }

    /**
     * @return 目标标识
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return 目标限定符
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * @return 目标补充事实
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
