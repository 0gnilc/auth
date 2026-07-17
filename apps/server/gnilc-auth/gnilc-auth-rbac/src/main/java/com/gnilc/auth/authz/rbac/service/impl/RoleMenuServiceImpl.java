package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.auth.authz.rbac.dao.RoleMenusDao;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleMenuBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleMenuDto;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service("roleMenuServiceImpl")
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenusDao, RoleMenuBo> implements RoleMenuService {
    private final MenuService menuService;

    public RoleMenuServiceImpl(@Lazy MenuService menuService) {
        this.menuService = menuService;
    }

    @Override
    public List<Long> getMenuIds(Long roleId) {
        return lambdaQuery()
                .select(RoleMenuBo::getMenuId)
                .eq(RoleMenuBo::getRoleId, roleId)
                .list()
                .stream()
                .map(RoleMenuBo::getMenuId)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> getMenuIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return lambdaQuery()
                .select(RoleMenuBo::getMenuId)
                .in(RoleMenuBo::getRoleId, roleIds)
                .list()
                .stream()
                .map(RoleMenuBo::getMenuId)
                .distinct()
                .toList();
    }

    @Transactional
    @Override
    public void updateRoleMenu(RoleMenuDto dto) {
        Preconditions.checkArgument(dto != null, "Role menu assignment information is required.");
        Long roleId = dto.getRoleId();
        List<Long> menuIds = dto.getMenuIds();
        Preconditions.checkArgument(roleId != null, "A role must be selected.");

        Set<Long> oldSet = lambdaQuery()
                .select(RoleMenuBo::getMenuId)
                .eq(RoleMenuBo::getRoleId, roleId)
                .list()
                .stream()
                .map(RoleMenuBo::getMenuId)
                .collect(Collectors.toSet());

        Set<Long> selectedMenuIds = CollectionUtils.isEmpty(menuIds)
                ? Set.of()
                : new HashSet<>(menuIds);
        Set<Long> newSet = menuService.getMenusWithAncestors(selectedMenuIds, true).stream()
                .map(MenuBo::getId)
                .collect(Collectors.toSet());

        Set<Long> removeSet = Sets.difference(oldSet, newSet);
        if (!removeSet.isEmpty()) {
            lambdaUpdate()
                    .eq(RoleMenuBo::getRoleId, roleId)
                    .in(RoleMenuBo::getMenuId, removeSet)
                    .remove();
        }

        List<RoleMenuBo> bos = Sets.difference(newSet, oldSet)
                .stream()
                .map(menuId -> {
                    RoleMenuBo bo = new RoleMenuBo();
                    bo.setRoleId(roleId);
                    bo.setMenuId(menuId);
                    return bo;
                }).toList();
        if (!bos.isEmpty()) {
            saveBatch(bos);
        }
    }

    @Transactional
    @Override
    public void removeByMenuIds(List<Long> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return;
        }
        lambdaUpdate()
                .in(RoleMenuBo::getMenuId, menuIds)
                .remove();
    }
}
