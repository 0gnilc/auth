package com.gnilc.authz.rbac.service.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RoleMenuEvent extends ApplicationEvent {
    public RoleMenuEvent(Object source, Long roleId) {
        super(source);
        this.roleId = roleId;
    }

    private final Long roleId;
}
