import type { MenuRecordRaw } from '@vben/types';

import { nextTick } from 'vue';

import { useAccessStore } from '@vben/stores';

import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';

import { useMenuBadgeStore } from './menu-badge';

function nestedMenus(configuredBadges = false): MenuRecordRaw[] {
  return [
    {
      ...(configuredBadges
        ? {
            badge: 'Configured',
            badgeType: 'normal' as const,
            badgeVariants: 'primary',
          }
        : {}),
      children: [
        {
          children: [
            {
              ...(configuredBadges
                ? {
                    badge: 'New',
                    badgeType: 'normal' as const,
                    badgeVariants: 'primary',
                  }
                : {}),
              name: 'Requests',
              path: '/workspace/review/requests',
            },
            { name: 'Alerts', path: '/workspace/review/alerts' },
          ],
          name: 'Review',
          path: '/workspace/review',
        },
      ],
      name: 'Workspace',
      path: '/workspace',
    },
  ];
}

describe('administrator menu badges', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('aggregates leaf counts through three menu levels and caps displayed totals', () => {
    const accessStore = useAccessStore();
    accessStore.setAccessMenus(nestedMenus());
    const badgeStore = useMenuBadgeStore();

    badgeStore.setMenuBadgeCounts({
      '/workspace/review/alerts': 80,
      '/workspace/review/requests': 30,
    });

    expect(accessStore.getMenuByPath('/workspace/review/alerts')).toMatchObject(
      {
        badge: '80',
        badgeType: 'normal',
        badgeVariants: 'destructive',
      },
    );
    expect(
      accessStore.getMenuByPath('/workspace/review/requests'),
    ).toMatchObject({
      badge: '30',
      badgeType: 'normal',
      badgeVariants: 'destructive',
    });
    expect(accessStore.getMenuByPath('/workspace/review')).toMatchObject({
      badge: '99+',
      badgeType: 'normal',
      badgeVariants: 'destructive',
    });
    expect(accessStore.getMenuByPath('/workspace')).toMatchObject({
      badge: '99+',
      badgeType: 'normal',
      badgeVariants: 'destructive',
    });

    badgeStore.setMenuBadgeCount('/workspace/review/requests', 10);

    expect(accessStore.getMenuByPath('/workspace/review')?.badge).toBe('90');
    expect(accessStore.getMenuByPath('/workspace')?.badge).toBe('90');
  });

  it.each([
    '30',
    '99+',
    -1,
    1.5,
    Number.NaN,
    Number.POSITIVE_INFINITY,
    Number.MAX_SAFE_INTEGER + 1,
  ])('excludes invalid child count %p from every parent total', (count) => {
    const accessStore = useAccessStore();
    accessStore.setAccessMenus(nestedMenus());
    const badgeStore = useMenuBadgeStore();
    badgeStore.setMenuBadgeCounts({
      '/workspace/review/alerts': 5,
      '/workspace/review/requests': 30,
    });

    const accepted = badgeStore.setMenuBadgeCount(
      '/workspace/review/requests',
      count,
    );

    expect(accepted).toBe(false);
    expect(
      accessStore.getMenuByPath('/workspace/review/requests')?.badge,
    ).toBeUndefined();
    expect(accessStore.getMenuByPath('/workspace/review')?.badge).toBe('5');
    expect(accessStore.getMenuByPath('/workspace')?.badge).toBe('5');
  });

  it('displays 99 exactly and caps 100 as 99+', () => {
    const accessStore = useAccessStore();
    accessStore.setAccessMenus(nestedMenus());
    const badgeStore = useMenuBadgeStore();

    badgeStore.setMenuBadgeCount('/workspace/review/requests', 99);

    expect(accessStore.getMenuByPath('/workspace/review')?.badge).toBe('99');
    expect(accessStore.getMenuByPath('/workspace')?.badge).toBe('99');

    badgeStore.setMenuBadgeCount('/workspace/review/requests', 100);

    expect(accessStore.getMenuByPath('/workspace/review')?.badge).toBe('99+');
    expect(accessStore.getMenuByPath('/workspace')?.badge).toBe('99+');
  });

  it('hides ancestor badges after every descendant count reaches zero', () => {
    const accessStore = useAccessStore();
    accessStore.setAccessMenus(nestedMenus());
    const badgeStore = useMenuBadgeStore();
    badgeStore.setMenuBadgeCounts({
      '/workspace/review/alerts': 5,
      '/workspace/review/requests': 3,
    });

    badgeStore.setMenuBadgeCount('/workspace/review/requests', 0);

    expect(accessStore.getMenuByPath('/workspace/review')?.badge).toBe('5');
    expect(accessStore.getMenuByPath('/workspace')?.badge).toBe('5');

    badgeStore.setMenuBadgeCount('/workspace/review/alerts', 0);

    expect(
      accessStore.getMenuByPath('/workspace/review')?.badge,
    ).toBeUndefined();
    expect(accessStore.getMenuByPath('/workspace')?.badge).toBeUndefined();
  });

  it('菜单树替换后重新应用现有待办数量', async () => {
    const accessStore = useAccessStore();
    const badgeStore = useMenuBadgeStore();
    badgeStore.setMenuBadgeCount('/workspace/review/requests', 4);

    accessStore.setAccessMenus(nestedMenus());
    await nextTick();

    expect(accessStore.getMenuByPath('/workspace/review/requests')?.badge).toBe(
      '4',
    );
    expect(accessStore.getMenuByPath('/workspace/review')?.badge).toBe('4');
    expect(accessStore.getMenuByPath('/workspace')?.badge).toBe('4');
  });

  it('clears leaf and ancestor badges when the store resets', () => {
    const accessStore = useAccessStore();
    accessStore.setAccessMenus(nestedMenus());
    const badgeStore = useMenuBadgeStore();
    badgeStore.setMenuBadgeCount('/workspace/review/requests', 4);

    badgeStore.$reset();

    expect(
      accessStore.getMenuByPath('/workspace/review/requests')?.badge,
    ).toBeUndefined();
    expect(
      accessStore.getMenuByPath('/workspace/review')?.badge,
    ).toBeUndefined();
    expect(accessStore.getMenuByPath('/workspace')?.badge).toBeUndefined();
  });

  it('动态待办清零后恢复 Server 原始徽标配置', () => {
    const accessStore = useAccessStore();
    accessStore.setAccessMenus(nestedMenus(true));
    const badgeStore = useMenuBadgeStore();
    badgeStore.setMenuBadgeCount('/workspace/review/requests', 4);

    badgeStore.$reset();

    expect(
      accessStore.getMenuByPath('/workspace/review/requests'),
    ).toMatchObject({
      badge: 'New',
      badgeType: 'normal',
      badgeVariants: 'primary',
    });
    expect(accessStore.getMenuByPath('/workspace')).toMatchObject({
      badge: 'Configured',
      badgeType: 'normal',
      badgeVariants: 'primary',
    });
  });
});
