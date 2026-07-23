package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.utils.PageResult;
import com.gnilc.auth.authz.rbac.dao.RoleDao;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.entity.dto.RolePageDto;
import com.gnilc.auth.authz.rbac.entity.dto.RoleQueryDto;
import com.gnilc.auth.authz.rbac.entity.vo.RoleVo;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;


@Service("roleService")
public class RoleServiceImpl extends ServiceImpl<RoleDao, RoleBo> implements RoleService {

    private final ApplicationEventPublisher eventPublisher;
    private final UserRoleService userRoleService;
    private final I18nMessageService messages;

    public RoleServiceImpl(ApplicationEventPublisher eventPublisher,
                           UserRoleService userRoleService,
                           I18nMessageService messages) {
        this.eventPublisher = eventPublisher;
        this.userRoleService = userRoleService;
        this.messages = messages;
    }

    @Override
    public PageResult<RoleVo> getRolePage(RolePageDto dto) {
        String code = dto.getCode();
        String name = dto.getName();
        IPage<RoleBo> page = lambdaQuery()
                .eq(StringUtils.isNotBlank(code), RoleBo::getCode, code)
                .like(StringUtils.isNotBlank(name), RoleBo::getName, name)
                .page(dto.getPage());
        List<RoleVo> vos = page.getRecords().stream()
                .map(this::toRoleVo)
                .toList();
        return PageResult.of(page, vos);
    }

    @Override
    public List<RoleVo> getRoles(RoleQueryDto dto) {
        String code = dto.getCode();
        String name = dto.getName();
        Boolean builtIn = dto.getBuiltIn();
        return lambdaQuery().eq(StringUtils.isNotBlank(code), RoleBo::getCode, code)
                .eq(builtIn != null, RoleBo::getBuiltIn, builtIn)
                .like(StringUtils.isNotBlank(name), RoleBo::getName, name)
                .list()
                .stream()
                .map(this::toRoleVo)
                .toList();
    }

    @Transactional
    @Override
    public void createRole(RoleDto dto) {
        Preconditions.checkArgument(dto != null, messages.get("rbac.role.information.required"));
        String name = dto.getName();
        String code = dto.getCode();
        String remark = dto.getRemark();
        Preconditions.checkArgument(StringUtils.isNotBlank(code), messages.get("rbac.role.code.required"));
        Preconditions.checkArgument(getRoleByCode(code) == null, messages.get("rbac.role.code.exists"));
        RoleBo bo = new RoleBo();
        bo.setName(name);
        bo.setCode(code);
        bo.setRemark(remark);
        bo.setBuiltIn(Boolean.FALSE);
        save(bo);

        eventPublisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE,
                RbacAuthzEvent.Action.CREATE,
                bo.getId()));
    }

    @Override
    public RoleBo getRoleByCode(String code) {
        if (StringUtils.isNotBlank(code)) {
            return lambdaQuery()
                    .eq(RoleBo::getCode, code)
                    .one();
        }
        return null;
    }

    @Transactional
    @Override
    public void updateRole(RoleDto dto) {
        Preconditions.checkArgument(dto != null, messages.get("rbac.role.information.required"));
        Long roleId = dto.getId();
        String name = dto.getName();
        String code = dto.getCode();
        String remark = dto.getRemark();
        Preconditions.checkArgument(roleId != null, messages.get("rbac.role.selection.required"));
        RoleBo bo = getById(roleId);
        Preconditions.checkCondition(bo != null, messages.get("rbac.role.notFound"));
        Preconditions.checkCondition(!Boolean.TRUE.equals(bo.getBuiltIn()), messages.get("rbac.role.builtIn.modify"));
        if (StringUtils.isNotBlank(code) && !code.equals(bo.getCode())) {
            RoleBo sameBo = getRoleByCode(code);
            Preconditions.checkArgument(sameBo == null, messages.get("rbac.role.code.exists"));
        }
        bo.setName(name);
        bo.setCode(code);
        bo.setRemark(remark);
        updateById(bo);

        eventPublisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE,
                RbacAuthzEvent.Action.UPDATE,
                roleId));
    }

    @Transactional
    @Override
    public void removeRole(Long id) {
        Preconditions.checkArgument(id != null, messages.get("rbac.role.selection.required"));
        RoleBo bo = getById(id);
        Preconditions.checkCondition(bo != null, messages.get("rbac.role.notFound"));
        Preconditions.checkCondition(!Boolean.TRUE.equals(bo.getBuiltIn()), messages.get("rbac.role.builtIn.delete"));
        bo.setCode(deletedCode(bo.getCode(), id));
        updateById(bo);
        removeById(id);

        eventPublisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE,
                RbacAuthzEvent.Action.DELETE,
                id));
    }

    @Override
    public List<RoleBo> getRoles(Long userId) {
        Preconditions.checkArgument(userId != null, "A user must be selected.");
        List<Long> roleIds = userRoleService.getRoleIds(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return listByIds(roleIds);
    }

    private String deletedCode(String code, Long id) {
        return code + "_del_" + id;
    }

    private RoleVo toRoleVo(RoleBo bo) {
        RoleVo vo = new RoleVo();
        BeanUtils.copyProperties(bo, vo);
        return vo;
    }
}
