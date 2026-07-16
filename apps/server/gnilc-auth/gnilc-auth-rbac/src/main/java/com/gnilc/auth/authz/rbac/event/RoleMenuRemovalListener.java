package com.gnilc.auth.authz.rbac.event;

import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 在菜单子树删除前同步清理角色菜单授权，与菜单删除共用同一事务。
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
