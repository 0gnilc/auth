package com.gnilc.auth.authz.context;

/**
 * 访问身份解析处理器。
 * <p>
 * Handler 是 {@link AccessIdentityResolver} 的下级协作组件，一个 handler 负责处理一种身份来源；
 * 完整的身份解析入口仍由 resolver 暴露。
 *
 * @param <T> 执行环境对象类型
 */
public interface AccessIdentityResolverHandler<T> {
    /**
     * 判断当前 handler 是否支持该执行环境对象。
     *
     * @param source 执行环境对象
     * @return 是否支持当前来源
     */
    boolean supports(T source);

    /**
     * 处理访问身份解析。
     *
     * @param source 执行环境对象
     * @return 访问身份
     */
    AccessIdentity handle(T source);
}
