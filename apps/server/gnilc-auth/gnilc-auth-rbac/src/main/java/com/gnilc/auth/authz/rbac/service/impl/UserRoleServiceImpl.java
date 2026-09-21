package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.auth.authz.rbac.dao.UserRoleDao;
import com.gnilc.auth.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.event.AuthorizationEvent;
import com.gnilc.auth.authz.rbac.service.RequiredRolePolicy;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.google.common.collect.Sets;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;


/** 维护用户角色关系，在移除前执行应用提供的必需角色策略并发布授权变化事件。 */
@Service("userRoleService")
public class UserRoleServiceImpl extends ServiceImpl<UserRoleDao, UserRoleBo> implements UserRoleService {

    private final ApplicationEventPublisher eventPublisher;
    private final List<RequiredRolePolicy> requiredRolePolicies;
    private final I18nMessageService messages;

    public UserRoleServiceImpl(ApplicationEventPublisher eventPublisher,
                               List<RequiredRolePolicy> requiredRolePolicies,
                               I18nMessageService messages) {
        this.eventPublisher = eventPublisher;
        this.requiredRolePolicies = requiredRolePolicies;
        this.messages = messages;
    }

    @Transactional
    @Override
    public void updateUserRole(UserRoleDto dto) {
        Preconditions.checkArgument(dto != null, messages.get("rbac.assignment.userRole.required"));
        Long userId = dto.getUserId();
        List<Long> roleIds = dto.getRoleIds();
        Preconditions.checkArgument(userId != null, messages.get("rbac.user.selection.required"));
        Set<Long> oldSet = Set.copyOf(getRoleIds(userId));
        Set<Long> newSet = CollectionUtils.isEmpty(roleIds) ? Set.of() : Sets.newHashSet(roleIds);

        Set<Long> removeSet = Sets.difference(oldSet, newSet);
        ensureRemovable(userId, removeSet);
        if (!removeSet.isEmpty()) {
            lambdaUpdate()
                    .eq(UserRoleBo::getUserId, userId)
                    .in(UserRoleBo::getRoleId, removeSet)
                    .remove();
        }

        List<UserRoleBo> bos = Sets.difference(newSet, oldSet)
                .stream()
                .map(roleId -> {
                    UserRoleBo bo = new UserRoleBo();
                    bo.setUserId(userId);
                    bo.setRoleId(roleId);
                    return bo;
                }).toList();
        if (!bos.isEmpty()) {
            saveBatch(bos);
        }

        eventPublisher.publishEvent(AuthorizationEvent.of(
                AuthorizationEvent.Type.USER_ROLE,
                AuthorizationEvent.Action.REPLACE,
                userId));
    }

    @Override
    public List<Long> getRoleIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return lambdaQuery()
                .select(UserRoleBo::getRoleId)
                .eq(UserRoleBo::getUserId, userId)
                .list()
                .stream()
                .map(UserRoleBo::getRoleId)
                .distinct()
                .toList();
    }

    @Transactional
    @Override
    public void bindRole(Long userId, Long roleId) {
        Preconditions.checkArgument(userId != null, "A user must be selected.");
        Preconditions.checkArgument(roleId != null, "A role must be selected.");
        UserRoleBo bo = getUserRole(userId, roleId);
        if (bo == null) {
            bo = new UserRoleBo();
            bo.setUserId(userId);
            bo.setRoleId(roleId);
            save(bo);
        }
        eventPublisher.publishEvent(AuthorizationEvent.of(
                AuthorizationEvent.Type.USER_ROLE,
                AuthorizationEvent.Action.REPLACE,
                userId));
    }

    @Transactional
    @Override
    public void unbindRole(Long userId, Long roleId) {
        Preconditions.checkArgument(userId != null, "A user must be selected.");
        Preconditions.checkArgument(roleId != null, "A role must be selected.");
        ensureRemovable(userId, Set.of(roleId));
        remove(new LambdaQueryWrapper<UserRoleBo>()
                .eq(UserRoleBo::getUserId, userId)
                .eq(UserRoleBo::getRoleId, roleId));
        eventPublisher.publishEvent(AuthorizationEvent.of(
                AuthorizationEvent.Type.USER_ROLE,
                AuthorizationEvent.Action.REPLACE,
                userId));
    }

    @Override
    public List<Long> getUserIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return lambdaQuery()
                .select(UserRoleBo::getUserId)
                .in(UserRoleBo::getRoleId, roleIds)
                .list()
                .stream()
                .map(UserRoleBo::getUserId)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> getUserIds(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return getUserIds(List.of(roleId));
    }

    /** 删除角色相关绑定前保留受影响用户，随后逐用户发布事件，避免关系删除后无法反查缓存归属。 */
    @Transactional
    @Override
    public void removeByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        List<Long> userIds = getUserIds(roleId);
        lambdaUpdate()
                .eq(UserRoleBo::getRoleId, roleId)
                .remove();
        userIds.forEach(userId -> eventPublisher.publishEvent(AuthorizationEvent.of(
                AuthorizationEvent.Type.USER_ROLE,
                AuthorizationEvent.Action.REPLACE,
                userId)));
    }

    @Override
    public UserRoleBo getUserRole(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(UserRoleBo::getUserId, userId)
                .eq(UserRoleBo::getRoleId, roleId)
                .list()
                .stream()
                .findFirst()
                .orElse(null);
    }

    /** 按每项应用策略检查待移除角色，属于身份必需角色时在任何写入前拒绝。 */
    private void ensureRemovable(Long userId, Set<Long> roleIds) {
        boolean required = roleIds.stream().anyMatch(roleId -> requiredRolePolicies.stream()
                .anyMatch(policy -> policy.isRequired(userId, roleId)));
        Preconditions.checkCondition(!required, messages.get("rbac.assignment.requiredRole.remove"));
    }
}
