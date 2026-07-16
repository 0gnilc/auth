package com.gnilc.auth.authz.rbac.event;

import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 在菜单子树删除前清理角色菜单授权。
 */
@Component
public class RoleMenuRemovalListener {
    private final RoleMenuService roleMenuService;

    public RoleMenuRemovalListener(RoleMenuService roleMenuService) {
        this.roleMenuService = roleMenuService;
    }

    @EventListener
    public void removeRoleMenuBindings(MenuSubtreeRemovingEvent event) {
        roleMenuService.removeByMenuIds(event.menuIds());
    }
}
