package com.gnilc.authz.rbac.service.event;

import lombok.Getter;

@Getter
public class UserEvent extends CrudEvent {
    public UserEvent(Object source, Event event, Long userId) {
        super(source, event);
        this.userId = userId;
    }

    /**
     * 用户id
     */
    private final Long userId;

}
