package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.gnilc.auth.authz.rbac.entity.bo.RolePermissionBo;
import com.gnilc.auth.authz.rbac.entity.dto.RolePermissionDto;
import com.gnilc.auth.authz.rbac.event.RbacAuthzEvent;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RolePermissionServiceImplTest {
    private ApplicationEventPublisher publisher;
    private RolePermissionServiceImpl service;
    private LambdaQueryChainWrapper<RolePermissionBo> query;
    private LambdaUpdateChainWrapper<RolePermissionBo> update;

    /**
     * Sets up a fresh role-permission service spy before each test.
     */
    @BeforeEach
    void setUp() {
        publisher = mock(ApplicationEventPublisher.class);
        service = spy(new RolePermissionServiceImpl());
        query = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        update = mock(LambdaUpdateChainWrapper.class, RETURNS_SELF);
        doReturn(query).when(service).lambdaQuery();
        doReturn(update).when(service).lambdaUpdate();
        doReturn(query).when(query).select(ArgumentMatchers.<SFunction<RolePermissionBo, ?>>any());
        doReturn(query).when(query).eq(ArgumentMatchers.<SFunction<RolePermissionBo, ?>>any(), ArgumentMatchers.any());
        doReturn(query).when(query).in(ArgumentMatchers.<SFunction<RolePermissionBo, ?>>any(), ArgumentMatchers.anyCollection());
        doReturn(update).when(update).eq(ArgumentMatchers.<SFunction<RolePermissionBo, ?>>any(), ArgumentMatchers.any());
        doReturn(update).when(update).in(ArgumentMatchers.<SFunction<RolePermissionBo, ?>>any(), ArgumentMatchers.anyCollection());
        doReturn(List.of()).when(query).list();
        doReturn(true).when(update).remove();
        doReturn(true).when(service).saveBatch(ArgumentMatchers.<RolePermissionBo>anyList());
        ReflectionTestUtils.setField(service, "publisher", publisher);
    }

    /**
     * Verifies empty permission ID updates remove existing role-permission relations.
     */
    // TestCaseId: RBAC-SERVICE-005
    @Test
    void updateRolePermissionRemovesOnlyMissingRelationsWhenPermissionIdsAreEmpty() {
        RolePermissionDto rpd = new RolePermissionDto();
        rpd.setRoleId(1L);
        rpd.setPermissionIds(List.of());
        doReturn(List.of(rpb(1L, 10L), rpb(1L, 20L))).when(query).list();

        service.updateRolePermission(rpd);

        verify(update).remove();
        verify(service, never()).saveBatch(ArgumentMatchers.<RolePermissionBo>anyList());
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.ROLE_PERMISSION);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.REPLACE);
        assertThat(event.getData()).isEqualTo(1L);
        assertThat(event.getExtra()).isNull();
    }

    /**
     * Verifies role-permission updates save only new relations.
     */
    // TestCaseId: RBAC-SERVICE-006
    @Test
    void updateRolePermissionBatchSavesOnlyNewRelations() {
        RolePermissionDto rpd = new RolePermissionDto();
        rpd.setRoleId(1L);
        rpd.setPermissionIds(List.of(10L, 20L, 20L));
        doReturn(List.of(rpb(1L, 10L))).when(query).list();

        service.updateRolePermission(rpd);

        verify(update, never()).remove();
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Collection<RolePermissionBo>> rpbsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(service).saveBatch(rpbsCaptor.capture());
        List<RolePermissionBo> rpbs = rpbsCaptor.getValue().stream().toList();
        assertThat(rpbs).hasSize(1);
        assertThat(rpbs).extracting(RolePermissionBo::getRoleId).containsExactly(1L);
        assertThat(rpbs).extracting(RolePermissionBo::getPermissionId).containsExactly(20L);
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.ROLE_PERMISSION);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.REPLACE);
        assertThat(event.getData()).isEqualTo(1L);
        assertThat(event.getExtra()).isNull();
    }

    /**
     * Verifies unchanged role-permission updates skip writes.
     */
    // TestCaseId: RBAC-SERVICE-007
    @Test
    void updateRolePermissionSkipsWritesWhenRelationsDoNotChange() {
        RolePermissionDto rpd = new RolePermissionDto();
        rpd.setRoleId(1L);
        rpd.setPermissionIds(List.of(10L, 20L));
        doReturn(List.of(rpb(1L, 10L), rpb(1L, 20L))).when(query).list();

        service.updateRolePermission(rpd);

        verify(update, never()).remove();
        verify(service, never()).saveBatch(ArgumentMatchers.<RolePermissionBo>anyList());
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.ROLE_PERMISSION);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.REPLACE);
        assertThat(event.getData()).isEqualTo(1L);
        assertThat(event.getExtra()).isNull();
    }

    /**
     * Verifies role-permission updates reject missing role IDs before writing or publishing.
     */
    // TestCaseId: RBAC-SERVICE-008
    @Test
    void rejectNullRoleIdBeforeWritingOrPublishingEvent() {
        RolePermissionDto rpd = new RolePermissionDto();
        rpd.setPermissionIds(List.of(10L));

        assertThatThrownBy(() -> service.updateRolePermission(rpd))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请选择角色");

        verify(update, never()).remove();
        verify(service, never()).saveBatch(ArgumentMatchers.<RolePermissionBo>anyList());
        verifyNoInteractions(publisher);
    }

    /**
     * Verifies relation ID query outputs are distinct even when duplicate rows exist.
     */
    // TestCaseId: RBAC-SERVICE-009
    @Test
    void relationIdQueriesReturnDistinctIds() {
        doReturn(List.of(rpb(1L, 10L), rpb(1L, 10L), rpb(1L, 20L))).when(query).list();

        assertThat(service.getPermissionIds(1L)).containsExactly(10L, 20L);
        assertThat(service.getPermissionIds(List.of(1L, 2L))).containsExactly(10L, 20L);

        doReturn(List.of(rpb(1L, 10L), rpb(1L, 10L), rpb(2L, 10L))).when(query).list();

        assertThat(service.getRoleIds(10L)).containsExactly(1L, 2L);
    }

    private RolePermissionBo rpb(Long roleId, Long permissionId) {
        RolePermissionBo rpb = new RolePermissionBo();
        rpb.setRoleId(roleId);
        rpb.setPermissionId(permissionId);
        return rpb;
    }

    private RbacAuthzEvent<?> publishedEvent() {
        ArgumentCaptor<RbacAuthzEvent> eventCaptor = ArgumentCaptor.forClass(RbacAuthzEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        return eventCaptor.getValue();
    }
}
