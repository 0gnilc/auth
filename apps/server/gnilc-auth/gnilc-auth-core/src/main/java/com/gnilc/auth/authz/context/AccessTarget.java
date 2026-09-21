package com.gnilc.auth.authz.context;

import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;

/**
 * 一次访问指向的受保护目标。
 *  <p>
 *  目标可以是 Servlet 路由、消息主题、任务入口或业务访问点。限定符用于区分同一目标下的变体，
 *  例如 HTTP method 或消息消费方向。
 */
public class AccessTarget {
    /** 受保护目标的标识，具体匹配方式由权限提供方约定。 */
    private final String identifier;
    /** 目标限定符，如 HTTP 方法；可为空以表示未限定变体。 */
    private final String qualifier;
    /** 目标补充事实；构造参数为 {@code null} 时创建空 Map。 */
    private final Map<String, Object> attributes;


    /**
     * 创建访问目标。
     *
     * @param identifier 目标标识
     * @param qualifier  目标限定符，可为空
     */
    public AccessTarget(String identifier, String qualifier) {
        this(identifier, qualifier, Maps.newConcurrentMap());
    }

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
        this.attributes = attributes == null ? Maps.newConcurrentMap() : attributes;
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
