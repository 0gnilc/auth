package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.common.base.Preconditions;
import com.gnilc.authz.rbac.dao.PermissionDao;
import com.gnilc.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.authz.rbac.entity.vo.PermissionVo;
import com.gnilc.authz.rbac.service.RolePermissionService;
import com.gnilc.authz.rbac.service.UserRoleService;
import com.gnilc.authz.rbac.service.event.CrudEvent;
import com.gnilc.authz.rbac.service.event.PermissionEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.gnilc.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.authz.rbac.service.PermissionService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


import java.util.List;

@Service("permissionService")
public class PermissionServiceImpl extends ServiceImpl<PermissionDao, PermissionBo> implements PermissionService {
    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private RolePermissionService rolePermissionService;

    @Transactional
    @Override
    public void savePermission(PermissionDto permissionDto) {
        String name = permissionDto.getName();
        String symbol = permissionDto.getSymbol();
        String resource = permissionDto.getResource();
        String remark = permissionDto.getRemark();
        Boolean exposed = permissionDto.getExposed();
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "请输入权限名称");
        Preconditions.checkArgument(StringUtils.isNotBlank(symbol), "请输入权限标识");
        Preconditions.checkArgument(StringUtils.isNotBlank(resource), "请输入权限主体");
        PermissionBo sp = getPermissionBySymbol(symbol);
        Preconditions.checkArgument(sp == null, "权限标识已存在");
        PermissionBo permission = new PermissionBo();
        permission.setName(name);
        permission.setSymbol(symbol);
        permission.setResource(resource);
        permission.setRemark(remark);
        permission.setExposed(exposed);
        save(permission);
        publisher.publishEvent(new PermissionEvent(this, CrudEvent.Event.CREATE, permission.getId()));
    }


    @Transactional
    @Override
    public void modifyPermission(PermissionDto pd) {
        Long id = pd.getId();
        String name = pd.getName();
        String symbol = pd.getSymbol();
        String resource = pd.getResource();
        String remark = pd.getRemark();
        Boolean exposed = pd.getExposed();
        Preconditions.checkArgument(id != null, "id cannot be empty!");
        PermissionBo permission = getById(id);
        Preconditions.checkCondition(permission != null, "请刷新后再试");
        if (StringUtils.isNotBlank(symbol) && !symbol.equals(permission.getSymbol())) {
            PermissionBo sp = getPermissionBySymbol(symbol);
            Preconditions.checkArgument(sp == null, "权限标识已存在");
        }
        permission.setName(name);
        permission.setSymbol(symbol);
        permission.setResource(resource);
        permission.setRemark(remark);
        permission.setExposed(exposed);
        updateById(permission);
        publisher.publishEvent(new PermissionEvent(this, CrudEvent.Event.UPDATE, id));
    }

    @Transactional
    @Override
    public void removePermission(Long id) {
        PermissionBo permission = getById(id);
        Preconditions.checkCondition(permission != null, "请刷新后再试");
        removeById(id);
        publisher.publishEvent(new PermissionEvent(this, CrudEvent.Event.DELETE, id));
    }

    @Override
    public List<PermissionVo> getPermissions(PermissionQueryDto qd) {
        String symbol = qd.getSymbol();
        String name = qd.getName();
        String resource = qd.getResource();
        LambdaQueryWrapper<PermissionBo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(symbol), PermissionBo::getSymbol, symbol);
        wrapper.like(StringUtils.isNotBlank(name), PermissionBo::getName, name);
        wrapper.like(StringUtils.isNotBlank(resource), PermissionBo::getResource, resource);
        return list(wrapper).stream().map(p -> {
            PermissionVo pv = new PermissionVo();
            BeanUtils.copyProperties(p, pv);
            return pv;
        }).toList();
    }

    @Override
    public PermissionBo getPermissionBySymbol(String symbol) {
        if (StringUtils.isNotBlank(symbol)) {
            return getOne(new LambdaQueryWrapper<PermissionBo>()
                    .eq(PermissionBo::getSymbol, symbol));
        }
        return null;
    }

    @Override
    public List<PermissionBo> getPermissions(Long userId) {
        List<Long> roleIds = userRoleService.getRoleIds(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionService.getPermissionIds(roleIds);
        if (CollectionUtils.isEmpty(permissionIds)) {
            return List.of();
        }
        return getPermissions(permissionIds);
    }

    @Override
    public List<PermissionBo> getPermissions(List<Long> ids) {
        Preconditions.checkArgument(ids != null, "ids == null");
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return listByIds(ids);
    }
}