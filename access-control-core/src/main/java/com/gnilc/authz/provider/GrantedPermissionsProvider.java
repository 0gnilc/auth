package com.gnilc.authz.provider;

import com.gnilc.authz.context.AccessContext;

import java.util.List;

/**
 * 已授予权限提供者。
 * <p>
 * 根据访问上下文解析访问身份可使用的权限集合。
 */
public interface GrantedPermissionsProvider {
    /**
     * 提供一次访问中已授予的权限。
     *
     * @param context 访问上下文
     * @return 已授予权限列表
     */
    List<Permission> provide(AccessContext context);
}
