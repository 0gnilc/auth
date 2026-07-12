package com.gnilc.auth.authz.context;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * 一次授权判断所需的核心访问事实输入。
 * <p>
 * 访问上下文包含访问环境、访问身份、访问目标和补充属性。执行环境对象（如 Servlet request、MQ message）
 * 应先由 adapter 翻译为访问上下文，再交给授权与权限校验核心 module 使用。
 */
public class AccessContext {
    private final AccessEnvironment environment;
    private final AccessIdentity identity;
    private final AccessTarget target;
    private final Map<String, Object> attributes;

    /**
     * 创建未指定访问环境的访问上下文。
     *
     * @param identity 访问身份
     * @param target   访问目标
     */
    public AccessContext(AccessIdentity identity, AccessTarget target) {
        this(AccessEnvironment.UNSPECIFIED, identity, target, null);
    }

    /**
     * 创建未指定访问环境的访问上下文。
     *
     * @param identity   访问身份
     * @param target     访问目标
     * @param attributes 补充授权事实，传入 {@code null} 时按空 Map 处理
     */
    public AccessContext(AccessIdentity identity, AccessTarget target, Map<String, Object> attributes) {
        this(AccessEnvironment.UNSPECIFIED, identity, target, attributes);
    }

    /**
     * 创建访问上下文。
     *
     * @param environment 访问环境，传入 {@code null} 时按未指定环境处理
     * @param identity    访问身份
     * @param target      访问目标
     */
    public AccessContext(AccessEnvironment environment, AccessIdentity identity, AccessTarget target) {
        this(environment, identity, target, null);
    }

    /**
     * 创建访问上下文。
     *
     * @param environment 访问环境，传入 {@code null} 时按未指定环境处理
     * @param identity    访问身份
     * @param target      访问目标
     * @param attributes  补充授权事实，传入 {@code null} 时按空 Map 处理
     */
    public AccessContext(AccessEnvironment environment, AccessIdentity identity, AccessTarget target, Map<String, Object> attributes) {
        this.environment = environment == null ? AccessEnvironment.UNSPECIFIED : environment;
        this.identity = identity;
        this.target = target;
        this.attributes = Maps.newConcurrentMap();
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
    }

    /**
     * @return 本次访问所属的访问环境
     */
    public AccessEnvironment getEnvironment() {
        return environment;
    }

    /**
     * @return 本次访问的身份事实
     */
    public AccessIdentity getIdentity() {
        return identity;
    }

    /**
     * @return 本次访问指向的受保护目标
     */
    public AccessTarget getTarget() {
        return target;
    }

    /**
     * @return 整次访问的补充授权事实
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
