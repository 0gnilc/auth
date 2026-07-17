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
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.utils.BeanCopyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
    private final ObjectMapper objectMapper;

    public MenuServiceImpl(MenuDao menuDao,
                           RoleMenuService roleMenuService,
                           ObjectMapper objectMapper) {
        this.menuDao = menuDao;
        this.roleMenuService = roleMenuService;
        this.objectMapper = objectMapper;
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
    public void createMenu(MenuDto dto) {
        Preconditions.checkArgument(dto != null, "Menu information is required.");
        MenuBo bo = new MenuBo();
        BeanUtils.copyProperties(dto, bo);
        validateMenu(bo);
        save(bo);
    }

    @Override
    public void updateMenu(MenuDto dto) {
        Preconditions.checkArgument(dto != null, "Menu information is required.");
        Long menuId = dto.getId();
        Preconditions.checkArgument(menuId != null, "A menu must be selected.");
        MenuBo existing = getById(menuId);
        Preconditions.checkArgument(existing != null, "The menu no longer exists. Refresh and try again.");
        MenuBo bo = new MenuBo();
        BeanUtils.copyProperties(existing, bo);
        BeanCopyUtils.copyNonNullProperties(dto, bo);
        validateMenu(bo);
        updateById(bo);
    }

    @Transactional
    @Override
    public void removeMenu(Long id) {
        Preconditions.checkArgument(id != null, "A menu must be selected.");
        MenuBo bo = getById(id);
        Preconditions.checkArgument(bo != null, "The menu no longer exists. Refresh and try again.");
        List<Long> menuIds = getSubtreeIds(id);
        roleMenuService.removeByMenuIds(menuIds);
        removeByIds(menuIds);
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
        // 逻辑删除的菜单不参与层级查找。
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
                hierarchy.forEach(item -> result.putIfAbsent(item.getId(), item));
                continue;
            }
            if (!thorough) {
                continue;
            }
            if (hierarchy.isEmpty()) {
                throw new InvalidArgumentException(
                        "A selected menu no longer exists. Refresh and try again.");
            }
            if (menu == null) {
                throw new InvalidArgumentException(
                        "The selected menu hierarchy is incomplete.");
            }
            throw new InvalidArgumentException(
                    "The selected menu hierarchy is invalid.");
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
        Preconditions.checkArgument(pid != null, "A parent menu must be selected.");
        validateParent(menuId, pid);
        Preconditions.checkArgument(type != null, "A menu type must be selected.");
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "Menu name is required.");
        Preconditions.checkArgument(StringUtils.isNotBlank(title), "Menu title is required.");
        switch (type) {
            case CATALOG -> Preconditions.checkArgument(StringUtils.isNotBlank(path), "Route path is required.");
            case MENU -> {
                Preconditions.checkArgument(StringUtils.isNotBlank(path), "Route path is required.");
                Preconditions.checkArgument(StringUtils.isNotBlank(component), "Page component is required.");
            }
            case BUTTON -> Preconditions.checkArgument(StringUtils.isNotBlank(accessCode), "Permission code is required.");
            case EMBEDDED -> {
                Preconditions.checkArgument(StringUtils.isNotBlank(path), "Route path is required.");
                Preconditions.checkArgument(StringUtils.isNotBlank(iframeSrc), "Embedded page URL is required.");
            }
            case LINK -> {
                Preconditions.checkArgument(StringUtils.isNotBlank(path), "Route path is required.");
                Preconditions.checkArgument(StringUtils.isNotBlank(link), "External URL is required.");
            }
        }
        MenuBo nameBo = getMenuByName(name);
        Preconditions.checkArgument(nameBo == null || Objects.equals(nameBo.getId(), menuId),
                "A menu with this name already exists.");
        MenuBo pathBo = getMenuByPath(path);
        Preconditions.checkArgument(pathBo == null || Objects.equals(pathBo.getId(), menuId),
                "A menu with this route path already exists.");
        MenuBo accessBo = getMenuByAccessCode(accessCode);
        Preconditions.checkArgument(accessBo == null || Objects.equals(accessBo.getId(), menuId),
                "A menu with this permission code already exists.");
    }

    /**
     * 校验候选父节点存在、层级完整，并确保调整父级后不会形成循环。
     */
    private void validateParent(Long menuId, Long pid) {
        if (Objects.equals(pid, MenuConstant.ROOT_PARENT_ID)) {
            return;
        }
        MenuBo parent = getById(pid);
        Preconditions.checkArgument(parent != null,
                "The parent menu no longer exists. Select another menu.");
        Set<Long> visited = new HashSet<>();
        // 从候选父节点回溯到根节点；途中遇到当前菜单即表示会形成父子环。
        while (true) {
            Preconditions.checkArgument(visited.add(parent.getId()),
                    "The parent menu hierarchy is invalid.");
            Preconditions.checkArgument(!Objects.equals(parent.getId(), menuId),
                    "A menu cannot use itself or one of its descendants as its parent.");
            if (Objects.equals(parent.getPid(), MenuConstant.ROOT_PARENT_ID)) {
                return;
            }
            parent = getById(parent.getPid());
            Preconditions.checkArgument(parent != null,
                    "The parent menu hierarchy is incomplete.");
        }
    }

    private List<Long> getSubtreeIds(Long rootId) {
        // IService queries hide logically deleted rows, but deletion must traverse through them.
        return menuDao.selectCompleteSubtreeIds(rootId);
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
