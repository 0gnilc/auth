package com.gnilc.core.session;

/**
 * 管理一种身份会话的令牌生命周期。
 */
final class SessionEngine {
    private final SessionPolicy policy;
    private final SessionRedisStore store;
    private final SessionTokenCodec codec;

    /** 创建指定身份域的 Session Engine。 */
    SessionEngine(SessionPolicy policy, SessionRedisStore store) {
        this.policy = policy;
        this.store = store;
        this.codec = new SessionTokenCodec(policy.getTokenPrefix());
    }

    /** 判断 Token 是否属于当前 Access Token 命名空间。 */
    boolean supportsAccessToken(String token) {
        return codec.matches(token);
    }

    /** 为 RBAC User 创建 Session。 */
    SessionTokenPair createSession(Long userId) {
        String accessToken = codec.issue(userId);
        String refreshToken = codec.issue(userId);
        store.saveSession(policy, userId, accessToken, refreshToken);
        return SessionTokenPair.of(accessToken, refreshToken);
    }

    /** 校验 Access Token 并返回 RBAC User ID。 */
    Long validateAccessToken(String accessToken) {
        Long userId = parseUserId(accessToken);
        if (userId == null) {
            return null;
        }
        return store.hasAccessToken(policy, userId, accessToken) ? userId : null;
    }

    /** 轮换访问令牌并保留原刷新令牌及其剩余寿命；旧配对已变化或刷新令牌失效时返回 null。 */
    SessionTokenPair refreshSession(String refreshToken) {
        Long userId = resolveRefreshUserId(refreshToken);
        if (userId == null) {
            return null;
        }
        String oldAccessToken = store.getPairedAccessToken(policy, userId, refreshToken);
        if (oldAccessToken == null || oldAccessToken.isBlank()) {
            return null;
        }
        String accessToken = codec.issue(userId);
        // 以读到的旧配对执行 CAS；同一旧配对只能成功一次，较晚读取新配对的请求仍可继续刷新。
        if (!store.rotateAccessToken(
                policy, userId, refreshToken, oldAccessToken, accessToken)) {
            return null;
        }
        return SessionTokenPair.of(accessToken, refreshToken);
    }

    /** 删除 Refresh Token 对应的 Session。 */
    boolean logout(String refreshToken) {
        Long userId = parseUserId(refreshToken);
        return userId != null && store.deleteSession(policy, userId, refreshToken);
    }

    /** 删除 RBAC User 的全部 Sessions。 */
    void cleanupUserSessions(Long userId) {
        store.deleteUserSessions(policy, userId);
    }

    /** 解析有效 Refresh Token 对应的 RBAC User ID。 */
    Long resolveRefreshUserId(String refreshToken) {
        Long userId = parseUserId(refreshToken);
        if (userId == null) {
            return null;
        }
        return store.hasRefreshToken(policy, userId, refreshToken) ? userId : null;
    }

    /** 仅解析令牌身份部分；格式错误表示凭据无效，Redis 故障仍向上传播而非伪装成匿名。 */
    private Long parseUserId(String token) {
        try {
            return codec.resolve(token);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
