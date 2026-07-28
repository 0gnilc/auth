import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useAuthStore } from './auth';

const api = vi.hoisted(() => ({
  getAdminUserInfo: vi.fn(),
  getMenuAccessCodes: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}));
const stores = vi.hoisted(() => ({
  access: {
    accessCodes: [] as string[],
    loginExpired: false,
    refreshToken: null,
    setAccessCodes: vi.fn(),
    setAccessToken: vi.fn(),
    setLoginExpired: vi.fn(),
    setRefreshToken: vi.fn(),
  },
  resetAllStores: vi.fn(),
  user: {
    setUserInfo: vi.fn(),
  },
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));
vi.mock('@vben/stores', () => ({
  resetAllStores: stores.resetAllStores,
  useAccessStore: () => stores.access,
  useUserStore: () => stores.user,
}));
vi.mock('#/api', () => api);
vi.mock('#/locales', () => ({ $t: (key: string) => key }));

describe('administrator session state', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    stores.access.accessCodes = [];
    stores.access.setAccessCodes.mockImplementation((codes: string[]) => {
      stores.access.accessCodes = codes;
    });
  });

  it('refreshes button access codes when restoring an existing session', async () => {
    api.getAdminUserInfo.mockResolvedValue({
      roleCodes: ['admin', 'rbac:manager'],
      userId: '1',
      username: 'admin',
    });
    api.getMenuAccessCodes.mockResolvedValue([
      'system:admin:create',
      'system:role:create',
    ]);
    await useAuthStore().getUserInfo();

    expect(api.getMenuAccessCodes).toHaveBeenCalledOnce();
    expect(stores.access.accessCodes).toEqual([
      'system:admin:create',
      'system:role:create',
    ]);
  });
});
