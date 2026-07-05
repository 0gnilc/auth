package com.gnilc.auth.authz.denied;

import com.gnilc.auth.authz.context.AccessContext;

/**
 * 访问拒绝处理策略。
 * <p>
 * Handler 可以存在多个，默认 {@link DefaultAccessDenied} 会收集并执行这些策略。每个 handler 只应处理自己支持的
 * 拒绝上下文；可以通过覆盖 {@link #supports(AccessContext, AccessDeniedContext)} 声明支持范围，也可以在
 * {@link #handle(AccessContext, AccessDeniedContext)} 内部自行判断并 no-op。
 */
public interface AccessDeniedHandler {
    /**
     * 判断当前 handler 是否支持本次拒绝处理。
     *
     * @param accessContext 访问上下文
     * @param deniedContext 执行环境拒绝处理上下文
     * @return 支持返回 {@code true}
     */
    default boolean supports(AccessContext accessContext, AccessDeniedContext deniedContext) {
        return true;
    }

    /**
     * 处理访问拒绝。
     *
     * @param accessContext 访问上下文
     * @param deniedContext 执行环境拒绝处理上下文
     */
    void handle(AccessContext accessContext, AccessDeniedContext deniedContext);
}
