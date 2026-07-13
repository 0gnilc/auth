package com.gnilc.auth.authz.denied;

import com.gnilc.auth.authz.context.AccessContext;

/**
 * 访问拒绝入口。
 * <p>
 * 该 module 是访问拒绝的全局入口，负责在 {@link com.gnilc.auth.authz.decision.AccessDecision}
 * 拒绝后执行访问拒绝。它不参与权限校验。
 */
public interface AccessDenied {
    /**
     * 执行访问拒绝。
     *
     * @param accessContext 访问上下文
     * @param deniedContext 执行环境拒绝上下文
     */
    void denied(AccessContext accessContext, AccessDeniedContext deniedContext);
}
