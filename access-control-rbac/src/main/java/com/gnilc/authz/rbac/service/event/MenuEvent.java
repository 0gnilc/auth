package com.gnilc.authz.rbac.service.event;

import lombok.Getter;

@Getter
public class MenuEvent extends CrudEvent {
    public MenuEvent(Object source, Event event, Long menuId) {
        super(source, event);
        this.menuId = menuId;
    }

    /**
     * 菜单id
     */
    private final Long menuId;

}
