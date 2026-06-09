package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.common.base.Preconditions;
import com.gnilc.authz.rbac.common.utils.PageResult;
import com.gnilc.authz.rbac.dao.RoleDao;
import com.gnilc.authz.rbac.entity.dto.RoleDto;
import com.gnilc.authz.rbac.entity.dto.RolePageDto;
import com.gnilc.authz.rbac.entity.dto.RoleQueryDto;
import com.gnilc.authz.rbac.entity.vo.RoleVo;
import com.gnilc.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.authz.rbac.service.UserRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.gnilc.authz.rbac.entity.bo.RoleBo;
import com.gnilc.authz.rbac.service.RoleService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;


@Service("roleService")
public class RoleServiceImpl extends ServiceImpl<RoleDao, RoleBo> implements RoleService {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private UserRoleService userRoleService;

    @Override
    public PageResult<RoleVo> getRolePage(RolePageDto rd) {
        String code = rd.getCode();
        String name = rd.getName();
        IPage<RoleBo> page = lambdaQuery()
                .eq(StringUtils.isNotBlank(code), RoleBo::getCode, code)
                .like(StringUtils.isNotBlank(name), RoleBo::getName, name)
                .page(rd.getPage());
        List<RoleVo> rvs = page.getRecords().stream().map(rb -> {
            RoleVo rv = new RoleVo();
            BeanUtils.copyProperties(rb, rv);
            return rv;
        }).toList();
        return PageResult.of(page, rvs);
    }

    @Override
    public List<RoleVo> getRoles(RoleQueryDto rd) {
        String code = rd.getCode();
        String name = rd.getName();
        Boolean builtIn = rd.getBuiltIn();
        return lambdaQuery().eq(StringUtils.isNotBlank(code), RoleBo::getCode, code)
                .eq(builtIn != null, RoleBo::getBuiltIn, builtIn)
                .like(StringUtils.isNotBlank(name), RoleBo::getName, name)
                .list().stream().map(rb -> {
                    RoleVo rv = new RoleVo();
                    BeanUtils.copyProperties(rb, rv);
                    return rv;
                }).toList();
    }

    @Transactional
    @Override
    public void createRole(RoleDto rd) {
        Preconditions.checkArgument(rd != null, "请填写角色信息");
        String name = rd.getName();
        String code = rd.getCode();
        String remark = rd.getRemark();
        Preconditions.checkArgument(StringUtils.isNotBlank(code), "请输入角色标识");
        RoleBo rb = getRoleByCode(code);
        Preconditions.checkArgument(rb == null, "角色标识已存在");
        rb = new RoleBo();
        rb.setName(name);
        rb.setCode(code);
        rb.setRemark(remark);
        rb.setBuiltIn(Boolean.FALSE);
        save(rb);

        publisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE,
                RbacAuthzEvent.Action.CREATE,
                rb.getId()));
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
    public void updateRole(RoleDto rd) {
        Preconditions.checkArgument(rd != null, "请填写角色信息");
        Long id = rd.getId();
        String name = rd.getName();
        String code = rd.getCode();
        String remark = rd.getRemark();
        Preconditions.checkArgument(id != null, "请选择角色");
        RoleBo rb = getById(id);
        Preconditions.checkCondition(rb != null, "角色不存在，请刷新后重试");
        Preconditions.checkCondition(!Boolean.TRUE.equals(rb.getBuiltIn()), "内置角色不允许修改");
        if (StringUtils.isNotBlank(code) && !code.equals(rb.getCode())) {
            RoleBo sameRb = getRoleByCode(code);
            Preconditions.checkArgument(sameRb == null, "角色标识已存在");
        }
        rb.setName(name);
        rb.setCode(code);
        rb.setRemark(remark);
        updateById(rb);

        publisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE,
                RbacAuthzEvent.Action.UPDATE,
                id));
    }

    @Transactional
    @Override
    public void removeRole(Long id) {
        Preconditions.checkArgument(id != null, "请选择角色");
        RoleBo rb = getById(id);
        Preconditions.checkCondition(rb != null, "角色不存在，请刷新后重试");
        Preconditions.checkCondition(!Boolean.TRUE.equals(rb.getBuiltIn()), "内置角色不允许删除");
        removeById(id);

        publisher.publishEvent(RbacAuthzEvent.of(
                RbacAuthzEvent.Type.ROLE,
                RbacAuthzEvent.Action.DELETE,
                id));
    }

    @Override
    public List<RoleBo> getRoles(Long userId) {
        Preconditions.checkArgument(userId != null, "请选择用户");
        List<Long> roleIds = userRoleService.getRoleIds(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return listByIds(roleIds);
    }


}