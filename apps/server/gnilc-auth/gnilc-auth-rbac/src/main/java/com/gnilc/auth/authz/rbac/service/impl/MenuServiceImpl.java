package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.rbac.constant.MenuConstant;
import com.gnilc.auth.authz.rbac.dao.MenuDao;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.dto.MenuDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.auth.authz.rbac.entity.vo.MenuVo;
import com.gnilc.auth.authz.rbac.event.MenuEvent;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.utils.BeanPropertyUtils;
import com.gnilc.common.utils.HttpUrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service("menuService")
public class MenuServiceImpl extends ServiceImpl<MenuDao, MenuBo> implements MenuService {
    private static final String IFRAME_VIEW = "IFrameView";
    private static final TypeReference<Map<String, Object>> QUERY_TYPE = new TypeReference<>() {
    };

    private final MenuDao menuDao;
    private final RoleMenuService roleMenuService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final I18nMessageService messages;

    public MenuServiceImpl(MenuDao menuDao,
                           RoleMenuService roleMenuService,
                           ApplicationEventPublisher eventPublisher,
                           ObjectMapper objectMapper,
                           I18nMessageService messages) {
        this.menuDao = menuDao;
        this.roleMenuService = roleMenuService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.messages = messages;
    }

    @Override
    public List<MenuVo> getMenuTree() {
        List<MenuVo> vos = list().stream()
                .map(bo -> {
                    MenuVo vo = new MenuVo();
                    BeanUtils.copyProperties(bo, vo);
                    return vo;
                })
                .toList();
        Map<Long, MenuVo> voMap = vos.stream()
                .collect(Collectors.toMap(MenuVo::getId, vo -> vo));
        List<MenuVo> roots = new ArrayList<>();
        for (MenuVo vo : vos) {
            Long pid = vo.getPid();
            if (Objects.equals(pid, MenuConstant.ROOT_PARENT_ID)) {
                roots.add(vo);
            }
            MenuVo parent = voMap.get(pid);
            if (parent != null) {
                parent.getChildren().add(vo);
            }
        }
        sortMenuTree(roots);
        return roots;
    }

    @Override
    @Transactional
    public void createMenu(MenuDto dto) {
        Preconditions.checkArgument(dto != null, messages.get("rbac.menu.information.required"));
        MenuBo bo = new MenuBo();
        BeanUtils.copyProperties(dto, bo);
        BeanPropertyUtils.trimToNull(bo);
        bo.setBuiltIn(Boolean.FALSE);
        validateMenu(bo);
        save(bo);
        eventPublisher.publishEvent(new MenuEvent(MenuEvent.Action.CREATE, bo.getId()));
    }

    /**
     * 使用同一个菜单对象完成完整请求的规范化、校验和持久化，确保校验值与落库值一致。
     */
    @Override
    @Transactional
    public void updateMenu(MenuDto dto) {
        Preconditions.checkArgument(dto != null, messages.get("rbac.menu.information.required"));
        Long menuId = dto.getId();
        Preconditions.checkArgument(menuId != null, messages.get("rbac.menu.selection.required"));
        MenuBo menu = getById(menuId);
        Preconditions.checkArgument(menu != null, messages.get("rbac.menu.notFound"));
        Preconditions.checkCondition(!Boolean.TRUE.equals(menu.getBuiltIn()),
                messages.get("rbac.menu.builtIn.modify"));
        Preconditions.checkArgument(dto.getType() != null, messages.get("rbac.menu.type.required"));
        Preconditions.checkCondition(Objects.equals(dto.getType(), menu.getType()),
                messages.get("rbac.menu.type.immutable"));
        BeanUtils.copyProperties(dto, menu);
        BeanPropertyUtils.trimToNull(menu);
        validateMenu(menu);
        updateById(menu);
        eventPublisher.publishEvent(new MenuEvent(MenuEvent.Action.UPDATE, menuId));
    }

    @Transactional
    @Override
    public void removeMenu(Long id) {
        Preconditions.checkArgument(id != null, messages.get("rbac.menu.selection.required"));
        MenuBo bo = getById(id);
        Preconditions.checkArgument(bo != null, messages.get("rbac.menu.notFound"));
        Preconditions.checkCondition(!Boolean.TRUE.equals(bo.getBuiltIn()),
                messages.get("rbac.menu.builtIn.delete"));
        List<Long> menuIds = getSubtreeIds(id);
        List<MenuBo> subtree = getMenus(menuIds);
        Preconditions.checkCondition(subtree.stream()
                        .noneMatch(menu -> Boolean.TRUE.equals(menu.getBuiltIn())),
                messages.get("rbac.menu.builtIn.descendant.delete"));
        subtree.forEach(menu -> {
            String suffix = "_del_" + menu.getId();
            menu.setName(menu.getName() + suffix);
            if (menu.getPath() != null) {
                menu.setPath(menu.getPath() + suffix);
            }
            if (menu.getAccessCode() != null) {
                menu.setAccessCode(menu.getAccessCode() + suffix);
            }
            updateById(menu);
        });
        roleMenuService.removeByMenuIds(menuIds);
        removeByIds(menuIds);
        eventPublisher.publishEvent(new MenuEvent(MenuEvent.Action.DELETE, id));
    }

    @Override
    public List<MenuBo> getMenus(List<Long> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return List.of();
        }
        return lambdaQuery()
                .in(MenuBo::getId, menuIds)
                .orderByAsc(MenuBo::getOrder)
                .list();
    }

    @Override
    public List<MenuBo> getMenusWithAncestors(Set<Long> menuIds, boolean thorough) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return List.of();
        }
        Map<Long, MenuBo> menuMap = list().stream()
                .collect(Collectors.toMap(MenuBo::getId, menu -> menu));
        Map<Long, MenuBo> result = new LinkedHashMap<>();

        for (Long menuId : menuIds) {
            MenuBo menu = menuMap.get(menuId);
            Set<Long> visited = new HashSet<>();
            List<MenuBo> hierarchy = new ArrayList<>();

            // 从所选菜单回溯到根节点，同时检测层级循环。
            while (menu != null && visited.add(menu.getId())) {
                hierarchy.add(menu);
                if (Objects.equals(menu.getPid(), MenuConstant.ROOT_PARENT_ID)) {
                    break;
                }
                menu = menuMap.get(menu.getPid());
            }

            boolean reachesRoot = menu != null
                    && Objects.equals(menu.getPid(), MenuConstant.ROOT_PARENT_ID);
            if (reachesRoot) {
                hierarchy.forEach(m -> result.putIfAbsent(m.getId(), m));
                continue;
            }
            if (!thorough) {
                continue;
            }
            if (hierarchy.isEmpty()) {
                throw new InvalidArgumentException(messages.get("rbac.menu.selected.notFound"));
            }
            if (menu == null) {
                throw new InvalidArgumentException(messages.get("rbac.menu.selectedHierarchy.incomplete"));
            }
            throw new InvalidArgumentException(messages.get("rbac.menu.selectedHierarchy.invalid"));
        }

        return result.values().stream()
                .sorted(Comparator.comparingInt(menu -> Optional.ofNullable(menu.getOrder()).orElse(999)))
                .toList();
    }

    @Override
    public List<MenuRouteVo> getMenuRoutes(List<Long> menuIds) {
        Set<Long> selectedMenuIds = CollectionUtils.isEmpty(menuIds)
                ? Set.of()
                : new HashSet<>(menuIds);
        List<MenuBo> menus = getMenusWithAncestors(selectedMenuIds, false).stream()
                .filter(menu -> Boolean.TRUE.equals(menu.getStatus()))
                .filter(menu -> menu.getType() != MenuType.BUTTON)
                .toList();
        Map<Long, MenuRouteVo> routeMap = menus.stream()
                .collect(Collectors.toMap(MenuBo::getId, this::toMenuRouteVo));
        Map<MenuRouteVo, MenuType> routeTypes = new IdentityHashMap<>();
        menus.forEach(menu -> routeTypes.put(routeMap.get(menu.getId()), menu.getType()));
        List<MenuRouteVo> roots = new ArrayList<>();
        for (MenuBo menu : menus) {
            MenuRouteVo route = routeMap.get(menu.getId());
            if (Objects.equals(menu.getPid(), MenuConstant.ROOT_PARENT_ID)) {
                roots.add(route);
                continue;
            }
            MenuRouteVo parent = routeMap.get(menu.getPid());
            if (parent != null) {
                parent.getChildren().add(route);
            }
        }
        roots.removeIf(route -> !hasNavigableRoute(route, routeTypes));
        sortMenuRoutes(roots);
        return roots;
    }

    @Override
    public MenuBo getMenuByPath(String path) {
        if (StringUtils.isNotBlank(path)) {
            return lambdaQuery()
                    .eq(MenuBo::getPath, path)
                    .one();
        }
        return null;
    }

    @Override
    public MenuBo getMenuByAccessCode(String accessCode) {
        if (StringUtils.isNotBlank(accessCode)) {
            return lambdaQuery()
                    .eq(MenuBo::getAccessCode, accessCode)
                    .one();
        }
        return null;
    }

    private MenuBo getMenuByName(String name) {
        if (StringUtils.isNotBlank(name)) {
            return lambdaQuery()
                    .eq(MenuBo::getName, name)
                    .one();
        }
        return null;
    }

    private void validateMenu(MenuBo bo) {
        Long menuId = bo.getId();
        Long pid = bo.getPid();
        MenuType type = bo.getType();
        String name = bo.getName();
        String title = bo.getTitle();
        String path = bo.getPath();
        String component = bo.getComponent();
        String accessCode = bo.getAccessCode();
        String iframeSrc = bo.getIframeSrc();
        String link = bo.getLink();
        Preconditions.checkArgument(type != null, messages.get("rbac.menu.type.required"));
        Preconditions.checkArgument(pid != null, messages.get("rbac.menu.parent.selection.required"));
        validateParent(menuId, pid, type);
        Preconditions.checkArgument(StringUtils.isNotBlank(name), messages.get("rbac.menu.name.required"));
        Preconditions.checkArgument(StringUtils.isNotBlank(title), messages.get("rbac.menu.title.required"));
        Preconditions.checkArgument(name.codePointCount(0, name.length()) <= 255,
                messages.get("rbac.menu.name.tooLong", 255));
        Preconditions.checkArgument(title.codePointCount(0, title.length()) <= 255,
                messages.get("rbac.menu.title.tooLong", 255));
        Preconditions.checkArgument(accessCode == null || accessCode.codePointCount(0, accessCode.length()) <= 255,
                messages.get("rbac.menu.accessCode.tooLong", 255));
        Preconditions.checkArgument(path == null || path.codePointCount(0, path.length()) <= 500,
                messages.get("rbac.menu.path.tooLong", 500));
        Preconditions.checkArgument(component == null || component.codePointCount(0, component.length()) <= 255,
                messages.get("rbac.menu.component.tooLong", 255));
        Preconditions.checkArgument(bo.getRedirect() == null
                        || bo.getRedirect().codePointCount(0, bo.getRedirect().length()) <= 500,
                messages.get("rbac.menu.redirect.tooLong", 500));
        Preconditions.checkArgument(bo.getActivePath() == null
                        || bo.getActivePath().codePointCount(0, bo.getActivePath().length()) <= 500,
                messages.get("rbac.menu.activePath.tooLong", 500));
        Preconditions.checkArgument(bo.getBadge() == null
                        || bo.getBadge().codePointCount(0, bo.getBadge().length()) <= 100,
                messages.get("rbac.menu.badge.tooLong", 100));
        Preconditions.checkArgument(bo.getBadgeType() == null
                        || bo.getBadgeType().codePointCount(0, bo.getBadgeType().length()) <= 16,
                messages.get("rbac.menu.badgeType.tooLong", 16));
        Preconditions.checkArgument(bo.getBadgeVariants() == null
                        || bo.getBadgeVariants().codePointCount(0, bo.getBadgeVariants().length()) <= 32,
                messages.get("rbac.menu.badgeVariants.tooLong", 32));
        Preconditions.checkArgument(bo.getIcon() == null
                        || bo.getIcon().codePointCount(0, bo.getIcon().length()) <= 255,
                messages.get("rbac.menu.icon.tooLong", 255));
        Preconditions.checkArgument(iframeSrc == null || iframeSrc.codePointCount(0, iframeSrc.length()) <= 500,
                messages.get("rbac.menu.iframeSrc.tooLong", 500));
        Preconditions.checkArgument(link == null || link.codePointCount(0, link.length()) <= 500,
                messages.get("rbac.menu.link.tooLong", 500));
        switch (type) {
            case CATALOG ->
                    Preconditions.checkArgument(StringUtils.isNotBlank(path), messages.get("rbac.menu.path.required"));
            case MENU -> {
                Preconditions.checkArgument(StringUtils.isNotBlank(path), messages.get("rbac.menu.path.required"));
                Preconditions.checkArgument(StringUtils.isNotBlank(component),
                        messages.get("rbac.menu.component.required"));
            }
            case BUTTON -> Preconditions.checkArgument(StringUtils.isNotBlank(accessCode),
                    messages.get("rbac.menu.accessCode.required"));
            case EMBEDDED -> {
                Preconditions.checkArgument(StringUtils.isNotBlank(path), messages.get("rbac.menu.path.required"));
                Preconditions.checkArgument(StringUtils.isNotBlank(iframeSrc),
                        messages.get("rbac.menu.iframeSrc.required"));
                Preconditions.checkArgument(HttpUrlUtils.isValid(iframeSrc),
                        messages.get("rbac.menu.url.invalid"));
            }
            case LINK -> {
                Preconditions.checkArgument(StringUtils.isNotBlank(path), messages.get("rbac.menu.path.required"));
                Preconditions.checkArgument(StringUtils.isNotBlank(link), messages.get("rbac.menu.link.required"));
                Preconditions.checkArgument(HttpUrlUtils.isValid(link),
                        messages.get("rbac.menu.url.invalid"));
            }
        }
        MenuBo nameBo = getMenuByName(name);
        Preconditions.checkArgument(nameBo == null || Objects.equals(nameBo.getId(), menuId),
                messages.get("rbac.menu.name.exists"));
        MenuBo pathBo = getMenuByPath(path);
        Preconditions.checkArgument(pathBo == null || Objects.equals(pathBo.getId(), menuId),
                messages.get("rbac.menu.path.exists"));
        MenuBo accessBo = getMenuByAccessCode(accessCode);
        Preconditions.checkArgument(accessBo == null || Objects.equals(accessBo.getId(), menuId),
                messages.get("rbac.menu.accessCode.exists"));
    }

    /**
     * 校验候选父节点存在、层级完整，并确保调整父级后不会形成循环。
     */
    private void validateParent(Long menuId, Long pid, MenuType childType) {
        if (Objects.equals(pid, MenuConstant.ROOT_PARENT_ID)) {
            Preconditions.checkArgument(childType != MenuType.BUTTON,
                    messages.get("rbac.menu.root.button.unsupported"));
            return;
        }
        MenuBo parent = getById(pid);
        Preconditions.checkArgument(parent != null,
                messages.get("rbac.menu.parent.notFound"));
        Preconditions.checkArgument(allowsChild(parent.getType(), childType),
                messages.get("rbac.menu.parent.type.invalid"));
        Set<Long> visited = new HashSet<>();
        // 从候选父节点回溯到根节点；途中遇到当前菜单即表示会形成父子环。
        while (true) {
            Preconditions.checkArgument(visited.add(parent.getId()),
                    messages.get("rbac.menu.parentHierarchy.invalid"));
            Preconditions.checkArgument(!Objects.equals(parent.getId(), menuId),
                    messages.get("rbac.menu.parent.cycle"));
            if (Objects.equals(parent.getPid(), MenuConstant.ROOT_PARENT_ID)) {
                return;
            }
            parent = getById(parent.getPid());
            Preconditions.checkArgument(parent != null,
                    messages.get("rbac.menu.parentHierarchy.incomplete"));
        }
    }

    private boolean allowsChild(MenuType parentType, MenuType childType) {
        if (parentType == MenuType.CATALOG) {
            return true;
        }
        return parentType == MenuType.MENU && childType == MenuType.BUTTON;
    }

    private boolean hasNavigableRoute(
            MenuRouteVo route,
            Map<MenuRouteVo, MenuType> routeTypes) {
        route.getChildren().removeIf(child -> !hasNavigableRoute(child, routeTypes));
        return routeTypes.get(route) != MenuType.CATALOG || !route.getChildren().isEmpty();
    }

    private List<Long> getSubtreeIds(Long rootId) {
        // IService queries hide logically deleted rows, but deletion must traverse through them.
        return menuDao.getSubtreeIds(rootId, true);
    }

    private MenuRouteVo toMenuRouteVo(MenuBo menu) {
        MenuRouteVo route = new MenuRouteVo();
        route.setName(menu.getName());
        route.setPath(menu.getPath());
        route.setRedirect(menu.getRedirect());
        route.setComponent(switch (menu.getType()) {
            case MENU -> menu.getComponent();
            case EMBEDDED, LINK -> IFRAME_VIEW;
            case BUTTON, CATALOG -> null;
        });
        MenuRouteVo.Meta meta = new MenuRouteVo.Meta();
        BeanUtils.copyProperties(menu, meta, "query");
        meta.setQuery(parseQuery(menu.getQuery()));
        route.setMeta(meta);
        return route;
    }

    /**
     * 将数据库 JSON 字段转换为 Vben {@code RouteMeta.query} 所需的对象结构。
     * Jackson 会把返回的 Map 序列化为 JSON 对象；如果保留 String，则会序列化为带转义的 JSON 字符串。
     */
    private Map<String, Object> parseQuery(String query) {
        if (StringUtils.isBlank(query)) {
            return null;
        }
        try {
            return objectMapper.readValue(query, QUERY_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Menu route query must be a JSON object.", exception);
        }
    }

    private void sortMenuRoutes(List<MenuRouteVo> routes) {
        routes.sort(Comparator.comparingInt(route -> Optional.ofNullable(route.getMeta().getOrder()).orElse(999)));
        for (MenuRouteVo route : routes) {
            sortMenuRoutes(route.getChildren());
        }
    }

    private void sortMenuTree(List<MenuVo> vos) {
        vos.sort(Comparator.comparingInt(vo -> Optional.ofNullable(vo.getOrder()).orElse(999)));
        for (MenuVo vo : vos) {
            List<MenuVo> children = vo.getChildren();
            if (!CollectionUtils.isEmpty(children)) {
                sortMenuTree(children);
            }
        }
    }
}
