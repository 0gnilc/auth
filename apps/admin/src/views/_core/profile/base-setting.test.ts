import { flushPromises, mount } from '@vue/test-utils';

import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, expect, it, vi } from 'vitest';

import BaseSetting from './base-setting.vue';

const runtime = vi.hoisted(() => ({
  getAdminUserInfo: vi.fn(),
  getMenuAccessCodes: vi.fn(),
  setUserInfo: vi.fn(),
  setValues: vi.fn(),
  updateProfile: vi.fn(),
}));

vi.mock('#/api/core', () => runtime);
vi.mock('#/store', async () => import('#/store/auth'));
vi.mock('#/locales', () => ({ $t: (key: string) => key }));
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn() } }));
vi.mock('vue-router', () => ({ useRouter: () => ({}) }));
vi.mock('@vben/stores', () => ({
  useAccessStore: () => ({ setAccessCodes: vi.fn() }),
  useUserStore: () => ({ setUserInfo: runtime.setUserInfo }),
}));
vi.mock('@vben/common-ui', async (importOriginal) => {
  const { defineComponent } = await import('vue');
  const { z } = await importOriginal<typeof import('@vben/common-ui')>();
  return {
    z,
    ProfileBaseSetting: defineComponent({
      name: 'ProfileBaseSetting',
      emits: ['submit'],
      setup(_, { expose }) {
        expose({ getFormApi: () => ({ setValues: runtime.setValues }) });
        return () => null;
      },
    }),
  };
});

beforeEach(() => {
  vi.clearAllMocks();
  setActivePinia(createPinia());
});

it('preserves the avatar URL in profile editing and auth state', async () => {
  const admin = {
    avatar: 'https://images.example.test/images/admin.png',
    desc: 'Profile',
    nickname: 'Administrator',
  };
  runtime.getAdminUserInfo.mockResolvedValue(admin);
  runtime.getMenuAccessCodes.mockResolvedValue([]);
  runtime.updateProfile.mockResolvedValue(undefined);

  const wrapper = mount(BaseSetting);
  await flushPromises();
  expect(runtime.setValues).toHaveBeenLastCalledWith(admin);

  const form = wrapper.findComponent({ name: 'ProfileBaseSetting' });
  for (let submission = 0; submission < 2; submission++) {
    const [values] = runtime.setValues.mock.lastCall ?? [];
    form.vm.$emit('submit', values);
    await flushPromises();

    expect(runtime.updateProfile).toHaveBeenLastCalledWith({
      avatar: admin.avatar,
      desc: admin.desc,
      nickname: admin.nickname,
    });
    expect(runtime.setValues).toHaveBeenLastCalledWith(admin);
    expect(runtime.setUserInfo).toHaveBeenLastCalledWith({
      ...admin,
      avatar: admin.avatar,
    });
  }
  expect(runtime.updateProfile).toHaveBeenCalledTimes(2);
  wrapper.unmount();
});

it('submits null when optional profile fields are cleared', async () => {
  runtime.getAdminUserInfo.mockResolvedValue({ nickname: 'Administrator' });
  runtime.getMenuAccessCodes.mockResolvedValue([]);
  runtime.updateProfile.mockResolvedValue(undefined);
  const wrapper = mount(BaseSetting);
  await flushPromises();
  wrapper.findComponent({ name: 'ProfileBaseSetting' }).vm.$emit('submit', {
    avatar: '',
    desc: '',
    nickname: 'Administrator',
  });
  await flushPromises();
  expect(runtime.updateProfile).toHaveBeenCalledWith({
    avatar: null,
    desc: null,
    nickname: 'Administrator',
  });
  wrapper.unmount();
});
