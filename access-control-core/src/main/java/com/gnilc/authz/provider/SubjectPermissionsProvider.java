package com.gnilc.authz.provider;

import java.util.List;

/**
 * 访问者拥有的权限提供者
 */
public interface SubjectPermissionsProvider {
    /**
     * 提供拥有的权限
     *
     * @return 提供拥有的权限列表
     */
    List<Permission> provide();
}
