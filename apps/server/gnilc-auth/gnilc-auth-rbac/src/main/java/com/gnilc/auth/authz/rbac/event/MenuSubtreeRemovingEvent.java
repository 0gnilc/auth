package com.gnilc.auth.authz.rbac.event;

import java.util.List;

/**
 * 菜单子树删除前事件，用于在不引入 Service 循环依赖的情况下清理关联数据。
 */
public record MenuSubtreeRemovingEvent(List<Long> menuIds) {
    public MenuSubtreeRemovingEvent {
        menuIds = List.copyOf(menuIds);
    }
}
