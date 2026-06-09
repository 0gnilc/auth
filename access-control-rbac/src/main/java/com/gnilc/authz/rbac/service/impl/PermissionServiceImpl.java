package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.common.base.Preconditions;
import com.gnilc.authz.rbac.dao.PermissionDao;
import com.gnilc.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.authz.rbac.entity.dto.PermissionQueryDto;
import com.gnilc.authz.rbac.entity.vo.PermissionVo;
import com.gnilc.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.authz.rbac.service.RolePermissionService;
import com.gnilc.authz.rbac.service.UserRoleService;
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
    public void createPermission(PermissionDto pd) {
        Preconditions.checkArgument(pd != null, "请填写权限信息");
        String name = pd.getName();
        String code = pd.getCode();
        String targetIdentifier = pd.getTargetIdentifier();
        String targetQualifier = pd.getTargetQualifier();
        String remark = pd.getRemark();
        Boolean publicAccess = pd.getPublicAccess();
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "请输入权限名称");
        Preconditions.checkArgument(StringUtils.isNotBlank(code), "请输入权限标识");
        Preconditions.checkArgument(StringUtils.isNotBlank(targetIdentifier), "请输入访问目标标识");
        PermissionBo pb = getPermissionByCode(code);
        Preconditions.checkArgument(pb == null, "权限标识已存在");
        pb = new PermissionBo();
        pb.setName(name);
        pb.setCode(code);
        pb.setTargetIdentifier(targetIdentifier);
        pb.setTargetQualifier(targetQualifier);
        pb.setRemark(remark);
        pb.setPublicAccess(publicAccess);
        save(pb);

        publisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.PERMISSION,
                RbacAuthzEvent.Action.CREATE,
                pb.getId()));
    }


    @Transactional
    @Override
    public void updatePermission(PermissionDto pd) {
        Preconditions.checkArgument(pd != null, "请填写权限信息");
        Long id = pd.getId();
        String name = pd.getName();
        String code = pd.getCode();
        String targetIdentifier = pd.getTargetIdentifier();
        String targetQualifier = pd.getTargetQualifier();
        String remark = pd.getRemark();
        Boolean publicAccess = pd.getPublicAccess();
        Preconditions.checkArgument(id != null, "请选择权限");
        PermissionBo pb = getById(id);
        Preconditions.checkCondition(pb != null, "权限不存在，请刷新后重试");
        if (StringUtils.isNotBlank(code) && !code.equals(pb.getCode())) {
            PermissionBo samePb = getPermissionByCode(code);
            Preconditions.checkArgument(samePb == null, "权限标识已存在");
        }
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "请输入权限名称");
        Preconditions.checkArgument(StringUtils.isNotBlank(code), "请输入权限标识");
        Preconditions.checkArgument(StringUtils.isNotBlank(targetIdentifier), "请输入访问目标标识");
        pb.setName(name);
        pb.setCode(code);
        pb.setTargetIdentifier(targetIdentifier);
        pb.setTargetQualifier(targetQualifier);
        pb.setRemark(remark);
        pb.setPublicAccess(Boolean.TRUE.equals(publicAccess));
        updateById(pb);

        publisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.PERMISSION,
                RbacAuthzEvent.Action.UPDATE, id));
    }

    @Transactional
    @Override
    public void removePermission(Long id) {
        Preconditions.checkArgument(id != null, "请选择权限");
        PermissionBo pb = getById(id);
        Preconditions.checkCondition(pb != null, "权限不存在，请刷新后重试");
        removeById(id);

        publisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.PERMISSION,
                RbacAuthzEvent.Action.DELETE, id));
    }

    @Override
    public List<PermissionVo> getPermissions(PermissionQueryDto pd) {
        String code = pd.getCode();
        String name = pd.getName();
        String targetIdentifier = pd.getTargetIdentifier();
        String targetQualifier = pd.getTargetQualifier();
        Boolean publicAccess = pd.getPublicAccess();
        return lambdaQuery()
                .eq(StringUtils.isNotBlank(code), PermissionBo::getCode, code)
                .like(StringUtils.isNotBlank(name), PermissionBo::getName, name)
                .like(StringUtils.isNotBlank(targetIdentifier), PermissionBo::getTargetIdentifier, targetIdentifier)
                .eq(StringUtils.isNotBlank(targetQualifier), PermissionBo::getTargetQualifier, targetQualifier)
                .eq(publicAccess != null, PermissionBo::getPublicAccess, publicAccess)
                .list().stream().map(pb -> {
                    PermissionVo pv = new PermissionVo();
                    BeanUtils.copyProperties(pb, pv);
                    return pv;
                }).toList();
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
        Preconditions.checkArgument(ids != null, "请选择权限");
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return listByIds(ids);
    }
}