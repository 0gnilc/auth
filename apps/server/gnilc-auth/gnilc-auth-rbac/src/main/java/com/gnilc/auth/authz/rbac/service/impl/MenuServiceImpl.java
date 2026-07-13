package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.auth.authz.rbac.constant.MenuConstant;
import com.gnilc.common.utils.BeanCopyUtils;
import com.gnilc.auth.authz.rbac.dao.MenuDao;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.dto.MenuDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.entity.vo.MenuVo;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Service("menuService")
public class MenuServiceImpl extends ServiceImpl<MenuDao, MenuBo> implements MenuService {

    private final UserRoleService userRoleService;
    private final RoleMenuService roleMenuService;

    public MenuServiceImpl(UserRoleService userRoleService, RoleMenuService roleMenuService) {
        this.userRoleService = userRoleService;
        this.roleMenuService = roleMenuService;
    }

    @Override
    public List<MenuVo> getMenuTree() {
        List<MenuVo> vos = list().stream()
                .map(this::toMenuVo)
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
        MenuBo bo = getById(menuId);
        Preconditions.checkArgument(bo != null, "The menu no longer exists. Refresh and try again.");
        BeanCopyUtils.copyNonNullProperties(dto, bo);
        validateMenu(bo);
        updateById(bo);
    }

    @Override
    public void removeMenu(Long id) {
        Preconditions.checkArgument(id != null, "A menu must be selected.");
        MenuBo bo = getById(id);
        Preconditions.checkArgument(bo != null, "The menu no longer exists. Refresh and try again.");
        removeById(id);
    }

    @Override
    public List<MenuBo> getMenus(Long userId) {
        List<Long> roleIds = userRoleService.getRoleIds(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        List<Long> menuIds = roleMenuService.getMenuIds(roleIds);
        if (CollectionUtils.isEmpty(menuIds)) {
            return List.of();
        }
        return getMenus(menuIds);
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
        if (!Objects.equals(pid, MenuConstant.ROOT_PARENT_ID)) {
            Preconditions.checkArgument(getById(pid) != null, "The parent menu no longer exists. Select another menu.");
        }
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
            case LINK -> Preconditions.checkArgument(StringUtils.isNotBlank(link), "External URL is required.");
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

    private void sortMenuTree(List<MenuVo> vos) {
        vos.sort(Comparator.comparingInt(vo -> Optional.ofNullable(vo.getOrder()).orElse(999)));
        for (MenuVo vo : vos) {
            List<MenuVo> children = vo.getChildren();
            if (!CollectionUtils.isEmpty(children)) {
                sortMenuTree(children);
            }
        }
    }

    private MenuVo toMenuVo(MenuBo bo) {
        MenuVo vo = new MenuVo();
        BeanUtils.copyProperties(bo, vo);
        return vo;
    }
}
