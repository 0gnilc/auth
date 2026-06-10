package com.gnilc.authn.web.handler;

import com.google.common.base.Preconditions;

import java.security.Principal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet 认证结果。
 * <p>
 * 结果只表达认证是否成功；不表达授权结论。
 */
public class AuthenticationResult {
    private final boolean authenticated;
    private final Principal principal;
    private final Map<String, Object> attributes;
    private final String reason;
    private final Throwable cause;

    private AuthenticationResult(boolean authenticated,
                                 Principal principal,
                                 Map<String, Object> attributes,
                                 String reason,
                                 Throwable cause) {
        this.authenticated = authenticated;
        this.principal = principal;
        this.attributes = attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        this.reason = reason;
        this.cause = cause;
    }

    /**
     * 创建认证成功结果。
     *
     * @param principal 认证主体
     * @return 认证成功结果
     */
    public static AuthenticationResult authenticated(Principal principal) {
        return authenticated(principal, Map.of());
    }

    /**
     * 创建认证成功结果。
     *
     * @param principal  认证主体
     * @param attributes 认证补充事实
     * @return 认证成功结果
     */
    public static AuthenticationResult authenticated(Principal principal, Map<String, Object> attributes) {
        Preconditions.checkArgument(principal != null, "principal == null!");
        return new AuthenticationResult(true, principal, attributes, null, null);
    }

    /**
     * 创建认证失败结果。
     *
     * @param reason 失败原因
     * @return 认证失败结果
     */
    public static AuthenticationResult failed(String reason) {
        return failed(reason, null);
    }

    /**
     * 创建认证失败结果。
     *
     * @param reason 失败原因
     * @param cause  失败异常
     * @return 认证失败结果
     */
    public static AuthenticationResult failed(String reason, Throwable cause) {
        return new AuthenticationResult(false, null, Map.of(), reason, cause);
    }

    /**
     * @return 是否认证成功
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * @return 认证主体，认证失败时为 {@code null}
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * @return 认证补充事实
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * @return 失败原因
     */
    public String getReason() {
        return reason;
    }

    /**
     * @return 失败异常
     */
    public Throwable getCause() {
        return cause;
    }
}
