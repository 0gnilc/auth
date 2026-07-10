package com.gnilc.system.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * 封装后台管理员会话的 Redis 读写。
 */
@Component
public class AdminSessionRedisCommands {
    private static final String ACCESS_PREFIX = "sys:admin:at:";
    private static final String REFRESH_PREFIX = "sys:admin:rt:";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofDays(7);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public AdminSessionRedisCommands(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存访问令牌和刷新令牌。
     */
    void saveSession(Long userId, String accessToken, String refreshToken) {
        saveAccessToken(userId, accessToken, refreshToken);
        redisTemplate.opsForValue().set(refreshKey(userId, refreshToken), accessToken, REFRESH_TOKEN_TTL);
    }

    /**
     * 保存访问令牌映射。
     */
    void saveAccessToken(Long userId, String accessToken, String refreshToken) {
        redisTemplate.opsForValue().set(accessKey(userId, accessToken), refreshToken, ACCESS_TOKEN_TTL);
    }

    /**
     * 判断访问令牌是否存在。
     */
    boolean hasAccessToken(Long userId, String accessToken) {
        return redisTemplate.hasKey(accessKey(userId, accessToken));
    }

    /**
     * 判断刷新令牌是否存在。
     */
    boolean hasRefreshToken(Long userId, String refreshToken) {
        return redisTemplate.hasKey(refreshKey(userId, refreshToken));
    }

    /**
     * 读取刷新令牌绑定的访问令牌。
     */
    String getPairedAccessToken(Long userId, String refreshToken) {
        return redisTemplate.opsForValue().get(refreshKey(userId, refreshToken));
    }

    /**
     * 替换刷新令牌绑定的访问令牌并保留 TTL。
     */
    boolean replacePairedAccessTokenKeepingTtl(Long userId, String refreshToken, String accessToken) {
        String refreshKey = refreshKey(userId, refreshToken);
        return Boolean.TRUE.equals(redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
            byte[] keyBytes = Objects.requireNonNull(serializer.serialize(refreshKey));
            byte[] valueBytes = Objects.requireNonNull(serializer.serialize(accessToken));

            return connection.stringCommands().set(
                    keyBytes,
                    valueBytes,
                    // 保留刷新令牌 TTL。
                    Expiration.keepTtl(),
                    // 仅 refresh key 存在时替换。
                    RedisStringCommands.SetOption.ifPresent()
            );
        }));
    }

    /**
     * 删除访问令牌。
     */
    void deleteAccessToken(Long userId, String accessToken) {
        redisTemplate.delete(accessKey(userId, accessToken));
    }

    /**
     * 删除刷新令牌。
     */
    void deleteRefreshToken(Long userId, String refreshToken) {
        redisTemplate.delete(refreshKey(userId, refreshToken));
    }

    /**
     * 删除用户全部会话令牌。
     */
    void deleteUserSessions(Long userId) {
        deleteKeys(redisTemplate.keys(accessPattern(userId)));
        deleteKeys(redisTemplate.keys(refreshPattern(userId)));
    }

    /**
     * 构造访问令牌 key。
     */
    String accessKey(Long userId, String accessToken) {
        return ACCESS_PREFIX + userId + ":" + accessToken;
    }

    /**
     * 构造刷新令牌 key。
     */
    String refreshKey(Long userId, String refreshToken) {
        return REFRESH_PREFIX + userId + ":" + refreshToken;
    }

    /**
     * 构造访问令牌清理 pattern。
     */
    String accessPattern(Long userId) {
        return ACCESS_PREFIX + userId + ":*";
    }

    /**
     * 构造刷新令牌清理 pattern。
     */
    String refreshPattern(Long userId) {
        return REFRESH_PREFIX + userId + ":*";
    }

    /**
     * 批量删除 key。
     */
    private void deleteKeys(Set<String> keys) {
        if (!CollectionUtils.isEmpty(keys)) {
            redisTemplate.delete(keys);
        }
    }
}
