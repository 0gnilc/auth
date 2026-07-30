/* eslint-disable vue/one-component-per-file -- Local stubs keep the workflow test focused on drawer behavior. */
import { shallowMount } from '@vue/test-utils';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import AdminForm from '../modules/form.vue';

const runtime = vi.hoisted(() => ({
  api: {
    createAdmin: vi.fn(),
    updateAdmin: vi.fn(),
  },
  confirmClose: vi.fn(),
  drawerApi: {
    close: vi.fn(),
    getData: vi.fn(),
    lock: vi.fn(),
    setState: vi.fn(),
    unlock: vi.fn(),
  },
  drawerData: {} as Record<string, unknown>,
  drawerOptions: undefined as Record<string, any> | undefined,
  formApi: {
    getValues: vi.fn(),
    reset: vi.fn(),
    setValues: vi.fn(),
    validate: vi.fn(),
  },
  formValues: {} as Record<string, unknown>,
  messages: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

vi.mock('@vben/common-ui', async () => {
  const { defineComponent } = await import('vue');
  const Empty = defineComponent({ render: () => null });
  return {
    useVbenDrawer: (options: Record<string, any>) => {
      runtime.drawerOptions = options;
      return [Empty, runtime.drawerApi];
    },
  };
});

vi.mock('#/adapter/form', async () => {
  const { defineComponent } = await import('vue');
  const Empty = defineComponent({ render: () => null });
  const stringRule: Record<string, any> = {};
  stringRule.max = vi.fn(() => stringRule);
  stringRule.refine = vi.fn(() => stringRule);
  return {
    useVbenForm: () => [Empty, runtime.formApi],
    z: { string: () => stringRule },
  };
});

vi.mock('#/api', () => runtime.api);
vi.mock('#/locales', () => ({ $t: (key: string) => key }));
vi.mock('element-plus', () => ({
  ElMessage: runtime.messages,
  ElMessageBox: { confirm: runtime.confirmClose },
}));

describe('administrator form workflow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    runtime.drawerData = {};
    runtime.drawerApi.getData.mockImplementation(() => runtime.drawerData);
    runtime.drawerApi.close.mockResolvedValue(undefined);
    runtime.formApi.getValues.mockImplementation(async () => ({
      ...runtime.formValues,
    }));
    runtime.formApi.reset.mockResolvedValue(undefined);
    runtime.formApi.setValues.mockImplementation(async (values) => {
      runtime.formValues = { ...values };
    });
    runtime.formApi.validate.mockResolvedValue({ valid: true });
  });

  it('keeps failed input editable and allows save retry without reopening', async () => {
    const wrapper = shallowMount(AdminForm);
    const options = runtime.drawerOptions;
    if (!options) throw new Error('Drawer options were not captured');
    await options.onOpenChange(true);
    runtime.formValues = {
      ...runtime.formValues,
      nickname: 'Retry User',
      password: 'Strong#123',
      username: 'retry-user',
    };

    runtime.api.createAdmin.mockRejectedValueOnce(new Error('timeout'));
    await expect(options.onConfirm()).rejects.toThrow('timeout');

    expect(runtime.drawerApi.lock).toHaveBeenCalledOnce();
    expect(runtime.drawerApi.unlock).toHaveBeenCalledOnce();
    expect(runtime.drawerApi.close).not.toHaveBeenCalled();
    expect(wrapper.emitted('success')).toBeUndefined();

    runtime.confirmClose.mockRejectedValueOnce(new Error('keep editing'));
    await expect(options.onBeforeClose()).resolves.toBe(false);

    runtime.api.createAdmin.mockResolvedValue(undefined);
    await expect(options.onConfirm()).resolves.toBeUndefined();

    expect(runtime.api.createAdmin).toHaveBeenCalledTimes(2);
    expect(runtime.api.createAdmin).toHaveBeenLastCalledWith(
      expect.objectContaining({
        nickname: 'Retry User',
        password: 'Strong#123',
        username: 'retry-user',
      }),
    );
    expect(runtime.drawerApi.lock).toHaveBeenCalledTimes(2);
    expect(runtime.drawerApi.unlock).toHaveBeenCalledTimes(2);
    expect(runtime.drawerApi.close).toHaveBeenCalledOnce();
    expect(wrapper.emitted('success')).toHaveLength(1);
    await expect(options.onBeforeClose()).resolves.toBe(true);
    expect(runtime.confirmClose).toHaveBeenCalledOnce();

    wrapper.unmount();
  });
});
