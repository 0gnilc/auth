package com.gnilc.core.session;

import lombok.Getter;

/** 会话访问令牌和刷新令牌。 */
@Getter
public final class SessionTokenPair {
    /** 用于访问当前身份受保护资源的访问令牌。 */
    private final String accessToken;
    /** 用于换取新令牌对的刷新令牌，与访问令牌用途不同。 */
    private final String refreshToken;

    private SessionTokenPair(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static SessionTokenPair of(String accessToken, String refreshToken) {
        return new SessionTokenPair(accessToken, refreshToken);
    }
}
