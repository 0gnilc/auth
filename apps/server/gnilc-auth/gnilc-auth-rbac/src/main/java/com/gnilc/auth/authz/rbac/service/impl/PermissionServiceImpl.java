package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.auth.authz.rbac.dao.PermissionDao;
import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.PermissionVo;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
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
        Preconditions.checkArgument(dto != null, messages.get("rbac.permission.information.required"));
        String name = dto.getName();
        String code = dto.getCode();
        String targetIdentifier = dto.getTargetIdentifier();
        String targetQualifier = dto.getTargetQualifier();
        String remark = dto.getRemark();
        Boolean publicAccess = dto.getPublicAccess();
        Preconditions.checkArgument(StringUtils.isNotBlank(name), messages.get("rbac.permission.name.required"));
        Preconditions.checkArgument(StringUtils.isNotBlank(code), messages.get("rbac.permission.code.required"));
        Preconditions.checkArgument(StringUtils.isNotBlank(targetIdentifier),
                messages.get("rbac.permission.targetIdentifier.required"));
        Preconditions.checkArgument(getPermissionByCode(code) == null,
                messages.get("rbac.permission.code.exists"));
        PermissionBo bo = new PermissionBo();
        bo.setName(name);
        bo.setCode(code);
        bo.setTargetIdentifier(targetIdentifier);
        bo.setTargetQualifier(targetQualifier);
        bo.setRemark(remark);
        bo.setPublicAccess(publicAccess);
        save(bo);

        eventPublisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.PERMISSION,
                RbacAuthzEvent.Action.CREATE,
                bo.getId()));
    }


    @Transactional
    @Override
    public void updatePermission(PermissionDto dto) {
        Preconditions.checkArgument(dto != null, messages.get("rbac.permission.information.required"));
        Long permissionId = dto.getId();
        String name = dto.getName();
        String code = dto.getCode();
        String targetIdentifier = dto.getTargetIdentifier();
        String targetQualifier = dto.getTargetQualifier();
        String remark = dto.getRemark();
        Boolean publicAccess = dto.getPublicAccess();
        Preconditions.checkArgument(permissionId != null, messages.get("rbac.permission.selection.required"));
        PermissionBo bo = getById(permissionId);
        Preconditions.checkCondition(bo != null, messages.get("rbac.permission.notFound"));
        if (StringUtils.isNotBlank(code) && !code.equals(bo.getCode())) {
            PermissionBo sameBo = getPermissionByCode(code);
            Preconditions.checkArgument(sameBo == null, messages.get("rbac.permission.code.exists"));
        }
        Preconditions.checkArgument(StringUtils.isNotBlank(name), messages.get("rbac.permission.name.required"));
        Preconditions.checkArgument(StringUtils.isNotBlank(code), messages.get("rbac.permission.code.required"));
        Preconditions.checkArgument(StringUtils.isNotBlank(targetIdentifier),
                messages.get("rbac.permission.targetIdentifier.required"));
        bo.setName(name);
        bo.setCode(code);
        bo.setTargetIdentifier(targetIdentifier);
        bo.setTargetQualifier(targetQualifier);
        bo.setRemark(remark);
        bo.setPublicAccess(Boolean.TRUE.equals(publicAccess));
        updateById(bo);

        eventPublisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.PERMISSION,
                RbacAuthzEvent.Action.UPDATE, permissionId));
    }

    @Transactional
    @Override
    public void removePermission(Long id) {
        Preconditions.checkArgument(id != null, messages.get("rbac.permission.selection.required"));
        PermissionBo bo = getById(id);
        Preconditions.checkCondition(bo != null, messages.get("rbac.permission.notFound"));
        bo.setCode(bo.getCode() + "_del_" + id);
        updateById(bo);
        removeById(id);

        eventPublisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.PERMISSION,
                RbacAuthzEvent.Action.DELETE, id));
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
                .map(this::toPermissionVo)
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

    private PermissionVo toPermissionVo(PermissionBo bo) {
        PermissionVo vo = new PermissionVo();
        BeanUtils.copyProperties(bo, vo);
        return vo;
    }
}
