package com.gnilc.authz.rbac.service.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserRoleEvent extends ApplicationEvent {
    public UserRoleEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }

    private final Long userId;
}
