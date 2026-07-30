<script setup lang="ts">
import type { MenuFormDrawerData } from './modules/form.vue';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MenuApi } from '#/api';

import { ref } from 'vue';

import { Page, useVbenDrawer, VbenButton } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { ElMessage, ElMessageBox, ElTag } from 'element-plus';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { getMenuTree, getRoleList, getRoleMenuIds, removeMenu } from '#/api';
import { $t } from '#/locales';

import { menuTypeTagTypes, useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const menuTree = ref<MenuApi.Menu[]>([]);

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

function filterTree(items: MenuApi.Menu[], keyword: string): MenuApi.Menu[] {
  if (!keyword) return items;
  return items.flatMap((item) => {
    const children = filterTree(item.children ?? [], keyword);
    const matches =
      `${$t(item.title)} ${item.name} ${item.path ?? ''} ${item.accessCode ?? ''}`
        .toLocaleLowerCase()
        .includes(keyword);
    return matches || children.length > 0 ? [{ ...item, children }] : [];
  });
}

const [Grid, gridApi] = useVbenVxeGrid<MenuApi.Menu>({
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
          menuTree.value = await getMenuTree();
          const keyword = String(args.keyword ?? '')
            .trim()
            .toLocaleLowerCase();
          const list = filterTree(menuTree.value, keyword);
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
    treeConfig: {
      parentField: 'pid',
      rowField: 'id',
      transform: false,
    },
  } as VxeTableGridOptions<MenuApi.Menu>,
});

function openForm(data: Omit<MenuFormDrawerData, 'menus'>) {
  formDrawerApi.setData({ ...data, menus: menuTree.value }).open();
}

function onCreate(parentId = '0') {
  openForm({ parentId: String(parentId) });
}

function onEdit(row: MenuApi.Menu) {
  if (!row.builtIn) openForm({ row });
}

function descendants(row: MenuApi.Menu): MenuApi.Menu[] {
  return (row.children ?? []).flatMap((child) => [
    child,
    ...descendants(child),
  ]);
}

function hasBuiltInDescendant(row: MenuApi.Menu) {
  return descendants(row).some((item) => item.builtIn);
}

async function onDelete(row: MenuApi.Menu) {
  if (row.builtIn || hasBuiltInDescendant(row)) return;
  const subtreeIds = new Set([row.id, ...descendants(row).map(({ id }) => id)]);
  const roles = await getRoleList();
  const assignedIds = await Promise.all(
    roles.map(async (role) => ({
      ids: await getRoleMenuIds(role.id),
      role,
    })),
  );
  const affectedRoles = assignedIds.filter(({ ids }) =>
    ids.some((id) => subtreeIds.has(id)),
  );
  try {
    await ElMessageBox.confirm(
      $t('page.systemMenu.messages.removeImpactConfirm', {
        descendants: subtreeIds.size - 1,
        name: $t(row.title),
        roles: affectedRoles.length,
      }),
      $t('page.systemMenu.messages.removeTitle'),
      { type: 'warning' },
    );
  } catch {
    return;
  }
  await removeMenu(row.id);
  ElMessage.success($t('page.systemMenu.messages.removeSuccess'));
  await gridApi.query();
}

function canAppend(row: MenuApi.Menu) {
  return row.type === 'catalog' || row.type === 'menu';
}

function refresh() {
  void gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="refresh" />
    <Grid :table-title="$t('page.systemMenu.title')">
      <template #toolbar-tools>
        <VbenButton
          v-access:code="'system:menu:create'"
          size="sm"
          @click="onCreate()"
        >
          <IconifyIcon icon="lucide:plus" class="mr-2 size-4" />
          {{ $t('page.systemMenu.actions.create') }}
        </VbenButton>
      </template>

      <template #title="{ row }">
        <div class="flex min-w-0 items-center gap-2">
          <IconifyIcon
            :icon="
              row.type === 'button'
                ? 'lucide:mouse-pointer-click'
                : row.icon || 'lucide:circle'
            "
            class="size-4 shrink-0"
          />
          <span class="truncate">{{ $t(row.title) }}</span>
        </div>
      </template>

      <template #type="{ row }">
        <ElTag :type="menuTypeTagTypes[row.type]" effect="plain">
          {{ $t(`page.systemMenu.types.${row.type}`) }}
        </ElTag>
      </template>

      <template #accessCode="{ row }">
        <code class="break-all text-xs">{{ row.accessCode }}</code>
      </template>

      <template #status="{ row }">
        <ElTag :type="row.status ? 'success' : 'info'" effect="plain">
          {{
            row.status
              ? $t('page.rbacCommon.enabled')
              : $t('page.rbacCommon.disabled')
          }}
        </ElTag>
      </template>

      <template #action="{ row }">
        <VbenTableAction
          :actions="[
            {
              auth: 'system:menu:create',
              disabled: !canAppend(row),
              text: $t('page.systemMenu.actions.append'),
              onClick: () => onCreate(row.id),
            },
            {
              auth: 'system:menu:update',
              disabled: row.builtIn,
              text: $t('page.rbacCommon.edit'),
              tooltip: row.builtIn
                ? $t('page.rbacCommon.builtInProtected')
                : undefined,
              onClick: () => onEdit(row),
            },
          ]"
          :dropdown-actions="[
            {
              auth: 'system:menu:remove',
              danger: true,
              disabled: row.builtIn || hasBuiltInDescendant(row),
              text: $t('page.rbacCommon.remove'),
              onClick: () => onDelete(row),
            },
          ]"
          align="center"
        />
      </template>
    </Grid>
  </Page>
</template>
