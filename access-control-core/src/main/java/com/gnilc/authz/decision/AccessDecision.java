package com.gnilc.authz.decision;

import com.gnilc.authz.context.AccessContext;

/**
 * 访问决策器。
 * <p>
 * 决策器只判断一次访问是否允许，不负责解析权限或处理拒绝结果。
 */
public interface AccessDecision {
    /**
     * 根据访问上下文执行授权决策。
     *
     * @param context 访问上下文
     * @return 决策通过返回 {@code true}，否则返回 {@code false}
     */
    boolean decide(AccessContext context);
}
