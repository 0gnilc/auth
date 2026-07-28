package com.gnilc.auth.authz.rbac.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.rbac.dao.MenuDao;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.dto.MenuDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.i18n.I18nMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
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
        LocaleContextHolder.setLocale(Locale.US);
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/rbac/messages");
        source.setDefaultEncoding("UTF-8");
        menus = spy(new MenuServiceImpl(
                menuDao,
                roleMenuService,
                new ObjectMapper(),
                new I18nMessageService(source, "en-US")));
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
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
        assertThat(routes.get(0).getComponent()).isNull();
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
    void getMenuRoutesPrunesCatalogsWithoutNavigableDescendants() {
        MenuBo emptyCatalog = menu(1L, 0L, MenuType.CATALOG, "Empty", "/empty", 1);
        doReturn(List.of(emptyCatalog)).when(menus).list();

        assertThat(menus.getMenuRoutes(List.of(1L))).isEmpty();
    }

    @Test
    void updateMenuRejectsChangingTheCreationTimeType() {
        MenuBo existing = menu(1L, 0L, MenuType.CATALOG, "Tools", "/tools", 1);
        doReturn(existing).when(menus).getById(1L);
        MenuDto update = completeMenu(MenuType.MENU, 0L);
        update.setId(1L);
        update.setComponent("/tools/index");

        assertThatThrownBy(() -> menus.updateMenu(update))
                .isInstanceOf(IllegalConditionException.class);
    }

    @Test
    void updateMenuRejectsAnIncompleteFullUpdate() {
        MenuBo existing = menu(1L, 0L, MenuType.MENU, "Reports", "/reports", 1);
        existing.setComponent("/reports/index");
        doReturn(existing).when(menus).getById(1L);
        MenuDto update = completeMenu(MenuType.MENU, 0L);
        update.setId(1L);

        assertThatThrownBy(() -> menus.updateMenu(update))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Page component is required.");
        verify(menus, never()).updateById(any(MenuBo.class));
    }

    @Test
    void createMenuRejectsIllegalRootAndParentCombinations() {
        MenuDto rootButton = completeMenu(MenuType.BUTTON, 0L);
        rootButton.setAccessCode("shared:read");

        assertThatThrownBy(() -> menus.createMenu(rootButton))
                .isInstanceOf(InvalidArgumentException.class);

        MenuBo page = menu(1L, 0L, MenuType.MENU, "Page", "/page", 1);
        page.setComponent("/page/index");
        doReturn(page).when(menus).getById(1L);
        MenuDto nestedCatalog = completeMenu(MenuType.CATALOG, 1L);

        assertThatThrownBy(() -> menus.createMenu(nestedCatalog))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void createMenuRejectsNonHttpEmbeddedAndExternalUrls() {
        MenuDto embedded = completeMenu(MenuType.EMBEDDED, 0L);
        embedded.setIframeSrc("/relative/docs");

        assertThatThrownBy(() -> menus.createMenu(embedded))
                .isInstanceOf(InvalidArgumentException.class);

        MenuDto link = completeMenu(MenuType.LINK, 0L);
        link.setLink("javascript:alert(1)");

        assertThatThrownBy(() -> menus.createMenu(link))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void getMenusWithAncestorsRejectsAnInvalidSelectionBeforeReturningAClosure() {
        doReturn(List.of(menu(1L, 0L, MenuType.CATALOG, "Root", "/root", 1)))
                .when(menus).list();

        assertThatThrownBy(() -> menus.getMenusWithAncestors(Set.of(Long.MAX_VALUE), true))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("A selected menu no longer exists. Refresh and try again.");
        verify(menus).list();
    }

    @Test
    void getMenusWithAncestorsUsesTheRequestLocaleForValidationErrors() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        doReturn(List.of(menu(1L, 0L, MenuType.CATALOG, "Root", "/root", 1)))
                .when(menus).list();

        assertThatThrownBy(() -> menus.getMenusWithAncestors(Set.of(Long.MAX_VALUE), true))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("所选菜单已不存在，请刷新后重试。");
    }

    @Test
    void removeMenuUsesTheCompleteSubtreeForBindingCleanupAndLogicalDeletion() {
        MenuBo root = menu(1L, 0L, MenuType.CATALOG, "Root", "/root", 1);
        List<Long> subtreeIds = List.of(1L, 2L, 3L);
        doReturn(root).when(menus).getById(1L);
        when(menuDao.getSubtreeIds(1L, true)).thenReturn(subtreeIds);
        doReturn(List.of(root)).when(menus).getMenus(subtreeIds);
        doReturn(true).when(menus).removeByIds(subtreeIds);

        menus.removeMenu(1L);

        verify(menuDao).getSubtreeIds(1L, true);
        verify(roleMenuService).removeByMenuIds(subtreeIds);
        verify(menus).removeByIds(subtreeIds);
    }

    @Test
    void removeMenuRejectsAMutableRootContainingABuiltInDescendant() {
        MenuBo root = menu(1L, 0L, MenuType.CATALOG, "Root", "/root", 1);
        MenuBo builtInChild = menu(2L, 1L, MenuType.MENU, "Protected", "/protected", 1);
        builtInChild.setBuiltIn(true);
        List<Long> subtreeIds = List.of(1L, 2L);
        doReturn(root).when(menus).getById(1L);
        when(menuDao.getSubtreeIds(1L, true)).thenReturn(subtreeIds);
        doReturn(List.of(root, builtInChild)).when(menus).getMenus(subtreeIds);

        assertThatThrownBy(() -> menus.removeMenu(1L))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("Built-in menus cannot be deleted.");
        verify(roleMenuService, never()).removeByMenuIds(anyList());
        verify(menus, never()).removeByIds(anyList());
    }

    @Test
    void updateAndRemoveRejectBuiltInMenus() {
        MenuBo builtIn = menu(1L, 0L, MenuType.CATALOG, "System", "/system", 1);
        builtIn.setBuiltIn(true);
        doReturn(builtIn).when(menus).getById(1L);
        MenuDto update = new MenuDto();
        update.setId(1L);

        assertThatThrownBy(() -> menus.updateMenu(update))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("Built-in menus cannot be modified.");
        assertThatThrownBy(() -> menus.removeMenu(1L))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("Built-in menus cannot be deleted.");
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

    private MenuDto completeMenu(MenuType type, Long pid) {
        MenuDto menu = new MenuDto();
        menu.setPid(pid);
        menu.setType(type);
        menu.setStatus(true);
        menu.setName("Candidate" + type);
        menu.setPath(type == MenuType.BUTTON ? null : "/candidate");
        menu.setTitle("menu.candidate.title");
        menu.setOrder(1);
        return menu;
    }
}
