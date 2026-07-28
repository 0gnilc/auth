package com.gnilc.auth.authz.rbac.event;

import java.util.Objects;

/**
 * 菜单数据变化事件。
 *
 * @param action 菜单变更动作
 * @param menuId 菜单 ID
 */
public record MenuEvent(Action action, Long menuId) {
    public MenuEvent {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(menuId, "menuId must not be null");
    }

    public enum Action {
        CREATE,
        UPDATE,
        DELETE
    }
}
