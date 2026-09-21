<script setup lang="ts">
import type { ChecklistDrawerData } from '../components/checklist-drawer.vue';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { AdminApi } from '#/api/system';

import { useAccess } from '@vben/access';
import { Page, useVbenDrawer, VbenButton } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import { ElMessage, ElMessageBox, ElPopover, ElTag } from 'element-plus';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import {
  getAdminPage,
  getRoleList,
  removeAdmin,
  saveAdminRoles,
  updateAdmin,
} from '#/api/system';
import { $t } from '#/locales';

import ChecklistDrawer from '../components/checklist-drawer.vue';
import Form from './components/form.vue';
import { useColumns, useGridFormSchema } from './data';

const ADMIN_ROLE_CODE = 'admin';

const userStore = useUserStore();
const { hasAccessByCodes } = useAccess();

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});
const [RolesDrawer, rolesDrawerApi] = useVbenDrawer({
  connectedComponent: ChecklistDrawer,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid<AdminApi.Admin>({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: false,
  },
  gridOptions: {
    columns: useColumns(
      hasAccessByCodes(['system:admin:update']) ? onStatusChange : undefined,
    ),
    height: 'auto',
    keepSource: true,
    pagerConfig: {},
    proxyConfig: {
      ajax: {
        async query({ page }, args) {
          const result = await getAdminPage({
            currentPage: page.currentPage,
            pageSize: page.pageSize,
            ...args,
          });
          return { list: result.list, total: result.totalCount };
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
  } as VxeTableGridOptions<AdminApi.Admin>,
});

/** 按 RBAC userId 识别当前管理员，不能将管理员资料表的 id 与会话用户主键混用。 */
function isCurrentAdmin(row: AdminApi.Admin) {
  return String(row.userId) === String(userStore.userInfo?.userId ?? '');
}

function onCreate() {
  formDrawerApi.setData({}).open();
}

function onEdit(row: AdminApi.Admin) {
  formDrawerApi.setData({ ...row, currentAdmin: isCurrentAdmin(row) }).open();
}

function onRoles(row: AdminApi.Admin) {
  const data: ChecklistDrawerData = {
    async load() {
      const roles = await getRoleList();
      return {
        options: roles.map((role) => ({
          description: role.remark ?? undefined,
          disabled: role.code === ADMIN_ROLE_CODE,
          label: role.name,
          value: role.code,
        })),
        // admin 是管理员身份的固定角色，回显与提交都保留它，额外角色才由此抽屉编辑。
        selected: [...new Set([ADMIN_ROLE_CODE, ...(row.roleCodes ?? [])])],
      };
    },
    async save(selected) {
      await saveAdminRoles(row.id, [
        ...new Set([ADMIN_ROLE_CODE, ...selected]),
      ]);
      ElMessage.success($t('systemAdmin.messages.rolesSuccess'));
    },
    title: $t('systemAdmin.drawer.rolesTitle', { name: row.username }),
  };
  rolesDrawerApi.setData(data).open();
}

async function onStatusChange(status: boolean, row: AdminApi.Admin) {
  if (isCurrentAdmin(row)) return false;
  try {
    await ElMessageBox.confirm(
      $t('systemAdmin.messages.statusConfirm', {
        name: row.username,
        status: status ? $t('rbacCommon.enabled') : $t('rbacCommon.disabled'),
      }),
      $t('systemAdmin.messages.statusTitle'),
      { type: 'warning' },
    );
    await updateAdmin({ id: row.id, status });
    ElMessage.success($t('systemAdmin.messages.statusSuccess'));
    return true;
  } catch {
    return false;
  }
}

async function onDelete(row: AdminApi.Admin) {
  if (isCurrentAdmin(row)) {
    ElMessage.warning($t('systemAdmin.messages.currentProtected'));
    return;
  }
  await removeAdmin(row.id);
  ElMessage.success($t('systemAdmin.messages.removeSuccess'));
  await gridApi.query();
}

function refresh() {
  void gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="refresh" />
    <RolesDrawer @success="refresh" />
    <Grid :table-title="$t('systemAdmin.title')">
      <template #toolbar-tools>
        <VbenButton
          v-access:code="'system:admin:create'"
          size="sm"
          @click="onCreate"
        >
          <IconifyIcon icon="lucide:plus" class="mr-2 size-4" />
          {{ $t('systemAdmin.actions.create') }}
        </VbenButton>
      </template>

      <template #roles="{ row }">
        <div class="flex min-w-0 items-center gap-1 overflow-hidden">
          <ElTag
            v-for="code in row.roleCodes?.slice(0, 2)"
            :key="code"
            class="max-w-32 shrink truncate"
            effect="plain"
            size="small"
          >
            {{ code }}
          </ElTag>
          <ElPopover
            v-if="row.roleCodes?.length > 2"
            effect="light"
            placement="bottom-start"
            trigger="click"
            :width="360"
          >
            <template #reference>
              <button
                type="button"
                class="bg-muted text-muted-foreground hover:text-foreground focus-visible:ring-ring inline-flex h-6 shrink-0 cursor-pointer items-center rounded-md px-2 text-xs transition-colors focus-visible:outline-none focus-visible:ring-2"
              >
                +{{ row.roleCodes.length - 2 }}
              </button>
            </template>
            <div class="text-muted-foreground mb-2 text-xs">
              {{
                $t('systemAdmin.table.roleCount', {
                  count: row.roleCodes.length,
                })
              }}
            </div>
            <div class="flex max-w-full flex-wrap gap-1.5">
              <ElTag
                v-for="code in row.roleCodes"
                :key="code"
                effect="plain"
                size="small"
              >
                {{ code }}
              </ElTag>
            </div>
          </ElPopover>
        </div>
      </template>

      <template #action="{ row }">
        <VbenTableAction
          :actions="[
            {
              auth: 'system:admin:update',
              text: $t('rbacCommon.edit'),
              onClick: () => onEdit(row),
            },
            {
              auth: 'system:admin:manage-roles',
              text: $t('systemAdmin.actions.roles'),
              onClick: () => onRoles(row),
            },
          ]"
          :dropdown-actions="[
            {
              auth: 'system:admin:remove',
              danger: true,
              disabled: isCurrentAdmin(row),
              text: $t('rbacCommon.remove'),
              popConfirm: {
                title: $t('systemAdmin.messages.removeConfirm', {
                  name: row.username,
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
