package com.gnilc.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.gnilc.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.authz.rbac.event.RbacAuthzEvent;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PermissionServiceImplTest {
    private ApplicationEventPublisher publisher;
    private PermissionServiceImpl service;
    private LambdaQueryChainWrapper<PermissionBo> query;

    /**
     * Sets up a fresh permission service spy before each test.
     */
    @BeforeEach
    void setUp() {
        publisher = mock(ApplicationEventPublisher.class);
        service = spy(new PermissionServiceImpl());
        query = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        doReturn(query).when(query).eq(ArgumentMatchers.<SFunction<PermissionBo, ?>>any(), any());
        doReturn(query).when(query).eq(anyBoolean(), ArgumentMatchers.<SFunction<PermissionBo, ?>>any(), any());
        doReturn(query).when(service).lambdaQuery();
        ReflectionTestUtils.setField(service, "publisher", publisher);
    }

    /**
     * Verifies permission creation persists target and public-access fields.
     */
    @Test
    void createPermissionUsesTargetAndPublicAccessFields() {
        PermissionDto pd = validPermission();
        when(query.one()).thenReturn(null);
        doReturn(true).when(service).save(any(PermissionBo.class));

        service.createPermission(pd);

        ArgumentCaptor<PermissionBo> pbCaptor = ArgumentCaptor.forClass(PermissionBo.class);
        verify(service).save(pbCaptor.capture());
        PermissionBo pb = pbCaptor.getValue();
        assertThat(pb.getCode()).isEqualTo("user:read");
        assertThat(pb.getTargetIdentifier()).isEqualTo("/users/**");
        assertThat(pb.getTargetQualifier()).isEqualTo("GET");
        assertThat(pb.getPublicAccess()).isTrue();
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.PERMISSION);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.CREATE);
    }

    /**
     * Verifies duplicate permission codes are rejected before saving.
     */
    @Test
    void rejectDuplicateCodeBeforeSavingPermission() {
        PermissionDto pd = validPermission();
        when(query.one()).thenReturn(new PermissionBo());

        assertThatThrownBy(() -> service.createPermission(pd))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("权限标识已存在");

        verify(service, never()).save(any(PermissionBo.class));
        verifyNoInteractions(publisher);
    }

    /**
     * Verifies permissions require a target identifier.
     */
    @Test
    void rejectMissingTargetIdentifier() {
        PermissionDto pd = validPermission();
        pd.setTargetIdentifier(null);

        assertThatThrownBy(() -> service.createPermission(pd))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请输入访问目标标识");

        verifyNoInteractions(publisher);
    }

    /**
     * Verifies permission updates persist qualifier and public-access fields.
     */
    @Test
    void updatePermissionPersistsTargetQualifierAndPublicAccess() {
        PermissionBo pb = new PermissionBo();
        pb.setId(10L);
        pb.setCode("user:old");
        doReturn(pb).when(service).getById(10L);
        when(query.one()).thenReturn(null);
        doReturn(true).when(service).updateById(any(PermissionBo.class));
        PermissionDto pd = validPermission();
        pd.setId(10L);
        pd.setPublicAccess(Boolean.FALSE);

        service.updatePermission(pd);

        ArgumentCaptor<PermissionBo> pbCaptor = ArgumentCaptor.forClass(PermissionBo.class);
        verify(service).updateById(pbCaptor.capture());
        PermissionBo updatedPb = pbCaptor.getValue();
        assertThat(updatedPb.getName()).isEqualTo("用户读取");
        assertThat(updatedPb.getCode()).isEqualTo("user:read");
        assertThat(updatedPb.getTargetIdentifier()).isEqualTo("/users/**");
        assertThat(updatedPb.getTargetQualifier()).isEqualTo("GET");
        assertThat(updatedPb.getPublicAccess()).isFalse();
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.UPDATE);
        assertThat(event.getData()).isEqualTo(10L);
    }

    private PermissionDto validPermission() {
        PermissionDto pd = new PermissionDto();
        pd.setCode("user:read");
        pd.setName("用户读取");
        pd.setTargetIdentifier("/users/**");
        pd.setTargetQualifier("GET");
        pd.setRemark("remark");
        pd.setPublicAccess(Boolean.TRUE);
        return pd;
    }

    private RbacAuthzEvent<?> publishedEvent() {
        ArgumentCaptor<RbacAuthzEvent> eventCaptor = ArgumentCaptor.forClass(RbacAuthzEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        return eventCaptor.getValue();
    }
}
