package com.gnilc.auth.authn.context;

import java.security.Principal;
import java.util.Map;

/**
 * 认证成功后的访问主体。
 * <p>
 * 访问主体是认证模块向后续链路输出的身份事实，不绑定具体运行环境。
 */
public interface AccessPrincipal extends Principal {
    /**
     * @return 访问主体标识
     */
    String getIdentifier();

    /**
     * @return 认证补充事实
     */
    Map<String, Object> getAttributes();

    /**
     * @return Principal 名称
     */
    @Override
    default String getName() {
        return getIdentifier();
    }
}
