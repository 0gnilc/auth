package com.gnilc.auth.authz.denied;

/**
 * 访问拒绝处理上下文。
 * <p>
 * 该 marker interface 表示执行环境在拒绝处理阶段需要保留的上下文对象。它与
 * {@link com.gnilc.auth.authz.context.AccessContext} 分离：AccessContext 只保存授权事实，
 * AccessDeniedContext 保存执行拒绝动作所需的环境上下文。
 */
public interface AccessDeniedContext {
}
