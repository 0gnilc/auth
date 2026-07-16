package com.gnilc.auth.authz.rbac.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.rbac.dao.MenuDao;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {
    @Mock
    private MenuDao menuDao;
    @Mock
    private RoleMenuService roleMenuService;

    private MenuServiceImpl menus;

    @BeforeEach
    void setUp() {
        menus = spy(new MenuServiceImpl(menuDao, roleMenuService, new ObjectMapper()));
    }

    @Test
    void getMenuRoutesBuildsAuthorizedHierarchyAndMapsRouteComponents() {
        MenuBo root = menu(1L, 0L, MenuType.CATALOG, "Tools", "/tools", 1);
        MenuBo page = menu(2L, 1L, MenuType.MENU, "Audit", "audit", 1);
        page.setComponent("/tools/audit/index");
        page.setQuery("{\"tab\":\"recent\"}");
        MenuBo button = menu(3L, 2L, MenuType.BUTTON, "AuditExport", null, 1);
        button.setAccessCode("audit:export");
        MenuBo embedded = menu(4L, 0L, MenuType.EMBEDDED, "Docs", "/docs", 2);
        embedded.setIframeSrc("https://example.test/docs");
        MenuBo link = menu(5L, 0L, MenuType.LINK, "Repository", "/repository", 3);
        link.setLink("https://example.test/repository");
        MenuBo disabled = menu(6L, 0L, MenuType.MENU, "Disabled", "/disabled", 4);
        disabled.setComponent("/disabled/index");
        disabled.setStatus(false);
        doReturn(List.of(root, page, button, embedded, link, disabled)).when(menus).list();

        List<MenuRouteVo> routes = menus.getMenuRoutes(List.of(3L, 4L, 5L, 6L, Long.MAX_VALUE));

        assertThat(routes).extracting(MenuRouteVo::getName)
                .containsExactly("Tools", "Docs", "Repository");
        assertThat(routes.get(0).getChildren()).singleElement().satisfies(route -> {
            assertThat(route.getName()).isEqualTo("Audit");
            assertThat(route.getComponent()).isEqualTo("/tools/audit/index");
            assertThat(route.getMeta().getQuery()).containsEntry("tab", "recent");
        });
        assertThat(routes.get(1).getComponent()).isEqualTo("IFrameView");
        assertThat(routes.get(1).getMeta().getIframeSrc()).isEqualTo("https://example.test/docs");
        assertThat(routes.get(2).getComponent()).isEqualTo("IFrameView");
        assertThat(routes.get(2).getMeta().getLink()).isEqualTo("https://example.test/repository");
        verify(menus).list();
    }

    @Test
    void getMenusWithAncestorsRejectsAnInvalidSelectionBeforeReturningAClosure() {
        doReturn(List.of(menu(1L, 0L, MenuType.CATALOG, "Root", "/root", 1)))
                .when(menus).list();

        assertThatThrownBy(() -> menus.getMenusWithAncestors(List.of(Long.MAX_VALUE)))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("A selected menu no longer exists. Refresh and try again.");
        verify(menus).list();
    }

    @Test
    void removeMenuUsesTheCompleteSubtreeForBindingCleanupAndLogicalDeletion() {
        MenuBo root = menu(1L, 0L, MenuType.CATALOG, "Root", "/root", 1);
        List<Long> subtreeIds = List.of(1L, 2L, 3L);
        doReturn(root).when(menus).getById(1L);
        when(menuDao.selectSubtreeIdsWithDeleted(1L)).thenReturn(subtreeIds);
        doReturn(true).when(menus).removeByIds(subtreeIds);

        menus.removeMenu(1L);

        verify(menuDao).selectSubtreeIdsWithDeleted(1L);
        verify(roleMenuService).removeByMenuIds(subtreeIds);
        verify(menus).removeByIds(subtreeIds);
    }

    private MenuBo menu(Long id, Long pid, MenuType type, String name, String path, int order) {
        MenuBo menu = new MenuBo();
        menu.setId(id);
        menu.setPid(pid);
        menu.setType(type);
        menu.setStatus(true);
        menu.setName(name);
        menu.setPath(path);
        menu.setOrder(order);
        menu.setTitle(name);
        return menu;
    }
}
