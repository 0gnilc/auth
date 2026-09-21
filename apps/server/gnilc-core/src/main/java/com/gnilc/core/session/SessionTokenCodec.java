package com.gnilc.core.session;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 生成并解析带身份命名空间的会话令牌。
 */
final class SessionTokenCodec {
    private static final int RANDOM_BYTES = 32;
    private static final String SEPARATOR = ".";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final String prefix;

    SessionTokenCodec(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix is blank");
        }
        this.prefix = prefix;
    }

    /** 生成身份前缀、RBAC 用户 ID 和密码学随机串组成的不透明令牌；有效性由 Redis 记录决定。 */
    String issue(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId == null");
        }
        byte[] random = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(random);
        return prefix + SEPARATOR + userId + SEPARATOR + ENCODER.encodeToString(random);
    }

    /** 解析命名空间和用户 ID，不将可解析的字符串视为已认证凭据。 */
    Long resolve(String token) {
        if (!matches(token)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        int first = token.indexOf(SEPARATOR);
        int second = token.indexOf(SEPARATOR, first + 1);
        if (second <= first + 1 || second == token.length() - 1) {
            throw new IllegalArgumentException("Invalid session token");
        }
        try {
            return Long.valueOf(token.substring(first + 1, second));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid session token", e);
        }
    }

    /** 仅检查身份域前缀，用于认证处理器分流，不检查令牌格式或存储有效性。 */
    boolean matches(String token) {
        return token != null && token.startsWith(prefix + SEPARATOR);
    }
}
