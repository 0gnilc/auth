package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.utils.BeanPropertyUtils;
import com.gnilc.auth.authz.rbac.dao.PermissionDao;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.PermissionVo;
import com.gnilc.auth.authz.rbac.event.AuthorizationEvent;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.List;

@Service("permissionService")
public class PermissionServiceImpl extends ServiceImpl<PermissionDao, PermissionBo> implements PermissionService {
    private final ApplicationEventPublisher eventPublisher;
    private final UserRoleService userRoleService;
    private final RolePermissionService rolePermissionService;
    private final I18nMessageService messages;

    public PermissionServiceImpl(ApplicationEventPublisher eventPublisher,
                                 UserRoleService userRoleService,
                                 RolePermissionService rolePermissionService,
                                 I18nMessageService messages) {
        this.eventPublisher = eventPublisher;
        this.userRoleService = userRoleService;
        this.rolePermissionService = rolePermissionService;
        this.messages = messages;
    }

    @Transactional
    @Override
    public void createPermission(PermissionDto dto) {
        validatePermission(dto, false);
        String name = dto.getName();
        String code = dto.getCode();
        String targetIdentifier = dto.getTargetIdentifier();
        String targetQualifier = dto.getTargetQualifier();
        String remark = dto.getRemark();
        Boolean publicAccess = dto.getPublicAccess();
        PermissionBo bo = new PermissionBo();
        bo.setName(name);
        bo.setCode(code);
        bo.setTargetIdentifier(targetIdentifier);
        bo.setTargetQualifier(targetQualifier);
        bo.setRemark(remark);
        bo.setPublicAccess(publicAccess);
        bo.setBuiltIn(Boolean.FALSE);
        save(bo);

        eventPublisher.publishEvent(AuthorizationEvent.of(
                AuthorizationEvent.Type.PERMISSION,
                AuthorizationEvent.Action.CREATE,
                bo.getId()));
    }


    @Transactional
    @Override
    public void updatePermission(PermissionDto dto) {
        PermissionBo bo = validatePermission(dto, true);
        Long permissionId = dto.getId();
        String name = dto.getName();
        String code = dto.getCode();
        String targetIdentifier = dto.getTargetIdentifier();
        String targetQualifier = dto.getTargetQualifier();
        String remark = dto.getRemark();
        Boolean publicAccess = dto.getPublicAccess();
        bo.setName(name);
        bo.setCode(code);
        bo.setTargetIdentifier(targetIdentifier);
        bo.setTargetQualifier(targetQualifier);
        bo.setRemark(remark);
        bo.setPublicAccess(Boolean.TRUE.equals(publicAccess));
        updateById(bo);

        eventPublisher.publishEvent(AuthorizationEvent.of(
                AuthorizationEvent.Type.PERMISSION,
                AuthorizationEvent.Action.UPDATE, permissionId));
    }

    @Transactional
    @Override
    public void removePermission(Long id) {
        Preconditions.checkArgument(id != null, messages.get("rbac.permission.selection.required"));
        PermissionBo bo = getById(id);
        Preconditions.checkCondition(bo != null, messages.get("rbac.permission.notFound"));
        Preconditions.checkCondition(!Boolean.TRUE.equals(bo.getBuiltIn()),
                messages.get("rbac.permission.builtIn.delete"));
        bo.setCode(bo.getCode() + "_del_" + id);
        updateById(bo);
        rolePermissionService.removeByPermissionId(id);
        removeById(id);

        eventPublisher.publishEvent(AuthorizationEvent.of(
                AuthorizationEvent.Type.PERMISSION,
                AuthorizationEvent.Action.DELETE, id));
    }

    @Override
    public List<PermissionVo> getPermissions(PermissionQueryDto dto) {
        String code = dto.getCode();
        String name = dto.getName();
        String targetIdentifier = dto.getTargetIdentifier();
        String targetQualifier = dto.getTargetQualifier();
        Boolean publicAccess = dto.getPublicAccess();
        return lambdaQuery()
                .eq(StringUtils.isNotBlank(code), PermissionBo::getCode, code)
                .like(StringUtils.isNotBlank(name), PermissionBo::getName, name)
                .like(StringUtils.isNotBlank(targetIdentifier), PermissionBo::getTargetIdentifier, targetIdentifier)
                .eq(StringUtils.isNotBlank(targetQualifier), PermissionBo::getTargetQualifier, targetQualifier)
                .eq(publicAccess != null, PermissionBo::getPublicAccess, publicAccess)
                .list()
                .stream()
                .map(bo -> {
                    PermissionVo vo = new PermissionVo();
                    BeanUtils.copyProperties(bo, vo);
                    return vo;
                })
                .toList();
    }

    @Override
    public PermissionBo getPermissionByCode(String code) {
        if (StringUtils.isNotBlank(code)) {
            return lambdaQuery()
                    .eq(PermissionBo::getCode, code)
                    .one();
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
        Preconditions.checkArgument(ids != null, "At least one permission must be selected.");
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return listByIds(new LinkedHashSet<>(ids));
    }

    private PermissionBo validatePermission(PermissionDto dto, boolean update) {
        Preconditions.checkArgument(dto != null, messages.get("rbac.permission.information.required"));
        BeanPropertyUtils.trimToNull(dto);
        PermissionBo permission = null;
        if (update) {
            Preconditions.checkArgument(dto.getId() != null,
                    messages.get("rbac.permission.selection.required"));
            permission = getById(dto.getId());
            Preconditions.checkCondition(permission != null, messages.get("rbac.permission.notFound"));
            Preconditions.checkCondition(!Boolean.TRUE.equals(permission.getBuiltIn()),
                    messages.get("rbac.permission.builtIn.modify"));
        }
        String code = dto.getCode();
        String name = dto.getName();
        String targetIdentifier = dto.getTargetIdentifier();
        String targetQualifier = dto.getTargetQualifier();
        String remark = dto.getRemark();
        Preconditions.checkArgument(StringUtils.isNotBlank(name), messages.get("rbac.permission.name.required"));
        Preconditions.checkArgument(StringUtils.isNotBlank(code), messages.get("rbac.permission.code.required"));
        Preconditions.checkArgument(StringUtils.isNotBlank(targetIdentifier),
                messages.get("rbac.permission.targetIdentifier.required"));
        Preconditions.checkArgument(code.codePointCount(0, code.length()) <= 255,
                messages.get("rbac.permission.code.tooLong", 255));
        Preconditions.checkArgument(name.codePointCount(0, name.length()) <= 255,
                messages.get("rbac.permission.name.tooLong", 255));
        Preconditions.checkArgument(targetIdentifier.codePointCount(0, targetIdentifier.length()) <= 500,
                messages.get("rbac.permission.targetIdentifier.tooLong", 500));
        Preconditions.checkArgument(targetQualifier == null
                        || targetQualifier.codePointCount(0, targetQualifier.length()) <= 100,
                messages.get("rbac.permission.targetQualifier.tooLong", 100));
        Preconditions.checkArgument(remark == null || remark.codePointCount(0, remark.length()) <= 500,
                messages.get("rbac.permission.remark.tooLong", 500));
        if (!update || !code.equals(permission.getCode())) {
            Preconditions.checkArgument(getPermissionByCode(code) == null,
                    messages.get("rbac.permission.code.exists"));
        }
        return permission;
    }
}
