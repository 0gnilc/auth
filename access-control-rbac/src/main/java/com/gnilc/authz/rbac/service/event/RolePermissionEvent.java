package com.gnilc.authz.rbac.service.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RolePermissionEvent extends ApplicationEvent {
    public RolePermissionEvent(Object source, Long roleId) {
        super(source);
        this.roleId = roleId;
    }

    private final Long roleId;
}
