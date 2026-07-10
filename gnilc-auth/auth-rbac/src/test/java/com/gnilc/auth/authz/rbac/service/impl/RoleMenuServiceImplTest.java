package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.gnilc.auth.authz.rbac.entity.bo.RoleMenuBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleMenuDto;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

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

class RoleMenuServiceImplTest {
    private RoleMenuServiceImpl service;
    private LambdaQueryChainWrapper<RoleMenuBo> query;
    private LambdaUpdateChainWrapper<RoleMenuBo> update;

    /**
     * Sets up a fresh role-menu service spy before each test.
     */
    @BeforeEach
    void setUp() {
        service = spy(new RoleMenuServiceImpl());
        query = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        update = mock(LambdaUpdateChainWrapper.class, RETURNS_SELF);
        doReturn(query).when(service).lambdaQuery();
        doReturn(update).when(service).lambdaUpdate();
        doReturn(query).when(query).select(ArgumentMatchers.<SFunction<RoleMenuBo, ?>>any());
        doReturn(query).when(query).eq(ArgumentMatchers.<SFunction<RoleMenuBo, ?>>any(), ArgumentMatchers.any());
        doReturn(query).when(query).in(ArgumentMatchers.<SFunction<RoleMenuBo, ?>>any(), ArgumentMatchers.anyCollection());
        doReturn(update).when(update).eq(ArgumentMatchers.<SFunction<RoleMenuBo, ?>>any(), ArgumentMatchers.any());
        doReturn(update).when(update).in(ArgumentMatchers.<SFunction<RoleMenuBo, ?>>any(), ArgumentMatchers.anyCollection());
        doReturn(List.of()).when(query).list();
        doReturn(true).when(update).remove();
        doReturn(true).when(service).saveBatch(ArgumentMatchers.<RoleMenuBo>anyList());
    }

    /**
     * Verifies empty menu ID updates remove existing role-menu relations.
     */
    // TestCaseId: RBAC-SERVICE-021
    @Test
    void updateRoleMenuRemovesOnlyMissingRelationsWhenMenuIdsAreEmpty() {
        RoleMenuDto rmd = new RoleMenuDto();
        rmd.setRoleId(1L);
        rmd.setMenuIds(List.of());
        doReturn(List.of(rmb(1L, 10L), rmb(1L, 20L))).when(query).list();

        service.updateRoleMenu(rmd);

        verify(update).remove();
        verify(service, never()).saveBatch(ArgumentMatchers.<RoleMenuBo>anyList());
    }

    /**
     * Verifies role-menu updates save only new relations.
     */
    // TestCaseId: RBAC-SERVICE-039
    @Test
    void updateRoleMenuBatchSavesOnlyNewRelations() {
        RoleMenuDto rmd = new RoleMenuDto();
        rmd.setRoleId(1L);
        rmd.setMenuIds(List.of(10L, 20L, 20L));
        doReturn(List.of(rmb(1L, 10L))).when(query).list();

        service.updateRoleMenu(rmd);

        verify(update, never()).remove();
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Collection<RoleMenuBo>> rmbsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(service).saveBatch(rmbsCaptor.capture());
        List<RoleMenuBo> rmbs = rmbsCaptor.getValue().stream().toList();
        assertThat(rmbs).hasSize(1);
        assertThat(rmbs).extracting(RoleMenuBo::getRoleId).containsExactly(1L);
        assertThat(rmbs).extracting(RoleMenuBo::getMenuId).containsExactly(20L);
    }

    /**
     * Verifies unchanged role-menu updates skip writes.
     */
    // TestCaseId: RBAC-SERVICE-022
    @Test
    void updateRoleMenuSkipsWritesWhenRelationsDoNotChange() {
        RoleMenuDto rmd = new RoleMenuDto();
        rmd.setRoleId(1L);
        rmd.setMenuIds(List.of(10L, 20L));
        doReturn(List.of(rmb(1L, 10L), rmb(1L, 20L))).when(query).list();

        service.updateRoleMenu(rmd);

        verify(update, never()).remove();
        verify(service, never()).saveBatch(ArgumentMatchers.<RoleMenuBo>anyList());
    }

    /**
     * Verifies role-menu updates reject missing role IDs before writing.
     */
    // TestCaseId: RBAC-SERVICE-040
    @Test
    void rejectNullRoleIdBeforeWriting() {
        RoleMenuDto rmd = new RoleMenuDto();
        rmd.setMenuIds(List.of(10L));

        assertThatThrownBy(() -> service.updateRoleMenu(rmd))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("请选择角色");

        verify(update, never()).remove();
        verify(service, never()).saveBatch(ArgumentMatchers.<RoleMenuBo>anyList());
    }

    /**
     * Verifies menu ID query outputs are distinct even when duplicate rows exist.
     */
    // TestCaseId: RBAC-SERVICE-023
    @Test
    void getMenuIdsReturnsDistinctIds() {
        doReturn(List.of(rmb(1L, 10L), rmb(1L, 10L), rmb(1L, 20L))).when(query).list();

        assertThat(service.getMenuIds(1L)).containsExactly(10L, 20L);
        assertThat(service.getMenuIds(List.of(1L, 2L))).containsExactly(10L, 20L);
    }

    private RoleMenuBo rmb(Long roleId, Long menuId) {
        RoleMenuBo rmb = new RoleMenuBo();
        rmb.setRoleId(roleId);
        rmb.setMenuId(menuId);
        return rmb;
    }
}
