package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.common.base.Preconditions;
import com.gnilc.authz.rbac.common.constant.MenuConstant;
import com.gnilc.authz.rbac.common.utils.BeanCopyUtils;
import com.gnilc.authz.rbac.dao.MenuDao;
import com.gnilc.authz.rbac.entity.bo.MenuBo;
import com.gnilc.authz.rbac.entity.dto.MenuDto;
import com.gnilc.authz.rbac.entity.enums.MenuType;
import com.gnilc.authz.rbac.entity.vo.MenuVo;
import com.gnilc.authz.rbac.service.MenuService;
import com.gnilc.authz.rbac.service.RoleMenuService;
import com.gnilc.authz.rbac.service.UserRoleService;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;


@Service("menuService")
public class MenuServiceImpl extends ServiceImpl<MenuDao, MenuBo> implements MenuService {

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private RoleMenuService roleMenuService;

    @Override
    public List<MenuVo> getMenuTree() {
        List<MenuVo> mvs = list()
                .stream().map(mb -> {
                    MenuVo mv = new MenuVo();
                    BeanUtils.copyProperties(mb, mv);
                    return mv;
                }).toList();
        Map<Long, MenuVo> mvMap = mvs.stream().collect(Collectors.toMap(MenuVo::getId, mv -> mv));
        List<MenuVo> roots = Lists.newArrayList();
        for (MenuVo mv : mvs) {
            Long pid = mv.getPid();
            if (Objects.equals(pid, MenuConstant.ROOT_PARENT_ID)) {
                roots.add(mv);
            }
            MenuVo pmv = mvMap.get(pid);
            if (pmv != null) {
                pmv.getChildren().add(mv);
            }
        }
        sortMenuTree(roots);
        return roots;
    }

    @Override
    public void createMenu(MenuDto md) {
        Preconditions.checkArgument(md != null, "请填写菜单信息");
        MenuBo mb = new MenuBo();
        BeanUtils.copyProperties(md, mb);
        validateMenu(mb);
        save(mb);
    }

    @Override
    public void updateMenu(MenuDto md) {
        Preconditions.checkArgument(md != null, "请填写菜单信息");
        Long id = md.getId();
        Preconditions.checkArgument(id != null, "请选择菜单");
        MenuBo mb = getById(id);
        Preconditions.checkArgument(mb != null, "菜单不存在，请刷新后重试");
        BeanCopyUtils.copyNonNullProperties(md, mb);
        validateMenu(mb);
        updateById(mb);
    }

    @Override
    public void removeMenu(Long id) {
        Preconditions.checkArgument(id != null, "请选择菜单");
        MenuBo mb = getById(id);
        Preconditions.checkArgument(mb != null, "菜单不存在，请刷新后重试");
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

    private void validateMenu(MenuBo mb) {
        Long menuId = mb.getId();
        Long pid = mb.getPid();
        MenuType type = mb.getType();
        String name = mb.getName();
        String title = mb.getTitle();
        String path = mb.getPath();
        String component = mb.getComponent();
        String accessCode = mb.getAccessCode();
        String iframeSrc = mb.getIframeSrc();
        String link = mb.getLink();
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
        MenuBo nmb = getMenuByName(name);
        Preconditions.checkArgument(nmb == null || Objects.equals(nmb.getId(), menuId),
                "菜单名称已存在");
        MenuBo pmb = getMenuByPath(path);
        Preconditions.checkArgument(pmb == null || Objects.equals(pmb.getId(), menuId),
                "路由路径已存在");
        MenuBo cmb = getMenuByAccessCode(accessCode);
        Preconditions.checkArgument(cmb == null || Objects.equals(cmb.getId(), menuId),
                "权限标识已存在");
    }

    private void sortMenuTree(List<MenuVo> mvs) {
        mvs.sort(Comparator.comparingInt(mv -> Optional.ofNullable(mv.getOrder()).orElse(999)));
        for (MenuVo mv : mvs) {
            List<MenuVo> children = mv.getChildren();
            if (!CollectionUtils.isEmpty(children)) {
                sortMenuTree(children);
            }
        }
    }
}
