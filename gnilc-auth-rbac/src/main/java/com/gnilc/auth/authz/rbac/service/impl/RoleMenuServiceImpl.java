package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.auth.authz.rbac.common.base.Preconditions;
import com.gnilc.auth.authz.rbac.dao.RoleMenusDao;
import com.gnilc.auth.authz.rbac.entity.bo.RoleMenuBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleMenuDto;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.google.common.collect.Sets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service("roleMenuServiceImpl")
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenusDao, RoleMenuBo> implements RoleMenuService {

    @Override
    public List<Long> getMenuIds(Long roleId) {
        return lambdaQuery()
                .select(RoleMenuBo::getMenuId)
                .eq(RoleMenuBo::getRoleId, roleId)
                .list().stream().
                map(RoleMenuBo::getMenuId)
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
                .stream().map(RoleMenuBo::getMenuId)
                .distinct()
                .toList();
    }

    @Transactional
    @Override
    public void updateRoleMenu(RoleMenuDto rmd) {
        Preconditions.checkArgument(rmd != null, "请填写角色菜单信息");
        Long roleId = rmd.getRoleId();
        List<Long> menuIds = rmd.getMenuIds();
        Preconditions.checkArgument(roleId != null, "请选择角色");

        Set<Long> oldSet = lambdaQuery()
                .select(RoleMenuBo::getMenuId)
                .eq(RoleMenuBo::getRoleId, roleId)
                .list()
                .stream()
                .map(RoleMenuBo::getMenuId)
                .collect(Collectors.toSet());
        Set<Long> newSet = CollectionUtils.isEmpty(menuIds) ? Set.of() : Sets.newHashSet(menuIds);

        Set<Long> removeSet = Sets.difference(oldSet, newSet);
        if (!removeSet.isEmpty()) {
            lambdaUpdate()
                    .eq(RoleMenuBo::getRoleId, roleId)
                    .in(RoleMenuBo::getMenuId, removeSet)
                    .remove();
        }

        List<RoleMenuBo> rmbs = Sets.difference(newSet, oldSet)
                .stream()
                .map(menuId -> {
                    RoleMenuBo rmb = new RoleMenuBo();
                    rmb.setRoleId(roleId);
                    rmb.setMenuId(menuId);
                    return rmb;
                }).toList();
        if (!rmbs.isEmpty()) {
            saveBatch(rmbs);
        }
    }
}
