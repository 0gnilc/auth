<script setup lang="ts">
import type { ChecklistDrawerData } from '../components/checklist-drawer.vue';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { RoleApi } from '#/api';

import { Page, useVbenDrawer, VbenButton } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { ElMessage, ElTag } from 'element-plus';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import {
  getPermissionList,
  getRoleList,
  getRolePermissionIds,
  removeRole,
  saveRolePermissions,
} from '#/api';
import { $t } from '#/locales';

import ChecklistDrawer from '../components/checklist-drawer.vue';
import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';
import Menu from './modules/menu.vue';

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});
const [PermissionsDrawer, permissionsDrawerApi] = useVbenDrawer({
  connectedComponent: ChecklistDrawer,
  destroyOnClose: true,
});
const [MenusDrawer, menusDrawerApi] = useVbenDrawer({
  connectedComponent: Menu,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid<RoleApi.Role>({
  formOptions: { schema: useGridFormSchema(), submitOnChange: false },
  gridOptions: {
    columns: useColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: {
      enabled: false,
    },
    proxyConfig: {
      ajax: {
        async query(_params, args) {
          const list = await getRoleList(args);
          return { list, total: list.length };
        },
      },
      showLoading: false,
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: true,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<RoleApi.Role>,
});

function onCreate() {
  formDrawerApi.setData({}).open();
}

function onEdit(row: RoleApi.Role) {
  if (!row.builtIn) formDrawerApi.setData(row).open();
}

function onPermissions(row: RoleApi.Role) {
  if (row.builtIn) return;
  const data: ChecklistDrawerData = {
    async load() {
      const [permissions, selected] = await Promise.all([
        getPermissionList(),
        getRolePermissionIds(row.id),
      ]);
      return {
        options: permissions.map((permission) => ({
          description: permission.targetQualifier
            ? `${permission.targetQualifier} ${permission.targetIdentifier}`
            : permission.targetIdentifier,
          label: permission.name,
          value: permission.id,
        })),
        selected,
      };
    },
    async save(selected) {
      await saveRolePermissions(row.id, selected);
      ElMessage.success($t('page.systemRole.messages.permissionsSuccess'));
    },
    title: $t('page.systemRole.drawer.permissionsTitle', { name: row.name }),
  };
  permissionsDrawerApi.setData(data).open();
}

function onMenus(row: RoleApi.Role) {
  if (!row.builtIn) menusDrawerApi.setData(row).open();
}

async function onDelete(row: RoleApi.Role) {
  if (row.builtIn) return;
  await removeRole(row.id);
  ElMessage.success($t('page.systemRole.messages.removeSuccess'));
  await gridApi.query();
}

function refresh() {
  void gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="refresh" />
    <PermissionsDrawer />
    <MenusDrawer />
    <Grid :table-title="$t('page.systemRole.title')">
      <template #toolbar-tools>
        <VbenButton
          v-access:code="'system:role:create'"
          size="sm"
          @click="onCreate"
        >
          <IconifyIcon icon="lucide:plus" class="mr-2 size-4" />
          {{ $t('page.systemRole.actions.create') }}
        </VbenButton>
      </template>

      <template #type="{ row }">
        <ElTag :type="row.builtIn ? 'warning' : 'info'" effect="plain">
          {{
            row.builtIn
              ? $t('page.rbacCommon.builtIn')
              : $t('page.rbacCommon.custom')
          }}
        </ElTag>
      </template>

      <template #action="{ row }">
        <VbenTableAction
          :actions="[
            {
              auth: 'system:role:update',
              disabled: row.builtIn,
              text: $t('page.rbacCommon.edit'),
              tooltip: row.builtIn
                ? $t('page.rbacCommon.builtInProtected')
                : undefined,
              onClick: () => onEdit(row),
            },
            {
              auth: 'system:role:manage-permissions',
              disabled: row.builtIn,
              text: $t('page.systemRole.actions.permissions'),
              onClick: () => onPermissions(row),
            },
            {
              auth: 'system:role:manage-menus',
              disabled: row.builtIn,
              text: $t('page.systemRole.actions.menus'),
              onClick: () => onMenus(row),
            },
          ]"
          :dropdown-actions="[
            {
              auth: 'system:role:remove',
              danger: true,
              disabled: row.builtIn,
              text: $t('page.rbacCommon.remove'),
              popConfirm: {
                title: $t('page.systemRole.messages.removeConfirm', {
                  name: row.name,
                }),
                confirm: () => onDelete(row),
              },
            },
          ]"
          align="center"
        />
      </template>
    </Grid>
  </Page>
</template>
