package com.gnilc.authz.provider;

import com.gnilc.authz.context.AccessContext;

import java.util.List;

/**
 * 所需权限提供者。
 * <p>
 * 根据访问上下文解析访问目标要求的权限集合。
 */
public interface RequiredPermissionsProvider {
    /**
     * 提供一次访问所需的权限。
     *
     * @param context 访问上下文
     * @return 所需权限列表
     */
    List<Permission> provide(AccessContext context);
}
