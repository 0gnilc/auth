package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.common.base.Preconditions;
import com.gnilc.authz.rbac.common.constant.MenuConstant;
import com.gnilc.authz.rbac.dao.MenuDao;
import com.gnilc.authz.rbac.entity.bo.MenuBo;
import com.gnilc.authz.rbac.entity.dto.MenuDto;
import com.gnilc.authz.rbac.entity.vo.MenuVo;
import com.gnilc.authz.rbac.service.MenuService;
import com.gnilc.authz.rbac.service.RoleMenuService;
import com.gnilc.authz.rbac.service.UserRoleService;
import com.gnilc.authz.rbac.service.event.CrudEvent;
import com.gnilc.authz.rbac.service.event.MenuEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.util.*;
import java.util.stream.Collectors;


@Service("menuService")
public class MenuServiceImpl extends ServiceImpl<MenuDao, MenuBo> implements MenuService {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private RoleMenuService roleMenuService;

    @Override
    public List<MenuVo> getMenuTree() {
        List<MenuVo> list = list().stream().map(m -> {
            MenuVo mv = new MenuVo();
            BeanUtils.copyProperties(m, mv);
            return mv;
        }).toList();
        Map<Long, MenuVo> map = list.stream().collect(Collectors.toMap(MenuVo::getId, m -> m));
        ArrayList<MenuVo> roots = new ArrayList<>();
        for (MenuVo mv : list) {
            Long parentId = mv.getParentId();
            if (parentId == MenuConstant.ROOT_PARENT_ID) {
                roots.add(mv);
            }
            MenuVo pmv = map.get(parentId);
            if (pmv != null) {
                pmv.getChildren().add(mv);
            }
        }
        // 排序
        sortMenuTree(roots);
        return roots;
    }

    @Override
    public void saveMenu(MenuDto menuDto) {
        Long parentId = menuDto.getParentId();
        Integer type = menuDto.getType();
        String path = menuDto.getPath();
        String name = menuDto.getName();
        String title = menuDto.getTitle();
        String symbol = menuDto.getSymbol();
        Preconditions.checkArgument(parentId != null, "请选择父菜单");
        if (parentId != MenuConstant.ROOT_PARENT_ID) {
            Preconditions.checkArgument(getById(parentId) != null, "请选择父菜单");
        }
        Preconditions.checkArgument(MenuConstant.TYPES.contains(type), "请选择菜单类型");
        Preconditions.checkArgument(StringUtils.isNotBlank(title), "请输入菜单名称");
        if (type == MenuConstant.TYPE_MENU || type == MenuConstant.TYPE_IFRAME || type == MenuConstant.TYPE_EXTERNAL_LINK) {
            Preconditions.checkArgument(StringUtils.isNotBlank(path), "请输入路由路径");
            Preconditions.checkArgument(getMenuByPath(path) == null, "路由路径已存在");
            Preconditions.checkArgument(StringUtils.isNotBlank(name), "请输入路由名称");
        }
        if (type == MenuConstant.TYPE_BUTTON) {
            Preconditions.checkArgument(StringUtils.isNotBlank(symbol), "请输入权限标识");
            MenuBo sm = getMenuBySymbol(symbol);
            Preconditions.checkArgument(sm == null, "权限标识已存在");
        } else {
            menuDto.setSymbol(path);
        }
        MenuBo menu = new MenuBo();
        BeanUtils.copyProperties(menuDto, menu);
        save(menu);
        publisher.publishEvent(new MenuEvent(this, CrudEvent.Event.CREATE, menu.getId()));
    }

    @Override
    public void modifyMenu(MenuDto menuDto) {
        Long id = menuDto.getId();
        MenuBo menu = getById(id);
        Preconditions.checkArgument(menu != null, "请刷新后再试");
        Long parentId = Optional.ofNullable(menuDto.getParentId()).orElse(menu.getParentId());
        Integer type = Optional.ofNullable(menuDto.getType()).orElse(menu.getType());
        String title = Optional.ofNullable(menuDto.getTitle()).orElse(menu.getTitle());
        String path = Optional.ofNullable(menuDto.getPath()).orElse(menu.getPath());
        String name = Optional.ofNullable(menuDto.getName()).orElse(menu.getName());
        String symbol = Optional.ofNullable(menuDto.getSymbol()).orElse(menu.getSymbol());
        if (parentId != MenuConstant.ROOT_PARENT_ID) {
            Preconditions.checkArgument(getById(parentId) != null, "请选择父菜单");
        }
        Preconditions.checkArgument(MenuConstant.TYPES.contains(type), "请选择菜单类型");
        Preconditions.checkArgument(StringUtils.isNotBlank(title), "请输入菜单名称");
        if (type == MenuConstant.TYPE_MENU || type == MenuConstant.TYPE_IFRAME || type == MenuConstant.TYPE_EXTERNAL_LINK) {
            Preconditions.checkArgument(StringUtils.isNotBlank(path), "请输入路由路径");
            MenuBo pm = getMenuByPath(path);
            Preconditions.checkArgument(pm == null || Objects.equals(pm.getId(), menu.getId()), "路由路径已存在");
            Preconditions.checkArgument(StringUtils.isNotBlank(name), "请输入路由名称");
        }
        if (type == MenuConstant.TYPE_BUTTON) {
            Preconditions.checkArgument(StringUtils.isNotBlank(symbol), "请输入权限标识");
            MenuBo sm = getMenuBySymbol(symbol);
            Preconditions.checkArgument(sm == null || Objects.equals(sm.getId(), menu.getId()), "权限标识已存在");
        } else {
            menuDto.setSymbol(path);
        }
        BeanUtils.copyProperties(menuDto, menu);
        updateById(menu);
        publisher.publishEvent(new MenuEvent(this, CrudEvent.Event.UPDATE, menu.getId()));
    }

    @Override
    public void removeMenu(Long id) {
        MenuBo menu = getById(id);
        Preconditions.checkArgument(menu != null, "请刷新后再试");
        removeById(id);
        publisher.publishEvent(new MenuEvent(this, CrudEvent.Event.DELETE, id));
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
        return list(new LambdaQueryWrapper<MenuBo>()
                .in(MenuBo::getId, menuIds)
                .orderByAsc(MenuBo::getSort));
    }

    @Override
    public MenuBo getMenuByPath(String path) {
        if (StringUtils.isNotBlank(path)) {
            return getOne(new LambdaQueryWrapper<MenuBo>()
                    .eq(MenuBo::getPath, path));
        }
        return null;
    }

    @Override
    public MenuBo getMenuBySymbol(String symbol) {
        if (StringUtils.isNotBlank(symbol)) {
            return getOne(new LambdaQueryWrapper<MenuBo>()
                    .eq(MenuBo::getSymbol, symbol));
        }
        return null;
    }

    private void sortMenuTree(List<MenuVo> mvs) {
        mvs.sort(Comparator.comparingInt(MenuVo::getSort));
        for (MenuVo mv : mvs) {
            List<MenuVo> children = mv.getChildren();
            if (!CollectionUtils.isEmpty(children)) {
                sortMenuTree(children);
            }
        }
    }
}