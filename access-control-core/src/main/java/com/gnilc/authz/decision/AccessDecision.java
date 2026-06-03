package com.gnilc.authz.decision;


/**
 * 访问决策器
 */
public interface AccessDecision {
    /**
     * 决策
     *
     * @return 决策通过返回 {@code true},
     * 否则返回 {@code false}
     */
    boolean decide();
}
