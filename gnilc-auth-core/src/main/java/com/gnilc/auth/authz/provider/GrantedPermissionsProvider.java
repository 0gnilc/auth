package com.gnilc.auth.authz.provider;

import com.gnilc.auth.authz.context.AccessContext;

import java.util.List;

/**
 * 已授予权限提供者。
 * <p>
 * 根据访问上下文解析访问身份可使用的权限集合。
 */
public interface GrantedPermissionsProvider {
    /**
     * 判断当前提供者是否参与该访问上下文。
     * <p>
     * 默认参与所有访问环境；多环境实现应根据 {@link AccessContext#getEnvironment()} 明确限定参与范围。
     *
     * @param context 访问上下文
     * @return 是否参与当前授权判断
     */
    default boolean supports(AccessContext context) {
        return true;
    }

    /**
     * 提供一次访问中已授予的权限。
     *
     * @param context 访问上下文
     * @return 已授予权限列表
     */
    List<Permission> provide(AccessContext context);
}
