package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.gnilc.authz.rbac.common.constant.MenuConstant;
import com.gnilc.authz.rbac.entity.bo.MenuBo;
import com.gnilc.authz.rbac.entity.dto.MenuDto;
import com.gnilc.authz.rbac.entity.enums.MenuType;
import com.gnilc.authz.rbac.entity.vo.MenuVo;
import com.gnilc.authz.rbac.exception.InvalidArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuServiceImplTest {
    private MenuServiceImpl service;
    private LambdaQueryChainWrapper<MenuBo> query;

    /**
     * Sets up a fresh menu service spy before each test.
     */
    @BeforeEach
    void setUp() {
        service = spy(new MenuServiceImpl());
        query = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        doReturn(query).when(query).eq(ArgumentMatchers.<SFunction<MenuBo, ?>>any(), any());
        doReturn(query).when(query).eq(anyBoolean(), ArgumentMatchers.<SFunction<MenuBo, ?>>any(), any());
        doReturn(query).when(service).lambdaQuery();
    }

    /**
     * Verifies menu creation keeps path separate from access code.
     */
    @Test
    void createMenuDoesNotMirrorPathToAccessCode() {
        MenuDto md = validMenu(MenuType.MENU);
        when(query.one()).thenReturn(null);
        doReturn(true).when(service).save(any(MenuBo.class));

        service.createMenu(md);

        ArgumentCaptor<MenuBo> mbCaptor = ArgumentCaptor.forClass(MenuBo.class);
        verify(service).save(mbCaptor.capture());
        MenuBo mb = mbCaptor.getValue();
        assertThat(mb.getAccessCode()).isNull();
        assertThat(mb.getPath()).isEqualTo("/system/user");
    }

    /**
     * Verifies menu updates persist validated fields by ID.
     */
    @Test
    void updateMenuUpdatesValidatedMenuById() {
        MenuBo mb = menu(10L, MenuConstant.ROOT_PARENT_ID, 1);
        doReturn(mb).when(service).getById(10L);
        when(query.one()).thenReturn(null);
        doReturn(true).when(service).updateById(any(MenuBo.class));
        MenuDto md = validMenu(MenuType.MENU);
        md.setId(10L);
        md.setTitle("用户列表");

        service.updateMenu(md);

        ArgumentCaptor<MenuBo> mbCaptor = ArgumentCaptor.forClass(MenuBo.class);
        verify(service).updateById(mbCaptor.capture());
        assertThat(mbCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(mbCaptor.getValue().getTitle()).isEqualTo("用户列表");
    }

    /**
     * Verifies menu validation enforces type-specific required fields.
     */
    @Test
    void validateRequiredFieldsByMenuType() {
        assertThatThrownBy(() -> service.createMenu(validMenu(MenuType.CATALOG).setPathForTest(null)))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请输入路由路径");
        assertThatThrownBy(() -> service.createMenu(validMenu(MenuType.MENU).setComponentForTest(null)))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请输入页面组件");
        assertThatThrownBy(() -> service.createMenu(validMenu(MenuType.BUTTON).setAccessCodeForTest(null)))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请输入权限标识");
        assertThatThrownBy(() -> service.createMenu(validMenu(MenuType.EMBEDDED).setIframeSrcForTest(null)))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请输入内嵌页面地址");
        assertThatThrownBy(() -> service.createMenu(validMenu(MenuType.LINK).setLinkForTest(null)))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请输入外链地址");
    }

    /**
     * Verifies menu trees are built by parent ID and order.
     */
    @Test
    void buildMenuTreeByPidAndOrder() {
        MenuBo root = menu(1L, MenuConstant.ROOT_PARENT_ID, 20);
        MenuBo first = menu(2L, 1L, 1);
        MenuBo second = menu(3L, 1L, 2);
        doReturn(List.of(root, second, first)).when(service).list();

        List<MenuVo> mvs = service.getMenuTree();

        assertThat(mvs).hasSize(1);
        assertThat(mvs.get(0).getId()).isEqualTo(1L);
        assertThat(mvs.get(0).getChildren()).extracting(MenuVo::getId).containsExactly(2L, 3L);
    }

    private TestMenuDto validMenu(MenuType type) {
        TestMenuDto md = new TestMenuDto();
        md.setPid(MenuConstant.ROOT_PARENT_ID);
        md.setType(type);
        md.setName("user");
        md.setTitle("用户管理");
        md.setPath("/system/user");
        md.setComponent("/system/user/index");
        md.setIframeSrc("https://example.test/embed");
        md.setLink("https://example.test");
        md.setOrder(1);
        if (type == MenuType.BUTTON) {
            md.setAccessCode("user:create");
        }
        return md;
    }

    private MenuBo menu(Long id, Long pid, Integer order) {
        MenuBo mb = new MenuBo();
        mb.setId(id);
        mb.setPid(pid);
        mb.setOrder(order);
        mb.setType(MenuType.MENU);
        mb.setName("menu-" + id);
        mb.setTitle("菜单" + id);
        mb.setPath("/menu/" + id);
        return mb;
    }

    private static class TestMenuDto extends MenuDto {
        TestMenuDto setPathForTest(String path) {
            setPath(path);
            return this;
        }

        TestMenuDto setComponentForTest(String component) {
            setComponent(component);
            return this;
        }

        TestMenuDto setAccessCodeForTest(String accessCode) {
            setAccessCode(accessCode);
            return this;
        }

        TestMenuDto setIframeSrcForTest(String iframeSrc) {
            setIframeSrc(iframeSrc);
            return this;
        }

        TestMenuDto setLinkForTest(String link) {
            setLink(link);
            return this;
        }
    }
}
