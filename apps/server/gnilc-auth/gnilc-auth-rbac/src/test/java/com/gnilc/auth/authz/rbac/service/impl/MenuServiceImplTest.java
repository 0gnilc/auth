package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.rbac.dao.MenuDao;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.dto.MenuDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.auth.authz.rbac.event.MenuEvent;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.i18n.I18nMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {
    @Mock
    private MenuDao menuDao;
    @Mock
    private RoleMenuService roleMenuService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MenuServiceImpl menus;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(MenuBo.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "menu-service-test"),
                    MenuBo.class);
        }
        LocaleContextHolder.setLocale(Locale.US);
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/rbac/messages");
        source.setDefaultEncoding("UTF-8");
        menus = spy(new MenuServiceImpl(
                menuDao,
                roleMenuService,
                eventPublisher,
                new ObjectMapper(),
                new I18nMessageService(source, "en-US")));
        lenient().doAnswer(invocation -> new LambdaQueryChainWrapper<>(
                menuDao, Wrappers.lambdaQuery(MenuBo.class)))
                .when(menus).lambdaQuery();
        lenient().when(menuDao.selectList(any())).thenReturn(List.of());
        lenient().doAnswer(invocation -> {
            ((MenuBo) invocation.getArgument(0)).setId(99L);
            return true;
        }).when(menus).save(any(MenuBo.class));
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
        verify(menus, never()).updateById(any(MenuBo.class));
        verifyNoInteractions(eventPublisher);
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
        verifyNoInteractions(eventPublisher);
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

    @ParameterizedTest(name = "accepts exact {0} business limit")
    @MethodSource("menuLengthBoundaries")
    void createMenuAcceptsExactBusinessLimits(
            String field,
            int maximum,
            String character,
            MenuType type,
            String ignoredMessage) {
        MenuDto dto = validMenu(type);
        prepareParent(dto);
        setField(dto, field, boundaryValue(field, maximum, character));

        menus.createMenu(dto);

        verify(menus).save(any(MenuBo.class));
        verify(eventPublisher).publishEvent(new MenuEvent(MenuEvent.Action.CREATE, 99L));
    }

    @ParameterizedTest(name = "rejects {0} beyond business limit")
    @MethodSource("menuLengthBoundaries")
    void createMenuRejectsFieldsBeyondBusinessLimits(
            String field,
            int maximum,
            String character,
            MenuType type,
            String message) {
        MenuDto dto = validMenu(type);
        prepareParent(dto);
        setField(dto, field, boundaryValue(field, maximum + 1, character));

        assertThatThrownBy(() -> menus.createMenu(dto))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage(message);
        verifyNoMenuWrite();
    }

    @ParameterizedTest(name = "{0} rejects a missing {1}")
    @MethodSource("missingTypeSpecificFields")
    void createMenuRejectsMissingTypeSpecificFields(
            MenuType type,
            String field,
            String message) {
        MenuDto dto = validMenu(type);
        prepareParent(dto);
        setField(dto, field, "   ");

        assertThatThrownBy(() -> menus.createMenu(dto))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage(message);
        verifyNoMenuWrite();
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
        doReturn(true).when(menus).updateById(any(MenuBo.class));
        doReturn(true).when(menus).removeByIds(subtreeIds);

        menus.removeMenu(1L);

        verify(menuDao).getSubtreeIds(1L, true);
        verify(roleMenuService).removeByMenuIds(subtreeIds);
        verify(menus).removeByIds(subtreeIds);
        verify(eventPublisher).publishEvent(new MenuEvent(MenuEvent.Action.DELETE, 1L));
        assertThat(root.getName()).isEqualTo("Root_del_1");
        assertThat(root.getPath()).isEqualTo("/root_del_1");
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
                .hasMessage("This menu contains built-in menus and cannot be deleted.");
        verify(menus, never()).updateById(any(MenuBo.class));
        verify(roleMenuService, never()).removeByMenuIds(anyList());
        verify(menus, never()).removeByIds(anyList());
        verifyNoInteractions(eventPublisher);
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

    private MenuDto validMenu(MenuType type) {
        Long pid = type == MenuType.BUTTON ? 10L : 0L;
        MenuDto menu = completeMenu(type, pid);
        switch (type) {
            case CATALOG -> {
            }
            case MENU -> menu.setComponent("/candidate/index");
            case BUTTON -> menu.setAccessCode("candidate:read");
            case EMBEDDED -> menu.setIframeSrc("https://example.test/embedded");
            case LINK -> menu.setLink("https://example.test/external");
        }
        return menu;
    }

    private void prepareParent(MenuDto dto) {
        if (dto.getType() != MenuType.BUTTON) {
            return;
        }
        doReturn(menu(10L, 0L, MenuType.CATALOG, "Parent", "/parent", 1))
                .when(menus).getById(10L);
    }

    private void verifyNoMenuWrite() {
        verify(menus, never()).save(any(MenuBo.class));
        verify(menus, never()).updateById(any(MenuBo.class));
        verifyNoInteractions(eventPublisher);
    }

    private static void setField(MenuDto dto, String field, String value) {
        switch (field) {
            case "name" -> dto.setName(value);
            case "title" -> dto.setTitle(value);
            case "accessCode" -> dto.setAccessCode(value);
            case "path" -> dto.setPath(value);
            case "component" -> dto.setComponent(value);
            case "redirect" -> dto.setRedirect(value);
            case "activePath" -> dto.setActivePath(value);
            case "badge" -> dto.setBadge(value);
            case "badgeType" -> dto.setBadgeType(value);
            case "badgeVariants" -> dto.setBadgeVariants(value);
            case "icon" -> dto.setIcon(value);
            case "iframeSrc" -> dto.setIframeSrc(value);
            case "link" -> dto.setLink(value);
            default -> throw new IllegalArgumentException("Unknown menu field: " + field);
        }
    }

    private static String boundaryValue(String field, int codePoints, String character) {
        String prefix = switch (field) {
            case "path", "redirect", "activePath" -> "/";
            case "iframeSrc", "link" -> "https://example.test/";
            default -> "";
        };
        return prefix + character.repeat(codePoints - prefix.codePointCount(0, prefix.length()));
    }

    private static Stream<Arguments> menuLengthBoundaries() {
        return Stream.of(
                Arguments.of("name", 255, "\uD83D\uDE00", MenuType.CATALOG,
                        "Menu name must not exceed 255 characters."),
                Arguments.of("title", 255, "t", MenuType.CATALOG,
                        "Menu title must not exceed 255 characters."),
                Arguments.of("accessCode", 255, "a", MenuType.BUTTON,
                        "Button access code must not exceed 255 characters."),
                Arguments.of("path", 500, "p", MenuType.CATALOG,
                        "Route path must not exceed 500 characters."),
                Arguments.of("component", 255, "c", MenuType.MENU,
                        "Component path must not exceed 255 characters."),
                Arguments.of("redirect", 500, "r", MenuType.CATALOG,
                        "Redirect path must not exceed 500 characters."),
                Arguments.of("activePath", 500, "a", MenuType.CATALOG,
                        "Active menu path must not exceed 500 characters."),
                Arguments.of("badge", 100, "b", MenuType.CATALOG,
                        "Badge content must not exceed 100 characters."),
                Arguments.of("badgeType", 16, "b", MenuType.CATALOG,
                        "Badge type must not exceed 16 characters."),
                Arguments.of("badgeVariants", 32, "b", MenuType.CATALOG,
                        "Badge variant must not exceed 32 characters."),
                Arguments.of("icon", 255, "i", MenuType.CATALOG,
                        "Icon must not exceed 255 characters."),
                Arguments.of("iframeSrc", 500, "i", MenuType.EMBEDDED,
                        "Embedded page URL must not exceed 500 characters."),
                Arguments.of("link", 500, "l", MenuType.LINK,
                        "External URL must not exceed 500 characters."));
    }

    private static Stream<Arguments> missingTypeSpecificFields() {
        return Stream.of(
                Arguments.of(MenuType.CATALOG, "path", "Route path is required."),
                Arguments.of(MenuType.MENU, "path", "Route path is required."),
                Arguments.of(MenuType.BUTTON, "accessCode", "Permission code is required."),
                Arguments.of(MenuType.EMBEDDED, "path", "Route path is required."),
                Arguments.of(MenuType.EMBEDDED, "iframeSrc", "Embedded page URL is required."),
                Arguments.of(MenuType.LINK, "path", "Route path is required."),
                Arguments.of(MenuType.LINK, "link", "External URL is required."));
    }
}
