package com.gnilc.auth.authz.rbac.event;

import lombok.Data;

import java.util.Objects;

/** 菜单数据变化事件。 */
@Data
public final class MenuEvent {
    /** 菜单创建、更新或删除动作，构造时必须提供。 */
    private final Action action;
    /** 发生变化的菜单 ID，构造时必须提供。 */
    private final Long menuId;

    /**
     * 创建菜单数据变化事件。
     *
     * @param action 菜单变更动作
     * @param menuId 菜单 ID
     */
    public MenuEvent(Action action, Long menuId) {
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.menuId = Objects.requireNonNull(menuId, "menuId must not be null");
    }

    /** 菜单变更动作。 */
    public enum Action {
        /** 创建菜单。 */
        CREATE,
        /** 更新菜单。 */
        UPDATE,
        /** 删除菜单。 */
        DELETE
    }
}
