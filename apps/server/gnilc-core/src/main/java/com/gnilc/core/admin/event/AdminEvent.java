package com.gnilc.core.admin.event;

import lombok.Data;

import java.util.Objects;

/** 管理员身份资料变化事件，以 RBAC 用户主键定位受影响缓存。 */
@Data
public final class AdminEvent {
    /** 管理员资料变更动作，不得为空。 */
    private final Action action;
    /** 发生变更的管理员所对应的 RBAC 用户主键，不得为空。 */
    private final Long userId;

    /**
     * 创建管理员数据变化事件。
     *
     * @param action 管理员变更动作
     * @param userId RBAC 用户 ID
     */
    public AdminEvent(Action action, Long userId) {
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
    }

    /** 管理员数据变化动作。 */
    public enum Action {
        /** 创建管理员身份。 */
        CREATE,
        /** 更新管理员资料或相关授权数据。 */
        UPDATE,
        /** 删除管理员身份。 */
        DELETE
    }
}
