package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.authz.rbac.common.base.Preconditions;
import com.gnilc.authz.rbac.common.utils.PageResult;
import com.gnilc.authz.rbac.dao.RoleDao;
import com.gnilc.authz.rbac.entity.dto.RoleDto;
import com.gnilc.authz.rbac.entity.dto.RolePageDto;
import com.gnilc.authz.rbac.entity.dto.RoleQueryDto;
import com.gnilc.authz.rbac.entity.vo.RoleVo;
import com.gnilc.authz.rbac.service.UserRoleService;
import com.gnilc.authz.rbac.service.event.CrudEvent;
import com.gnilc.authz.rbac.service.event.RoleEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.gnilc.authz.rbac.entity.bo.RoleBo;
import com.gnilc.authz.rbac.service.RoleService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;


@Service("roleService")
public class RoleServiceImpl extends ServiceImpl<RoleDao, RoleBo> implements RoleService {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private UserRoleService userRoleService;

    @Override
    public PageResult<RoleVo> getRolePage(RolePageDto pd) {
        String symbol = pd.getSymbol();
        String name = pd.getName();
        LambdaQueryWrapper<RoleBo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(symbol), RoleBo::getSymbol, symbol);
        wrapper.like(StringUtils.isNotBlank(name), RoleBo::getName, name);
        IPage<RoleBo> page = page(pd.getPage(), wrapper);
        List<RoleVo> rvs = page.getRecords().stream().map(r -> {
            RoleVo rv = new RoleVo();
            BeanUtils.copyProperties(r, rv);
            return rv;
        }).toList();
        return PageResult.of(page, rvs);
    }

    @Override
    public List<RoleVo> getRoles(RoleQueryDto qd) {
        String symbol = qd.getSymbol();
        String name = qd.getName();
        Boolean internal = qd.getInternal();
        LambdaQueryWrapper<RoleBo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(symbol), RoleBo::getSymbol, symbol);
        wrapper.eq(internal != null, RoleBo::getInternal, internal);
        wrapper.like(StringUtils.isNotBlank(name), RoleBo::getName, name);
       return list(wrapper).stream().map(r -> {
            RoleVo rv = new RoleVo();
            BeanUtils.copyProperties(r, rv);
            return rv;
        }).toList();
    }

    @Transactional
    @Override
    public void saveRole(RoleDto roleDto) {
        String name = roleDto.getName();
        String symbol = roleDto.getSymbol();
        String remark = roleDto.getRemark();
        RoleBo sr = getRoleBySymbol(symbol);
        Preconditions.checkArgument(sr == null, "角色标识已存在");
        RoleBo role = new RoleBo();
        role.setName(name);
        role.setSymbol(symbol);
        role.setRemark(remark);
        save(role);
        publisher.publishEvent(new RoleEvent(this, CrudEvent.Event.CREATE, role.getId()));
    }

    @Override
    public RoleBo getRoleBySymbol(String symbol) {
        if (StringUtils.isNotBlank(symbol)) {
            return getOne(new LambdaQueryWrapper<RoleBo>()
                    .eq(RoleBo::getSymbol, symbol));
        }
        return null;
    }

    @Transactional
    @Override
    public void modifyRole(RoleDto roleDto) {
        Long id = roleDto.getId();
        String name = roleDto.getName();
        String symbol = roleDto.getSymbol();
        String remark = roleDto.getRemark();
        RoleBo role = getById(id);
        Preconditions.checkCondition(role != null, "请刷新后再试");
        Preconditions.checkCondition(!role.getInternal(), "内置角色不允许删除");
        if (StringUtils.isNotBlank(symbol) && !symbol.equals(role.getSymbol())) {
            RoleBo sr = getRoleBySymbol(symbol);
            Preconditions.checkArgument(sr == null, "角色标识已存在");
        }
        role.setName(name);
        role.setSymbol(symbol);
        role.setRemark(remark);
        updateById(role);
        publisher.publishEvent(new RoleEvent(this, CrudEvent.Event.UPDATE, id));
    }

    @Override
    public void removeRole(Long id) {
        RoleBo role = getById(id);
        Preconditions.checkCondition(role != null, "请刷新后再试");
        Preconditions.checkCondition(!role.getInternal(), "内置角色不允许删除");
        removeById(id);
        publisher.publishEvent(new RoleEvent(this, CrudEvent.Event.DELETE, id));
    }

    @Override
    public List<RoleBo> getRoles(Long userId) {
        Preconditions.checkArgument(userId != null, "userId == null");
        List<Long> roleIds = userRoleService.getRoleIds(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return new ArrayList<>();
        }
        return listByIds(roleIds);
    }


}