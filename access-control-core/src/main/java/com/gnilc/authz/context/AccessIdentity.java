package com.gnilc.authz.context;

import java.util.Collections;
import java.util.Map;

/**
 * 一次访问的身份事实。
 * <p>
 * 身份可以表示用户、匿名访问、系统任务或服务账号；具体来源由环境 adapter 决定。
 */
public class AccessIdentity {
    private final String identifier;
    private final Map<String, Object> attributes;

    /**
     * 创建访问身份。
     *
     * @param identifier 身份标识
     * @param attributes 身份补充事实，传入 {@code null} 时按空 Map 处理
     */
    public AccessIdentity(String identifier, Map<String, Object> attributes) {
        this.identifier = identifier;
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
    }

    /**
     * @return 身份标识
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return 身份补充事实
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
