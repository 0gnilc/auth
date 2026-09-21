import { flushPromises, shallowMount } from '@vue/test-utils';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import SelectItem from '../../select-item.vue';
import General from '../general.vue';

const runtime = vi.hoisted(() => ({
  getTimezoneOptions: vi.fn(),
  setTimezone: vi.fn(),
  timezone: 'Asia/Shanghai',
}));

vi.mock('@vben/constants', () => ({ SUPPORT_LANGUAGES: [] }));
vi.mock('@vben/locales', () => ({ $t: (key: string) => key }));
vi.mock('@vben/stores', () => ({ useTimezoneStore: () => runtime }));

describe('preference timezone selection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    runtime.getTimezoneOptions.mockResolvedValue([
      { label: 'Asia/Shanghai(GMT+8)', value: 'Asia/Shanghai' },
      { label: 'Africa/Lagos(GMT+1)', value: 'Africa/Lagos' },
    ]);
    runtime.setTimezone.mockResolvedValue(undefined);
  });

  it('applies a preference drawer selection through the Timezone Store', async () => {
    const wrapper = shallowMount(General, {
      props: { appTimezone: 'Asia/Shanghai' },
    });
    await flushPromises();

    const selects = wrapper.findAllComponents(SelectItem);
    expect(selects).toHaveLength(2);
    selects[1]?.vm.$emit('update:modelValue', 'Africa/Lagos');
    await flushPromises();

    expect(runtime.setTimezone).toHaveBeenCalledWith('Africa/Lagos');
  });
});
