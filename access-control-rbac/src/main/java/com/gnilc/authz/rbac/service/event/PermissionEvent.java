package com.gnilc.authz.rbac.service.event;

import lombok.Getter;

@Getter
public class PermissionEvent extends CrudEvent {
    public PermissionEvent(Object source, Event event, Long permissionId) {
        super(source, event);
        this.permissionId = permissionId;
    }

    /**
     * 权限id
     */
    private final Long permissionId;

}
