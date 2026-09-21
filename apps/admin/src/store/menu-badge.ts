import type { MenuRecordRaw } from '@vben/types';

import { reactive, watch } from 'vue';

import { useAccessStore } from '@vben/stores';

import { defineStore } from 'pinia';

/** 待办数量接管菜单徽标之前的静态配置快照，用于待办清零后恢复。 */
type MenuBadgeConfiguration = Pick<
  MenuRecordRaw,
  'badge' | 'badgeType' | 'badgeVariants'
>;

const MAX_VISIBLE_BADGE_COUNT = 99;

export const useMenuBadgeStore = defineStore('menu-badge', () => {
  const accessStore = useAccessStore();
  const configuredBadgeByMenu = new WeakMap<
    MenuRecordRaw,
    MenuBadgeConfiguration
  >();
  const menuBadgeCountByPath = reactive(new Map<string, number>());

  function isValidCount(value: unknown): value is number {
    return (
      typeof value === 'number' && Number.isSafeInteger(value) && value >= 0
    );
  }

  function applyBadge(menu: MenuRecordRaw, total: number) {
    if (!isValidCount(total) || total === 0) {
      restoreConfiguredBadge(menu);
      return;
    }

    menu.badge = total > MAX_VISIBLE_BADGE_COUNT ? '99+' : String(total);
    menu.badgeType = 'normal';
    menu.badgeVariants = 'destructive';
  }

  /** 首次接管时保存静态徽标；动态数量归零后恢复配置，而非删除菜单原有提示。 */
  function rememberConfiguredBadge(menu: MenuRecordRaw) {
    if (configuredBadgeByMenu.has(menu)) {
      return;
    }

    configuredBadgeByMenu.set(menu, {
      badge: menu.badge,
      badgeType: menu.badgeType,
      badgeVariants: menu.badgeVariants,
    });
  }

  function restoreConfiguredBadge(menu: MenuRecordRaw) {
    const configuredBadge = configuredBadgeByMenu.get(menu);
    if (!configuredBadge) {
      return;
    }

    menu.badge = configuredBadge.badge;
    menu.badgeType = configuredBadge.badgeType;
    menu.badgeVariants = configuredBadge.badgeVariants;
  }

  function synchronizeMenuBadges() {
    /** 当前菜单及已接管子菜单的待办徽标聚合结果。 */
    function synchronizeMenuBadge(menu: MenuRecordRaw): {
      /** 当前菜单或后代是否由动态待办数量管理。 */
      managed: boolean;
      /** 已校验为非负安全整数的待办总数；不包含未接管菜单的静态徽标。 */
      total: number;
    } {
      rememberConfiguredBadge(menu);

      const ownsCount = menuBadgeCountByPath.has(menu.path);
      const ownCount = menuBadgeCountByPath.get(menu.path);
      let managed = ownsCount;
      let total = ownsCount && isValidCount(ownCount) ? ownCount : 0;

      for (const child of menu.children ?? []) {
        const childResult = synchronizeMenuBadge(child);
        if (!childResult.managed || !isValidCount(childResult.total)) {
          continue;
        }

        const mergedTotal = total + childResult.total;
        if (!isValidCount(mergedTotal)) {
          continue;
        }

        managed = true;
        total = mergedTotal;
      }

      if (managed) {
        applyBadge(menu, total);
      }

      return { managed, total };
    }

    for (const menu of accessStore.accessMenus) {
      synchronizeMenuBadge(menu);
    }
  }

  function setMenuBadgeCounts(counts: Record<string, unknown>) {
    for (const [path, count] of Object.entries(counts)) {
      menuBadgeCountByPath.set(path, isValidCount(count) ? count : 0);
    }

    synchronizeMenuBadges();
  }

  function setMenuBadgeCount(path: string, count: unknown) {
    const accepted = isValidCount(count);
    menuBadgeCountByPath.set(path, accepted ? count : 0);
    synchronizeMenuBadges();
    return accepted;
  }

  function $reset() {
    const paths = [...menuBadgeCountByPath.keys()];
    for (const path of paths) {
      menuBadgeCountByPath.set(path, 0);
    }

    synchronizeMenuBadges();
    menuBadgeCountByPath.clear();
  }

  watch(
    () => accessStore.accessMenus,
    () => synchronizeMenuBadges(),
    { flush: 'post' },
  );

  return {
    $reset,
    menuBadgeCountByPath,
    setMenuBadgeCount,
    setMenuBadgeCounts,
  };
});
