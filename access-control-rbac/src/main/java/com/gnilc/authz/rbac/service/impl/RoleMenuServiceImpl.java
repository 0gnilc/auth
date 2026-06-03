package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.dao.RoleMenusDao;
import com.gnilc.authz.rbac.entity.bo.RoleMenuBo;
import com.gnilc.authz.rbac.entity.dto.RoleMenuDto;
import com.gnilc.authz.rbac.service.RoleMenuService;
import com.gnilc.authz.rbac.service.event.RoleMenuEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;


@Service("roleMenuServiceImpl")
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenusDao, RoleMenuBo> implements RoleMenuService {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public List<Long> getMenuIds(Long roleId) {
        List<RoleMenuBo> list = list(new LambdaQueryWrapper<RoleMenuBo>()
                .eq(RoleMenuBo::getRoleId, roleId));
        return list.stream().map(RoleMenuBo::getMenuId).toList();
    }

    @Override
    public List<Long> getMenuIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        List<RoleMenuBo> list = list(new LambdaQueryWrapper<RoleMenuBo>()
                .in(RoleMenuBo::getRoleId, roleIds));
        return list.stream().map(RoleMenuBo::getMenuId).toList();
    }

    @Transactional
    @Override
    public void saveRoleMenu(RoleMenuDto roleMenuDto) {
        Long roleId = roleMenuDto.getRoleId();
        List<Long> menuIds = roleMenuDto.getMenuIds();
        remove(new LambdaQueryWrapper<RoleMenuBo>()
                .eq(RoleMenuBo::getRoleId, roleId));
        if (CollectionUtils.isEmpty(menuIds)) {
            return;
        }
        List<RoleMenuBo> roleMenus = menuIds.stream().map(menuId -> {
            RoleMenuBo roleMenu = new RoleMenuBo();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            return roleMenu;
        }).toList();
        saveBatch(roleMenus);

        publisher.publishEvent(new RoleMenuEvent(this, roleId));
    }
}