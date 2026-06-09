package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.gnilc.authz.rbac.entity.bo.RoleBo;
import com.gnilc.authz.rbac.entity.dto.RoleDto;
import com.gnilc.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.authz.rbac.exception.IllegalConditionException;
import com.gnilc.authz.rbac.exception.InvalidArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoleServiceImplTest {
    private ApplicationEventPublisher publisher;
    private RoleServiceImpl service;
    private LambdaQueryChainWrapper<RoleBo> query;

    /**
     * Sets up a fresh role service spy before each test.
     */
    @BeforeEach
    void setUp() {
        publisher = mock(ApplicationEventPublisher.class);
        service = spy(new RoleServiceImpl());
        query = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        doReturn(query).when(query).eq(ArgumentMatchers.<SFunction<RoleBo, ?>>any(), any());
        doReturn(query).when(service).lambdaQuery();
        ReflectionTestUtils.setField(service, "publisher", publisher);
    }

    /**
     * Verifies role creation persists code and defaults built-in to false.
     */
    @Test
    void createRoleUsesCodeAndDefaultsBuiltInToFalse() {
        RoleDto rd = new RoleDto();
        rd.setCode("admin");
        rd.setName("管理员");
        rd.setRemark("remark");
        when(query.one()).thenReturn(null);
        doReturn(true).when(service).save(any(RoleBo.class));

        service.createRole(rd);

        ArgumentCaptor<RoleBo> rbCaptor = ArgumentCaptor.forClass(RoleBo.class);
        verify(service).save(rbCaptor.capture());
        RoleBo rb = rbCaptor.getValue();
        assertThat(rb.getCode()).isEqualTo("admin");
        assertThat(rb.getBuiltIn()).isFalse();
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.ROLE);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.CREATE);
    }

    /**
     * Verifies duplicate role codes are rejected before saving.
     */
    @Test
    void rejectDuplicateCodeBeforeSavingRole() {
        RoleDto rd = new RoleDto();
        rd.setCode("admin");
        when(query.one()).thenReturn(new RoleBo());

        assertThatThrownBy(() -> service.createRole(rd))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("角色标识已存在");

        verify(service, never()).save(any(RoleBo.class));
        verifyNoInteractions(publisher);
    }

    /**
     * Verifies role updates persist mutable fields by ID.
     */
    @Test
    void updateRoleUpdatesMutableFieldsById() {
        RoleBo rb = new RoleBo();
        rb.setId(1L);
        rb.setCode("old-admin");
        rb.setBuiltIn(Boolean.FALSE);
        doReturn(rb).when(service).getById(1L);
        when(query.one()).thenReturn(null);
        doReturn(true).when(service).updateById(any(RoleBo.class));
        RoleDto rd = new RoleDto();
        rd.setId(1L);
        rd.setCode("admin");
        rd.setName("管理员");
        rd.setRemark("remark");

        service.updateRole(rd);

        ArgumentCaptor<RoleBo> rbCaptor = ArgumentCaptor.forClass(RoleBo.class);
        verify(service).updateById(rbCaptor.capture());
        assertThat(rbCaptor.getValue().getCode()).isEqualTo("admin");
        assertThat(rbCaptor.getValue().getName()).isEqualTo("管理员");
        assertThat(rbCaptor.getValue().getRemark()).isEqualTo("remark");
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.UPDATE);
        assertThat(event.getData()).isEqualTo(1L);
    }

    /**
     * Verifies built-in roles cannot be modified or removed.
     */
    @Test
    void rejectBuiltInRoleModificationAndRemoval() {
        RoleBo rb = new RoleBo();
        rb.setId(1L);
        rb.setBuiltIn(Boolean.TRUE);
        doReturn(rb).when(service).getById(1L);
        RoleDto rd = new RoleDto();
        rd.setId(1L);
        rd.setCode("admin");

        assertThatThrownBy(() -> service.updateRole(rd))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("内置角色不允许修改");
        assertThatThrownBy(() -> service.removeRole(1L))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("内置角色不允许删除");

        verifyNoInteractions(publisher);
    }

    private RbacAuthzEvent<?> publishedEvent() {
        ArgumentCaptor<RbacAuthzEvent> eventCaptor = ArgumentCaptor.forClass(RbacAuthzEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        return eventCaptor.getValue();
    }
}
