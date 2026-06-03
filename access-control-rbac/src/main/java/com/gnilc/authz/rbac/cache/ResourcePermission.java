package com.gnilc.authz.rbac.cache;
/**
 * @param symbol   权限标识
 * @param resource 受保护的资源
 */
public record ResourcePermission(String symbol, String resource) {

}
