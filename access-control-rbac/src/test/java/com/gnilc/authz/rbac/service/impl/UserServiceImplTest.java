package com.gnilc.authz.rbac.service.impl;

import com.gnilc.authz.rbac.entity.bo.MenuBo;
import com.gnilc.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.authz.rbac.entity.bo.RoleBo;
import com.gnilc.authz.rbac.entity.bo.UserBo;
import com.gnilc.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.authz.rbac.service.MenuService;
import com.gnilc.authz.rbac.service.PermissionService;
import com.gnilc.authz.rbac.service.RoleService;
import com.gnilc.authz.rbac.service.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceImplTest {
    private ApplicationEventPublisher publisher;
    private RoleService roleService;
    private UserRoleService userRoleService;
    private PermissionService permissionService;
    private MenuService menuService;
    private UserServiceImpl service;

    /**
     * Sets up a fresh user service spy before each test.
     */
    @BeforeEach
    void setUp() {
        publisher = mock(ApplicationEventPublisher.class);
        roleService = mock(RoleService.class);
        userRoleService = mock(UserRoleService.class);
        permissionService = mock(PermissionService.class);
        menuService = mock(MenuService.class);
        service = spy(new UserServiceImpl());
        ReflectionTestUtils.setField(service, "publisher", publisher);
        ReflectionTestUtils.setField(service, "roleService", roleService);
        ReflectionTestUtils.setField(service, "userRoleService", userRoleService);
        ReflectionTestUtils.setField(service, "permissionService", permissionService);
        ReflectionTestUtils.setField(service, "menuService", menuService);
    }

    /**
     * Verifies user creation saves and returns the generated ID.
     */
    @Test
    void createUserSavesUserAndReturnsGeneratedId() {
        doAnswer(invocation -> {
            UserBo user = invocation.getArgument(0);
            user.setId(100L);
            return true;
        }).when(service).save(any(UserBo.class));

        Long userId = service.createUser();

        assertThat(userId).isEqualTo(100L);
        verify(service).save(any(UserBo.class));
    }

    /**
     * Verifies user removal publishes a delete event.
     */
    @Test
    void removeUserPublishesUserDeleteEvent() {
        doReturn(true).when(service).removeById(100L);

        boolean removed = service.removeUser(100L);

        assertThat(removed).isTrue();
        verify(service).removeById(100L);
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.USER);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.DELETE);
        assertThat(event.getData()).isEqualTo(100L);
    }

    /**
     * Verifies bindRole returns false for missing input or role.
     */
    @Test
    void bindRoleReturnsFalseForMissingInputOrRole() {
        assertThat(service.bindRole(null, "admin")).isFalse();
        assertThat(service.bindRole(100L, " ")).isFalse();
        when(roleService.getRoleByCode("missing")).thenReturn(null);

        assertThat(service.bindRole(100L, "missing")).isFalse();

        verify(userRoleService, never()).bindRole(any(), any());
    }

    /**
     * Verifies bindRole uses the role code and user ID.
     */
    @Test
    void bindRoleUsesRoleCodeAndUserId() {
        RoleBo role = new RoleBo();
        role.setId(1L);
        when(roleService.getRoleByCode("admin")).thenReturn(role);

        assertThat(service.bindRole(100L, "admin")).isTrue();

        verify(userRoleService).bindRole(100L, 1L);
    }

    /**
     * Verifies unbindRole returns false for missing input or role.
     */
    @Test
    void unbindRoleReturnsFalseForMissingInputOrRole() {
        assertThat(service.unbindRole(null, "admin")).isFalse();
        assertThat(service.unbindRole(100L, " ")).isFalse();
        when(roleService.getRoleByCode("missing")).thenReturn(null);

        assertThat(service.unbindRole(100L, "missing")).isFalse();

        verify(userRoleService, never()).unbindRole(any(), any());
    }

    /**
     * Verifies unbindRole uses the role code and user ID.
     */
    @Test
    void unbindRoleUsesRoleCodeAndUserId() {
        RoleBo role = new RoleBo();
        role.setId(1L);
        when(roleService.getRoleByCode("admin")).thenReturn(role);

        assertThat(service.unbindRole(100L, "admin")).isTrue();

        verify(userRoleService).unbindRole(100L, 1L);
    }

    /**
     * Verifies checkRole returns false when the role does not exist.
     */
    @Test
    void checkRoleReturnsFalseWhenRoleDoesNotExist() {
        when(roleService.getRoleByCode("missing")).thenReturn(null);

        assertThat(service.checkRole(100L, "missing")).isFalse();

        verifyNoInteractions(userRoleService);
    }

    /**
     * Verifies checkRole uses the user-role relation.
     */
    @Test
    void checkRoleUsesUserRoleRelation() {
        RoleBo role = new RoleBo();
        role.setId(1L);
        when(roleService.getRoleByCode("admin")).thenReturn(role);
        when(userRoleService.getUserRole(100L, 1L)).thenReturn(new com.gnilc.authz.rbac.entity.bo.UserRoleBo());

        assertThat(service.checkRole(100L, "admin")).isTrue();
    }

    /**
     * Verifies aggregate getters return empty results for null user IDs.
     */
    @Test
    void aggregateGettersReturnEmptyForNullUserId() {
        assertThat(service.getRoles(null)).isEmpty();
        assertThat(service.getPermissions(null)).isEmpty();
        assertThat(service.getMenus(null)).isEmpty();
        assertThat(service.geUser(null)).isNull();
    }

    /**
     * Verifies aggregate getters delegate when user ID is present.
     */
    @Test
    void aggregateGettersDelegateForUserId() {
        RoleBo role = new RoleBo();
        PermissionBo permission = new PermissionBo();
        MenuBo menu = new MenuBo();
        when(roleService.getRoles(100L)).thenReturn(List.of(role));
        when(permissionService.getPermissions(100L)).thenReturn(List.of(permission));
        when(menuService.getMenus(100L)).thenReturn(List.of(menu));
        UserBo user = new UserBo();
        doReturn(user).when(service).getById(100L);

        assertThat(service.getRoles(100L)).containsExactly(role);
        assertThat(service.getPermissions(100L)).containsExactly(permission);
        assertThat(service.getMenus(100L)).containsExactly(menu);
        assertThat(service.geUser(100L)).isSameAs(user);
    }

    private RbacAuthzEvent<?> publishedEvent() {
        ArgumentCaptor<RbacAuthzEvent> eventCaptor = ArgumentCaptor.forClass(RbacAuthzEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        return eventCaptor.getValue();
    }
}
