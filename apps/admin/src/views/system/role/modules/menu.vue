<script setup lang="ts">
import type { ElTree } from 'element-plus';

import type { MenuApi, RoleApi } from '#/api';

import { nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { isEqual } from '@vben/utils';

import { ElMessage, ElTree as ElTreeComponent } from 'element-plus';

import { getMenuTree, getRoleMenuIds, saveRoleMenus } from '#/api';
import { $t } from '#/locales';

import { confirmDrawerClose } from '../../components/dirty';

interface MenuTreeNode extends Omit<MenuApi.Menu, 'children'> {
  children: MenuTreeNode[];
  displayTitle: string;
}

const emit = defineEmits<{ success: [] }>();

const treeRef = ref<InstanceType<typeof ElTree>>();
const treeData = ref<MenuTreeNode[]>([]);
const initialSelected = ref<string[]>([]);
const currentSelected = ref<string[]>([]);
const role = ref<RoleApi.Role>();
const saved = ref(false);

function normalize(values: Array<number | string>) {
  return [...new Set(values.map(String))].toSorted();
}

function mapMenus(items: MenuApi.Menu[]): MenuTreeNode[] {
  return items.map((item) => ({
    ...item,
    children: mapMenus(item.children ?? []),
    displayTitle: $t(item.title),
  }));
}

function readCheckedKeys() {
  return normalize((treeRef.value?.getCheckedKeys(false) ?? []) as string[]);
}

function onCheck() {
  currentSelected.value = readCheckedKeys();
}

const [Drawer, drawerApi] = useVbenDrawer({
  async onBeforeClose() {
    if (saved.value) return true;
    return confirmDrawerClose(
      !isEqual(currentSelected.value, initialSelected.value),
    );
  },
  async onConfirm() {
    if (!role.value) return;
    drawerApi.lock();
    try {
      const menuIds = readCheckedKeys();
      await saveRoleMenus(role.value.id, menuIds);
      initialSelected.value = menuIds;
      currentSelected.value = menuIds;
      saved.value = true;
      ElMessage.success($t('page.systemRole.messages.menusSuccess'));
      emit('success');
      await drawerApi.close();
    } finally {
      drawerApi.unlock();
    }
  },
  async onOpenChange(open) {
    if (!open) return;
    saved.value = false;
    role.value = drawerApi.getData<RoleApi.Role>();
    drawerApi.setState({
      loading: true,
      title: $t('page.systemRole.drawer.menusTitle', {
        name: role.value.name,
      }),
    });
    try {
      const [menus, selected] = await Promise.all([
        getMenuTree(),
        getRoleMenuIds(role.value.id),
      ]);
      treeData.value = mapMenus(menus);
      initialSelected.value = normalize(selected);
      currentSelected.value = normalize(selected);
      await nextTick();
      treeRef.value?.setCheckedKeys([]);
      for (const id of selected) {
        treeRef.value?.setChecked(String(id), true, false);
      }
    } finally {
      drawerApi.setState({ loading: false });
    }
  },
});
</script>

<template>
  <Drawer class="w-full sm:max-w-2xl" content-class="min-h-0 overflow-auto">
    <ElTreeComponent
      ref="treeRef"
      :data="treeData"
      node-key="id"
      show-checkbox
      check-on-click-node
      default-expand-all
      :props="{ children: 'children', label: 'displayTitle' }"
      @check="onCheck"
    >
      <template #default="{ data }">
        <span class="flex min-w-0 items-center gap-2">
          <span class="truncate">{{ data.displayTitle }}</span>
          <code class="text-xs text-muted-foreground">{{ data.name }}</code>
        </span>
      </template>
    </ElTreeComponent>
  </Drawer>
</template>
