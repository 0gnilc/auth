package com.gnilc.authz.rbac.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

import java.util.Objects;

/**
 * RBAC 授权事件。
 * <p>
 * 表示会影响授权结果的 RBAC 数据发生了变化。
 */
@Getter
public class RbacAuthzEvent<T> implements ResolvableTypeProvider {
    /**
     * 授权数据类型。
     */
    private final Type type;
    /**
     * 授权数据变更动作。
     */
    private final Action action;
    /**
     * 授权事件数据，具体类型和含义由 {@link #type} 决定。
     * -- SETTER --
     * 设置授权事件数据，并根据传入数据推导事件泛型。
     *
     * @param data 授权事件数据
     */
    @Setter
    private T data;
    /**
     * 扩展上下文，用于保存未来可能需要的事件数据。
     */
    @Setter
    private Object extra;

    /**
     * 创建携带数据的 RBAC 授权事件。
     *
     * @param type   授权数据类型
     * @param action 授权数据变更动作
     * @param data   授权事件数据
     */
    private RbacAuthzEvent(Type type, Action action, T data) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.data = data;
    }

    /**
     * 创建无数据的 RBAC 授权事件。
     *
     * @param type   授权数据类型
     * @param action 授权数据变更动作
     * @return RBAC 授权事件
     */
    public static RbacAuthzEvent<Void> of(Type type, Action action) {
        return of(type, action, null);
    }

    /**
     * 创建无数据的 RBAC 授权事件。
     *
     * @param type 授权数据类型
     * @return RBAC 授权事件
     */
    public static RbacAuthzEvent<Void> of(Type type) {
        return of(type, Action.DEFAULT, null);
    }

    /**
     * 创建携带数据的 RBAC 授权事件。
     *
     * @param type   授权数据类型
     * @param action 授权数据变更动作
     * @param data   授权事件数据
     * @return RBAC 授权事件
     */
    public static <T> RbacAuthzEvent<T> of(Type type, Action action, T data) {
        return new RbacAuthzEvent<>(type, action, data);
    }

    @Override
    public ResolvableType getResolvableType() {
        ResolvableType generic = data == null ? ResolvableType.forClass(Void.class) : ResolvableType.forInstance(data);
        return ResolvableType.forClassWithGenerics(RbacAuthzEvent.class, generic);
    }

    /**
     * RBAC 授权数据类型。
     */
    public enum Type {
        /**
         * 权限数据，data 通常表示 permissionId。
         */
        PERMISSION,
        /**
         * 角色数据，data 通常表示 roleId。
         */
        ROLE,
        /**
         * 用户数据，data 通常表示 userId。
         */
        USER,
        /**
         * 角色权限绑定关系，data 通常表示 roleId。
         */
        ROLE_PERMISSION,
        /**
         * 用户角色绑定关系，data 通常表示 userId。
         */
        USER_ROLE,
        /**
         * 全部授权数据，data 通常为空。
         */
        ALL
    }

    /**
     * RBAC 授权数据变更动作。
     */
    public enum Action {
        DEFAULT,
        CREATE,
        UPDATE,
        DELETE,
        REPLACE,
        CLEAR
    }
}
