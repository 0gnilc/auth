package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.gnilc.auth.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserRoleServiceImplTest {
    private ApplicationEventPublisher publisher;
    private UserRoleServiceImpl service;
    private LambdaQueryChainWrapper<UserRoleBo> query;
    private LambdaUpdateChainWrapper<UserRoleBo> update;

    /**
     * Sets up a fresh user-role service spy before each test.
     */
    @BeforeEach
    void setUp() {
        publisher = mock(ApplicationEventPublisher.class);
        service = spy(new UserRoleServiceImpl());
        query = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        update = mock(LambdaUpdateChainWrapper.class, RETURNS_SELF);
        doReturn(query).when(service).lambdaQuery();
        doReturn(update).when(service).lambdaUpdate();
        doReturn(query).when(query).select(ArgumentMatchers.<SFunction<UserRoleBo, ?>>any());
        doReturn(query).when(query).eq(ArgumentMatchers.<SFunction<UserRoleBo, ?>>any(), ArgumentMatchers.any());
        doReturn(query).when(query).in(ArgumentMatchers.<SFunction<UserRoleBo, ?>>any(), ArgumentMatchers.anyCollection());
        doReturn(update).when(update).eq(ArgumentMatchers.<SFunction<UserRoleBo, ?>>any(), ArgumentMatchers.any());
        doReturn(update).when(update).in(ArgumentMatchers.<SFunction<UserRoleBo, ?>>any(), ArgumentMatchers.anyCollection());
        doReturn(List.of()).when(query).list();
        doReturn(true).when(update).remove();
        doReturn(true).when(service).remove(ArgumentMatchers.<Wrapper<UserRoleBo>>any());
        doReturn(true).when(service).saveBatch(ArgumentMatchers.<UserRoleBo>anyList());
        doReturn(true).when(service).save(any(UserRoleBo.class));
        ReflectionTestUtils.setField(service, "publisher", publisher);
    }

    /**
     * Verifies empty role ID updates remove existing user-role relations.
     */
    // TestCaseId: RBAC-SERVICE-028
    @Test
    void updateUserRoleRemovesOnlyMissingRelationsWhenRoleIdsAreEmpty() {
        UserRoleDto urd = new UserRoleDto();
        urd.setUserId(100L);
        urd.setRoleIds(List.of());
        doReturn(List.of(urb(100L, 1L), urb(100L, 2L))).when(query).list();

        service.updateUserRole(urd);

        verify(update).remove();
        verify(service, never()).saveBatch(ArgumentMatchers.<UserRoleBo>anyList());
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.USER_ROLE);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.REPLACE);
        assertThat(event.getData()).isEqualTo(100L);
        assertThat(event.getExtra()).isNull();
    }

    /**
     * Verifies user-role updates save only new relations.
     */
    // TestCaseId: RBAC-SERVICE-029
    @Test
    void updateUserRoleBatchSavesOnlyNewRelations() {
        UserRoleDto urd = new UserRoleDto();
        urd.setUserId(100L);
        urd.setRoleIds(List.of(1L, 2L, 2L));
        doReturn(List.of(urb(100L, 1L))).when(query).list();

        service.updateUserRole(urd);

        verify(update, never()).remove();
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Collection<UserRoleBo>> urbsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(service).saveBatch(urbsCaptor.capture());
        List<UserRoleBo> urbs = urbsCaptor.getValue().stream().toList();
        assertThat(urbs).hasSize(1);
        assertThat(urbs).extracting(UserRoleBo::getUserId).containsExactly(100L);
        assertThat(urbs).extracting(UserRoleBo::getRoleId).containsExactly(2L);
    }

    /**
     * Verifies unchanged user-role updates skip writes.
     */
    // TestCaseId: RBAC-SERVICE-030
    @Test
    void updateUserRoleSkipsWritesWhenRelationsDoNotChange() {
        UserRoleDto urd = new UserRoleDto();
        urd.setUserId(100L);
        urd.setRoleIds(List.of(1L, 2L));
        doReturn(List.of(urb(100L, 1L), urb(100L, 2L))).when(query).list();

        service.updateUserRole(urd);

        verify(update, never()).remove();
        verify(service, never()).saveBatch(ArgumentMatchers.<UserRoleBo>anyList());
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.USER_ROLE);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.REPLACE);
        assertThat(event.getData()).isEqualTo(100L);
        assertThat(event.getExtra()).isNull();
    }

    /**
     * Verifies bindRole rejects null IDs before publishing.
     */
    // TestCaseId: RBAC-SERVICE-031
    @Test
    void bindRoleRejectsNullIdsBeforePublishingEvent() {
        assertThatThrownBy(() -> service.bindRole(null, 1L))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请选择用户");
        assertThatThrownBy(() -> service.bindRole(100L, null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请选择角色");

        verifyNoInteractions(publisher);
    }

    /**
     * Verifies bindRole saves the relation and publishes replacement.
     */
    // TestCaseId: RBAC-SERVICE-041
    @Test
    void bindRoleSavesRelationAndPublishesReplacement() {
        service.bindRole(100L, 1L);

        ArgumentCaptor<UserRoleBo> urbCaptor = ArgumentCaptor.forClass(UserRoleBo.class);
        verify(service).save(urbCaptor.capture());
        UserRoleBo urb = urbCaptor.getValue();
        assertThat(urb.getUserId()).isEqualTo(100L);
        assertThat(urb.getRoleId()).isEqualTo(1L);
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.USER_ROLE);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.REPLACE);
        assertThat(event.getData()).isEqualTo(100L);
        assertThat(event.getExtra()).isNull();
    }

    /**
     * Verifies unbindRole rejects null IDs before publishing.
     */
    // TestCaseId: RBAC-SERVICE-032
    @Test
    void unbindRoleRejectsNullIdsBeforePublishingEvent() {
        assertThatThrownBy(() -> service.unbindRole(null, 1L))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请选择用户");
        assertThatThrownBy(() -> service.unbindRole(100L, null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请选择角色");

        verifyNoInteractions(publisher);
    }

    /**
     * Verifies unbindRole removes the relation and publishes replacement.
     */
    // TestCaseId: RBAC-SERVICE-042
    @Test
    void unbindRoleRemovesRelationAndPublishesReplacement() {
        service.unbindRole(100L, 1L);

        verify(service).remove(ArgumentMatchers.<Wrapper<UserRoleBo>>any());
        RbacAuthzEvent<?> event = publishedEvent();
        assertThat(event.getType()).isEqualTo(RbacAuthzEvent.Type.USER_ROLE);
        assertThat(event.getAction()).isEqualTo(RbacAuthzEvent.Action.REPLACE);
        assertThat(event.getData()).isEqualTo(100L);
        assertThat(event.getExtra()).isNull();
    }

    /**
     * Verifies relation ID query outputs are distinct even when duplicate rows exist.
     */
    // TestCaseId: RBAC-SERVICE-033
    @Test
    void relationIdQueriesReturnDistinctIds() {
        doReturn(List.of(urb(100L, 1L), urb(100L, 1L), urb(100L, 2L))).when(query).list();

        assertThat(service.getRoleIds(100L)).containsExactly(1L, 2L);

        doReturn(List.of(urb(100L, 1L), urb(100L, 1L), urb(200L, 1L))).when(query).list();

        assertThat(service.getUserIds(1L)).containsExactly(100L, 200L);
    }

    /**
     * Verifies getUserRole tolerates repeated relation rows.
     */
    // TestCaseId: RBAC-SERVICE-043
    @Test
    void getUserRoleReturnsFirstRepeatedRow() {
        UserRoleBo first = urb(100L, 1L);
        doReturn(List.of(first, urb(100L, 1L))).when(query).list();

        assertThat(service.getUserRole(100L, 1L)).isSameAs(first);
    }

    private UserRoleBo urb(Long userId, Long roleId) {
        UserRoleBo urb = new UserRoleBo();
        urb.setUserId(userId);
        urb.setRoleId(roleId);
        return urb;
    }

    private RbacAuthzEvent<?> publishedEvent() {
        ArgumentCaptor<RbacAuthzEvent> eventCaptor = ArgumentCaptor.forClass(RbacAuthzEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        return eventCaptor.getValue();
    }
}
