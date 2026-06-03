package com.gnilc.authz.rbac.service.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CrudEvent extends ApplicationEvent {

    public CrudEvent(Object source, Event event) {
        super(source);
        this.event = event;
    }

    /**
     * {@link Event}
     * 事件
     */
    private final Event event;

    public enum Event {
        CREATE,
        READ,
        UPDATE,
        DELETE
    }
}
