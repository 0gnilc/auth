package com.gnilc.authz.context;

import java.util.Collections;
import java.util.Map;

/**
 * 一次授权判断所需的环境无关输入。
 * <p>
 * 执行环境对象（如 Servlet request、MQ message）应先由 adapter 翻译为访问上下文，
 * 再交给授权核心模块使用。
 */
public class AccessContext {
    private final AccessIdentity identity;
    private final AccessTarget target;
    private final Map<String, Object> attributes;

    /**
     * 创建访问上下文。
     *
     * @param identity   访问身份
     * @param target     访问目标
     * @param attributes 补充授权事实，传入 {@code null} 时按空 Map 处理
     */
    public AccessContext(AccessIdentity identity, AccessTarget target, Map<String, Object> attributes) {
        this.identity = identity;
        this.target = target;
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
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
