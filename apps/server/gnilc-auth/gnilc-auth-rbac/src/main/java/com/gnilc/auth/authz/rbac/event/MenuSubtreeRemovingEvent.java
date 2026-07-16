package com.gnilc.auth.authz.rbac.event;

import java.util.List;

/**
 * 菜单子树删除前事件。
 */
public record MenuSubtreeRemovingEvent(List<Long> menuIds) {
    public MenuSubtreeRemovingEvent {
        menuIds = List.copyOf(menuIds);
    }
}
