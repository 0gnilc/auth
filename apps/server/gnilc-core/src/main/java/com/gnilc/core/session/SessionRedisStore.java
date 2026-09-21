package com.gnilc.core.session;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

/**
 * 封装会话令牌的 Redis 读写。
 */
final class SessionRedisStore {
    // 比较、配对改写与访问键替换必须在一次 Redis 脚本内完成，刷新键剩余寿命不能随刷新延长。
    private static final DefaultRedisScript<Long> ROTATE_ACCESS_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            local paired_access_token = redis.call('GET', KEYS[1])
            if paired_access_token ~= ARGV[1] then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[2], 'KEEPTTL')
            redis.call('DEL', KEYS[2])
            redis.call('SET', KEYS[3], ARGV[3], 'EX', ARGV[4])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> DELETE_SESSION_SCRIPT = new DefaultRedisScript<>("""
            local paired_access_token = redis.call('GET', KEYS[1])
            if not paired_access_token then
                return 0
            end
            redis.call('DEL', ARGV[1] .. paired_access_token)
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    SessionRedisStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 按身份域分别保存访问与刷新映射并设置各自 TTL；新登录不覆盖其他登录会话。 */
    void saveSession(SessionPolicy policy, Long userId, String accessToken, String refreshToken) {
        redis.opsForValue().set(
                policy.accessKey(userId, accessToken), refreshToken, policy.getAccessTtl());
        redis.opsForValue().set(
                policy.refreshKey(userId, refreshToken), accessToken, policy.getRefreshTtl());
    }

    boolean hasAccessToken(SessionPolicy policy, Long userId, String accessToken) {
        return redis.hasKey(policy.accessKey(userId, accessToken));
    }

    boolean hasRefreshToken(SessionPolicy policy, Long userId, String refreshToken) {
        return redis.hasKey(policy.refreshKey(userId, refreshToken));
    }

    String getPairedAccessToken(SessionPolicy policy, Long userId, String refreshToken) {
        return redis.opsForValue().get(policy.refreshKey(userId, refreshToken));
    }

    /** 原子比较旧配对后替换访问令牌；刷新键使用 KEEPTTL，新的访问键重新获得访问 TTL。 */
    boolean rotateAccessToken(SessionPolicy policy, Long userId, String refreshToken,
                              String oldAccessToken, String newAccessToken) {
        Long result = redis.execute(
                ROTATE_ACCESS_TOKEN_SCRIPT,
                List.of(
                        policy.refreshKey(userId, refreshToken),
                        policy.accessKey(userId, oldAccessToken),
                        policy.accessKey(userId, newAccessToken)),
                oldAccessToken,
                newAccessToken,
                refreshToken,
                Long.toString(policy.getAccessTtl().toSeconds()));
        return Long.valueOf(1L).equals(result);
    }

    /** 原子删除刷新键及其当前配对的访问键，避免与并发刷新竞争后遗留有效访问令牌。 */
    boolean deleteSession(SessionPolicy policy, Long userId, String refreshToken) {
        Long result = redis.execute(
                DELETE_SESSION_SCRIPT,
                List.of(policy.refreshKey(userId, refreshToken)),
                policy.accessKeyPrefix(userId));
        return Long.valueOf(1L).equals(result);
    }

    /** 清除指定 RBAC 用户在本身份域的全部令牌；不会影响另一身份域或其他用户。 */
    void deleteUserSessions(SessionPolicy policy, Long userId) {
        deleteKeys(redis.keys(policy.accessPattern(userId)));
        deleteKeys(redis.keys(policy.refreshPattern(userId)));
    }

    private void deleteKeys(Set<String> keys) {
        if (!CollectionUtils.isEmpty(keys)) {
            redis.delete(keys);
        }
    }
}
