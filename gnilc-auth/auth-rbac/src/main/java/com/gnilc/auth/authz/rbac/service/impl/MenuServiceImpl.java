package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.auth.authz.rbac.common.base.Preconditions;
import com.gnilc.auth.authz.rbac.common.constant.MenuConstant;
import com.gnilc.auth.authz.rbac.common.utils.BeanCopyUtils;
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
        Preconditions.checkArgument(dto != null, "请填写菜单信息");
        MenuBo bo = new MenuBo();
        BeanUtils.copyProperties(dto, bo);
        validateMenu(bo);
        save(bo);
    }

    @Override
    public void updateMenu(MenuDto dto) {
        Preconditions.checkArgument(dto != null, "请填写菜单信息");
        Long menuId = dto.getId();
        Preconditions.checkArgument(menuId != null, "请选择菜单");
        MenuBo bo = getById(menuId);
        Preconditions.checkArgument(bo != null, "菜单不存在，请刷新后重试");
        BeanCopyUtils.copyNonNullProperties(dto, bo);
        validateMenu(bo);
        updateById(bo);
    }

    @Override
    public void removeMenu(Long id) {
        Preconditions.checkArgument(id != null, "请选择菜单");
        MenuBo bo = getById(id);
        Preconditions.checkArgument(bo != null, "菜单不存在，请刷新后重试");
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
        Preconditions.checkArgument(pid != null, "请选择父菜单");
        if (!Objects.equals(pid, MenuConstant.ROOT_PARENT_ID)) {
            Preconditions.checkArgument(getById(pid) != null, "父菜单不存在，请重新选择");
        }
        Preconditions.checkArgument(type != null, "请选择菜单类型");
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "请输入菜单名称");
        Preconditions.checkArgument(StringUtils.isNotBlank(title), "请输入菜单标题");
        switch (type) {
            case CATALOG -> Preconditions.checkArgument(StringUtils.isNotBlank(path), "请输入路由路径");
            case MENU -> {
                Preconditions.checkArgument(StringUtils.isNotBlank(path), "请输入路由路径");
                Preconditions.checkArgument(StringUtils.isNotBlank(component), "请输入页面组件");
            }
            case BUTTON -> Preconditions.checkArgument(StringUtils.isNotBlank(accessCode), "请输入权限标识");
            case EMBEDDED -> {
                Preconditions.checkArgument(StringUtils.isNotBlank(path), "请输入路由路径");
                Preconditions.checkArgument(StringUtils.isNotBlank(iframeSrc), "请输入内嵌页面地址");
            }
            case LINK -> Preconditions.checkArgument(StringUtils.isNotBlank(link), "请输入外链地址");
        }
        MenuBo nameBo = getMenuByName(name);
        Preconditions.checkArgument(nameBo == null || Objects.equals(nameBo.getId(), menuId),
                "菜单名称已存在");
        MenuBo pathBo = getMenuByPath(path);
        Preconditions.checkArgument(pathBo == null || Objects.equals(pathBo.getId(), menuId),
                "路由路径已存在");
        MenuBo accessBo = getMenuByAccessCode(accessCode);
        Preconditions.checkArgument(accessBo == null || Objects.equals(accessBo.getId(), menuId),
                "权限标识已存在");
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
