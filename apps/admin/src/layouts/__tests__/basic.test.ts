import { mount } from '@vue/test-utils';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import BasicLayout from '../basic.vue';

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('@vben/common-ui', () => ({
  AuthenticationLoginExpiredModal: {
    name: 'AuthenticationLoginExpiredModal',
    template: '<div><slot /></div>',
  },
}));

vi.mock('@vben/hooks', () => ({
  useWatermark: () => ({
    destroyWatermark: vi.fn(),
    updateWatermark: vi.fn(),
  }),
}));

vi.mock('@vben/layouts', () => ({
  BasicLayout: {
    name: 'BasicLayout',
    template: `
      <main>
        <slot name="user-dropdown" />
        <slot name="notification" />
        <slot name="extra" />
      </main>
    `,
  },
  Notification: {
    name: 'Notification',
    template: '<div />',
  },
  UserDropdown: {
    name: 'UserDropdown',
    props: ['menus'],
    template: '<div />',
  },
}));

vi.mock('@vben/preferences', () => ({
  preferences: {
    app: {
      defaultAvatar: '',
      watermark: false,
      watermarkContent: '',
    },
  },
  usePreferences: () => ({ isDark: { value: false } }),
}));

vi.mock('@vben/stores', () => ({
  useAccessStore: () => ({ loginExpired: false }),
  useUserStore: () => ({
    userInfo: {
      avatar: '',
      desc: '',
      nickname: 'Administrator',
      username: 'admin',
    },
  }),
}));

vi.mock('#/locales', () => ({ $t: (key: string) => key }));
vi.mock('#/store', () => ({
  useAuthStore: () => ({ logout: vi.fn() }),
}));
vi.mock('#/views/_core/authentication/login.vue', () => ({
  default: {
    name: 'LoginForm',
    template: '<form />',
  },
}));

describe('admin basic layout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('omits repository actions from the user dropdown', () => {
    const wrapper = mount(BasicLayout);

    const dropdown = wrapper.getComponent({ name: 'UserDropdown' });

    const menus = dropdown.props('menus');

    expect(menus).not.toEqual(
      expect.arrayContaining([
        expect.objectContaining({ text: 'auth.github' }),
      ]),
    );
    expect(menus).not.toEqual(
      expect.arrayContaining([expect.objectContaining({ text: 'auth.help' })]),
    );
  });
});
