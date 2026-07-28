package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.gnilc.auth.authz.rbac.dao.RoleMenusDao;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleMenuBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleMenuDto;
import com.gnilc.auth.authz.rbac.event.AuthorizationEvent;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.i18n.I18nMessageService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleMenuServiceImplTest {
    @Mock
    private RoleMenusDao roleMenusDao;
    @Mock
    private MenuService menuService;
    @Mock
    private RoleService roleService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RoleMenuServiceImpl roleMenus;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(RoleMenuBo.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "role-menu-service-test"),
                    RoleMenuBo.class);
        }
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/rbac/messages");
        source.setDefaultEncoding("UTF-8");
        roleMenus = spy(new RoleMenuServiceImpl(
                menuService,
                roleService,
                eventPublisher,
                new I18nMessageService(source, "en-US")));
        lenient().doAnswer(invocation -> new LambdaQueryChainWrapper<>(
                roleMenusDao, Wrappers.lambdaQuery(RoleMenuBo.class)))
                .when(roleMenus).lambdaQuery();
    }

    @Test
    void saveRoleMenusSavesTheValidatedAncestorClosure() {
        RoleMenuDto dto = assignment(7L, List.of(30L));
        when(roleService.getById(7L)).thenReturn(role(7L, false));
        when(roleMenusDao.selectList(any())).thenReturn(List.of());
        when(menuService.getMenusWithAncestors(Set.of(30L), true))
                .thenReturn(List.of(menu(20L), menu(30L)));
        doReturn(true).when(roleMenus).saveBatch(anyCollection());

        roleMenus.saveRoleMenus(dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<RoleMenuBo>> saved = ArgumentCaptor.forClass(Collection.class);
        verify(roleMenus).saveBatch(saved.capture());
        assertThat(saved.getValue()).extracting(RoleMenuBo::getMenuId)
                .containsExactlyInAnyOrder(20L, 30L);
        assertThat(saved.getValue()).extracting(RoleMenuBo::getRoleId)
                .containsOnly(7L);
        verify(menuService).getMenusWithAncestors(Set.of(30L), true);
        verify(roleMenusDao).selectList(any());
        assertRoleMenuEvent(7L);
    }

    @Test
    void saveRoleMenusDoesNotMutateBindingsWhenMenuValidationFails() {
        RoleMenuDto dto = assignment(7L, List.of(Long.MAX_VALUE));
        when(roleService.getById(7L)).thenReturn(role(7L, false));
        when(roleMenusDao.selectList(any())).thenReturn(List.of());
        when(menuService.getMenusWithAncestors(Set.of(Long.MAX_VALUE), true))
                .thenThrow(new InvalidArgumentException("The selected menu is invalid."));

        assertThatThrownBy(() -> roleMenus.saveRoleMenus(dto))
                .isInstanceOf(InvalidArgumentException.class);
        verify(roleMenus, never()).lambdaUpdate();
        verify(roleMenus, never()).saveBatch(anyCollection());
        verify(roleMenusDao).selectList(any());
    }

    @Test
    void saveRoleMenusRejectsBuiltInRolesBeforeReadingBindings() {
        RoleMenuDto dto = assignment(7L, List.of(30L));
        when(roleService.getById(7L)).thenReturn(role(7L, true));

        assertThatThrownBy(() -> roleMenus.saveRoleMenus(dto))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("Built-in role permissions and menus cannot be modified.");
        verify(roleMenus, never()).lambdaQuery();
        verify(roleMenus, never()).saveBatch(anyCollection());
    }

    private RoleMenuDto assignment(Long roleId, List<Long> menuIds) {
        RoleMenuDto dto = new RoleMenuDto();
        dto.setRoleId(roleId);
        dto.setMenuIds(menuIds);
        return dto;
    }

    private void assertRoleMenuEvent(Long roleId) {
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(AuthorizationEvent.class, published -> {
            assertThat(published.getType()).isEqualTo(AuthorizationEvent.Type.ROLE_MENU);
            assertThat(published.getAction()).isEqualTo(AuthorizationEvent.Action.REPLACE);
            assertThat(published.getData()).isEqualTo(roleId);
        });
    }

    private MenuBo menu(Long id) {
        MenuBo menu = new MenuBo();
        menu.setId(id);
        return menu;
    }

    private RoleBo role(Long id, boolean builtIn) {
        RoleBo role = new RoleBo();
        role.setId(id);
        role.setBuiltIn(builtIn);
        return role;
    }
}
