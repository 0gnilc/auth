package com.gnilc.core.session;

import lombok.Data;

import java.time.Duration;

/** 定义一种身份会话的令牌和 Redis 策略。 */
@Data
final class SessionPolicy {
    /** 非空令牌前缀，用于区分不同身份会话的令牌。 */
    private final String tokenPrefix;
    /** 非空 Redis 键命名空间，用于隔离身份类型的会话记录。 */
    private final String redisNamespace;
    /** 访问令牌有效时长，必须为正。 */
    private final Duration accessTtl;
    /** 刷新令牌有效时长，必须为正。 */
    private final Duration refreshTtl;

    SessionPolicy(String tokenPrefix, String redisNamespace, Duration accessTtl, Duration refreshTtl) {
        if (tokenPrefix == null || tokenPrefix.isBlank()) {
            throw new IllegalArgumentException("tokenPrefix is blank");
        }
        if (redisNamespace == null || redisNamespace.isBlank()) {
            throw new IllegalArgumentException("redisNamespace is blank");
        }
        if (accessTtl == null || accessTtl.isNegative() || accessTtl.isZero()) {
            throw new IllegalArgumentException("accessTtl must be positive");
        }
        if (refreshTtl == null || refreshTtl.isNegative() || refreshTtl.isZero()) {
            throw new IllegalArgumentException("refreshTtl must be positive");
        }
        this.tokenPrefix = tokenPrefix;
        this.redisNamespace = redisNamespace;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    String accessKey(Long userId, String token) {
        return accessKeyPrefix(userId) + token;
    }

    String accessKeyPrefix(Long userId) {
        return redisNamespace + ":at:" + userId + ":";
    }

    String accessPattern(Long userId) {
        return accessKeyPrefix(userId) + "*";
    }

    String refreshKey(Long userId, String token) {
        return redisNamespace + ":rt:" + userId + ":" + token;
    }

    String refreshPattern(Long userId) {
        return redisNamespace + ":rt:" + userId + ":*";
    }
}
