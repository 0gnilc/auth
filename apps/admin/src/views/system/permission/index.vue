<script setup lang="ts">
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { PermissionApi } from '#/api/system';

import { Page, useVbenDrawer, VbenButton } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { ElMessage, ElTag } from 'element-plus';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { getPermissionList, removePermission } from '#/api/system';
import { $t } from '#/locales';

import Form from './components/form.vue';
import { useColumns, useGridFormSchema } from './data';

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid<PermissionApi.Permission>({
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
          const list = await getPermissionList(args);
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
  } as VxeTableGridOptions<PermissionApi.Permission>,
});

function onCreate() {
  formDrawerApi.setData({ publicAccess: false }).open();
}

function onEdit(row: PermissionApi.Permission) {
  if (!row.builtIn) formDrawerApi.setData(row).open();
}

async function onDelete(row: PermissionApi.Permission) {
  if (row.builtIn) return;
  await removePermission(row.id);
  ElMessage.success($t('systemPermission.messages.removeSuccess'));
  await gridApi.query();
}

function refresh() {
  void gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="refresh" />
    <Grid :table-title="$t('systemPermission.title')">
      <template #toolbar-tools>
        <VbenButton
          v-access:code="'system:permission:create'"
          size="sm"
          @click="onCreate"
        >
          <IconifyIcon icon="lucide:plus" class="mr-2 size-4" />
          {{ $t('systemPermission.actions.create') }}
        </VbenButton>
      </template>

      <template #access="{ row }">
        <ElTag :type="row.publicAccess ? 'success' : 'info'" effect="plain">
          {{
            row.publicAccess
              ? $t('systemPermission.public')
              : $t('systemPermission.protected')
          }}
        </ElTag>
      </template>

      <template #type="{ row }">
        <ElTag :type="row.builtIn ? 'warning' : 'info'" effect="plain">
          {{ row.builtIn ? $t('rbacCommon.builtIn') : $t('rbacCommon.custom') }}
        </ElTag>
      </template>

      <template #action="{ row }">
        <VbenTableAction
          :actions="[
            {
              auth: 'system:permission:update',
              disabled: row.builtIn,
              text: $t('rbacCommon.edit'),
              tooltip: row.builtIn
                ? $t('rbacCommon.builtInProtected')
                : undefined,
              onClick: () => onEdit(row),
            },
          ]"
          :dropdown-actions="[
            {
              auth: 'system:permission:remove',
              danger: true,
              disabled: row.builtIn,
              text: $t('rbacCommon.remove'),
              popConfirm: {
                title: $t('systemPermission.messages.removeConfirm', {
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
