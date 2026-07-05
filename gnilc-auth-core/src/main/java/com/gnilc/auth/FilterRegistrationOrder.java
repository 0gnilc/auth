package com.gnilc.auth;

/**
 * Filter 注册顺序。
 * <p>
 * 该类统一管理认证、授权等 Filter 在容器中的注册顺序，避免 authn 与 authz 模块互相引用。
 */
public final class FilterRegistrationOrder {
    public static final int SERVLET_AUTHORIZATION_FILTER_ORDER = Integer.MAX_VALUE;
    public static final int SERVLET_AUTHENTICATION_FILTER_ORDER = SERVLET_AUTHORIZATION_FILTER_ORDER - 1;

    private FilterRegistrationOrder() {
    }
}
