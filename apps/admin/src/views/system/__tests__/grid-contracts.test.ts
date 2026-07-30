/* eslint-disable vue/one-component-per-file -- Inline stubs keep the grid contract test self-contained. */
import { flushPromises, shallowMount } from '@vue/test-utils';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import AdminPage from '../admin/index.vue';
import I18nMessagePage from '../i18n-message/index.vue';
import MenuPage from '../menu/index.vue';
import PermissionPage from '../permission/index.vue';
import RolePage from '../role/index.vue';

const runtime = vi.hoisted(() => ({
  api: {
    getAdminPage: vi.fn(),
    getI18nMessageCategories: vi.fn(),
    getI18nMessagePage: vi.fn(),
    getMenuTree: vi.fn(),
    getPermissionList: vi.fn(),
    getRoleList: vi.fn(),
    updateAdmin: vi.fn(),
  },
  confirm: vi.fn(),
  gridConfigs: [] as Array<Record<string, any>>,
}));

vi.mock('#/adapter/vxe-table', async () => {
  const { defineComponent } = await import('vue');
  const Empty = defineComponent({ render: () => null });
  return {
    VbenTableAction: Empty,
    useVbenVxeGrid: vi.fn((config: Record<string, any>) => {
      runtime.gridConfigs.push(config);
      return [Empty, { query: vi.fn() }];
    }),
  };
});

vi.mock('#/adapter/form', () => ({
  useVbenForm: vi.fn(),
  z: {},
}));

vi.mock('#/api', () => ({
  ...runtime.api,
  MenuApi: {
    BadgeTypes: [],
    BadgeVariants: [],
    MenuTypes: ['catalog', 'menu', 'embedded', 'link', 'button'],
  },
}));

vi.mock('#/locales', () => ({
  $t: (key: string) => key,
  SUPPORTED_LOCALES: ['en-US', 'zh-CN'],
}));

vi.mock('#/locales/dynamic', () => ({
  reloadDynamicMessages: vi.fn(),
}));

vi.mock('@vben/access', () => ({
  useAccess: () => ({ hasAccessByCodes: () => true }),
}));

vi.mock('@vben/common-ui', async () => {
  const { defineComponent } = await import('vue');
  const Empty = defineComponent({ render: () => null });
  return {
    Page: Empty,
    VbenButton: Empty,
    useVbenDrawer: () => {
      const api: Record<string, any> = {};
      api.close = vi.fn();
      api.getData = vi.fn(() => ({}));
      api.lock = vi.fn();
      api.open = vi.fn(() => api);
      api.setData = vi.fn(() => api);
      api.setState = vi.fn(() => api);
      api.unlock = vi.fn();
      return [Empty, api];
    },
  };
});

vi.mock('@vben/icons', async () => {
  const { defineComponent } = await import('vue');
  return { IconifyIcon: defineComponent({ render: () => null }) };
});

vi.mock('@vben/stores', () => ({
  useUserStore: () => ({ userInfo: { userId: '1' } }),
}));

vi.mock('element-plus', async () => {
  const { defineComponent } = await import('vue');
  const Empty = defineComponent({ render: () => null });
  return {
    ElMessage: { success: vi.fn(), warning: vi.fn() },
    ElMessageBox: { confirm: runtime.confirm },
    ElPopover: Empty,
    ElTag: Empty,
  };
});

async function captureGrid(page: any) {
  const wrapper = shallowMount(page, {
    global: { directives: { access: () => undefined } },
  });
  await flushPromises();
  const config = runtime.gridConfigs.at(-1);
  wrapper.unmount();
  if (!config) throw new Error('Grid configuration not captured');
  return config;
}

function queryOf(config: Record<string, any>) {
  return config.gridOptions.proxyConfig.ajax.query as (
    params: Record<string, any>,
    args: Record<string, any>,
  ) => Promise<{ list: any[]; total: number }>;
}

describe('system management grid contracts', () => {
  beforeEach(() => {
    runtime.gridConfigs.length = 0;
    vi.clearAllMocks();
    runtime.api.getAdminPage.mockResolvedValue({
      list: [{ id: '1' }],
      totalCount: 7,
    });
    runtime.api.getI18nMessageCategories.mockResolvedValue([
      'default',
      'admin',
    ]);
    runtime.api.getI18nMessagePage.mockResolvedValue({
      list: [{ category: 'admin', messageKey: 'menu.title', values: [] }],
      totalCount: 3,
    });
    runtime.api.getPermissionList.mockResolvedValue([{ id: 'permission-1' }]);
    runtime.api.getRoleList.mockResolvedValue([{ id: 'role-1' }]);
    runtime.api.updateAdmin.mockResolvedValue(undefined);
    runtime.confirm.mockResolvedValue(undefined);
    runtime.api.getMenuTree.mockResolvedValue([
      {
        accessCode: null,
        children: [
          {
            accessCode: 'system:report:export',
            children: [],
            id: '2',
            name: 'Export',
            path: null,
            title: 'menu.export',
          },
        ],
        id: '1',
        name: 'Reports',
        path: '/reports',
        title: 'menu.reports',
      },
    ]);
  });

  it('queries only through explicit grid actions and maps every result to list and total', async () => {
    const admin = await captureGrid(AdminPage);
    const role = await captureGrid(RolePage);
    const permission = await captureGrid(PermissionPage);
    const menu = await captureGrid(MenuPage);
    const i18n = await captureGrid(I18nMessagePage);

    for (const config of [admin, role, permission, menu, i18n]) {
      expect(config.formOptions.submitOnChange).toBe(false);
      expect(config.gridOptions.proxyConfig.showLoading).toBe(false);
    }

    await expect(
      queryOf(admin)(
        { page: { currentPage: 2, pageSize: 20 } },
        { username: 'alice' },
      ),
    ).resolves.toEqual({ list: [{ id: '1' }], total: 7 });
    expect(runtime.api.getAdminPage).toHaveBeenCalledWith({
      currentPage: 2,
      pageSize: 20,
      username: 'alice',
    });

    await expect(queryOf(role)({}, { name: 'Operator' })).resolves.toEqual({
      list: [{ id: 'role-1' }],
      total: 1,
    });
    await expect(
      queryOf(permission)({}, { publicAccess: false }),
    ).resolves.toEqual({ list: [{ id: 'permission-1' }], total: 1 });
    await expect(queryOf(menu)({}, { keyword: '  EXPORT  ' })).resolves.toEqual(
      {
        list: [
          expect.objectContaining({
            children: [expect.objectContaining({ id: '2' })],
            id: '1',
          }),
        ],
        total: 1,
      },
    );
    await expect(
      queryOf(i18n)(
        { page: { currentPage: 1, pageSize: 10 } },
        { category: 'admin' },
      ),
    ).resolves.toEqual({
      list: [expect.objectContaining({ rowKey: 'menu.title' })],
      total: 3,
    });
  });

  it('keeps an admin status switch unchanged on cancel or failure and permits retry', async () => {
    const admin = await captureGrid(AdminPage);
    const statusColumn = admin.gridOptions.columns.find(
      (column: Record<string, any>) => column.field === 'status',
    );
    const beforeChange = statusColumn.cellRender.attrs.beforeChange as (
      status: boolean,
      row: Record<string, any>,
    ) => Promise<boolean>;
    const row = { id: '2', userId: '2', username: 'operator' };

    runtime.confirm.mockRejectedValueOnce(new Error('cancelled'));
    await expect(beforeChange(false, row)).resolves.toBe(false);
    expect(runtime.api.updateAdmin).not.toHaveBeenCalled();

    runtime.api.updateAdmin.mockRejectedValueOnce(new Error('timeout'));
    await expect(beforeChange(false, row)).resolves.toBe(false);
    expect(runtime.api.updateAdmin).toHaveBeenCalledOnce();

    await expect(beforeChange(false, row)).resolves.toBe(true);
    expect(runtime.api.updateAdmin).toHaveBeenCalledTimes(2);
    expect(runtime.api.updateAdmin).toHaveBeenLastCalledWith({
      id: '2',
      status: false,
    });

    await expect(
      beforeChange(false, { ...row, id: '1', userId: '1' }),
    ).resolves.toBe(false);
    expect(runtime.confirm).toHaveBeenCalledTimes(3);
  });
});
