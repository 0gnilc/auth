package com.gnilc.authz.rbac.service.event;

import org.springframework.context.ApplicationEvent;

public class ClearEvent extends ApplicationEvent {
    public ClearEvent(Object source) {
        super(source);
    }
}
