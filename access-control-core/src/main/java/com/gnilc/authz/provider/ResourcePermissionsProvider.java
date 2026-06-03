package com.gnilc.authz.provider;

import java.util.List;

/**
 * 访问资源需要的权限提供者
 */
public interface ResourcePermissionsProvider {
    /**
     * 访问资源需要的权限
     *
     * @return 访问资源需要的权限列表
     */
    List<Permission> provide();
}
