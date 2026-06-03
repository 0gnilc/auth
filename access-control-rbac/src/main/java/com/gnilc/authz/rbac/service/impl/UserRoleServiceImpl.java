package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.common.base.Preconditions;
import com.gnilc.authz.rbac.dao.UserRoleDao;
import com.gnilc.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.authz.rbac.service.UserRoleService;
import com.gnilc.authz.rbac.service.event.UserRoleEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;


@Service("userRoleService")
public class UserRoleServiceImpl extends ServiceImpl<UserRoleDao, UserRoleBo> implements UserRoleService {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Transactional
    @Override
    public void saveUserRole(UserRoleDto userRoleDto) {
        Long userId = userRoleDto.getUserId();
        List<Long> roleIds = userRoleDto.getRoleIds();
        Preconditions.checkArgument(userId != null, "userId == null");
        remove(new LambdaQueryWrapper<UserRoleBo>()
                .eq(UserRoleBo::getUserId, userId));
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        List<UserRoleBo> urs = roleIds.stream().map(roleId -> {
            UserRoleBo userRole = new UserRoleBo();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            return userRole;
        }).toList();
        saveBatch(urs);
        publisher.publishEvent(new UserRoleEvent(this, userId));
    }

    @Override
    public List<Long> getRoleIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<UserRoleBo>()
                .eq(UserRoleBo::getUserId, userId))
                .stream()
                .map(UserRoleBo::getRoleId)
                .toList();
    }

    @Transactional
    @Override
    public void bindRole(Long userId, Long roleId) {
        UserRoleBo userRole = new UserRoleBo();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        saveOrUpdate(userRole);
        publisher.publishEvent(new UserRoleEvent(this, userId));
    }

    @Override
    public void unbindRole(Long userId, Long roleId) {
        remove(new LambdaQueryWrapper<UserRoleBo>()
                .eq(UserRoleBo::getUserId, userId)
                .eq(UserRoleBo::getRoleId, roleId));
        publisher.publishEvent(new UserRoleEvent(this, userId));
    }

    @Override
    public List<Long> getUserIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<UserRoleBo>()
                .select(UserRoleBo::getUserId)
                .in(UserRoleBo::getRoleId, roleIds))
                .stream().map(UserRoleBo::getUserId).toList();
    }

    @Override
    public List<Long> getUserIds(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return getUserIds(List.of(roleId));
    }

    @Override
    public UserRoleBo getUserRole(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(UserRoleBo.class)
                .eq(UserRoleBo::getUserId, userId)
                .eq(UserRoleBo::getRoleId, roleId));
    }
}