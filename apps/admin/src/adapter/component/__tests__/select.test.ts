import { flushPromises, mount } from '@vue/test-utils';

import { describe, expect, it, vi } from 'vitest';

import { Select } from '../index';

vi.mock('element-plus/es/components/select-v2/index', async () => {
  const { defineComponent, h } = await import('vue');
  return {
    ElSelectV2: defineComponent({
      name: 'ElSelectV2',
      inheritAttrs: false,
      props: { options: { default: () => [], type: Array } },
      setup(props) {
        return () => {
          const options = props.options as Array<{ label: unknown }>;
          return h(
            'div',
            { 'data-options': JSON.stringify(options) },
            options.map((option) => String(option.label)),
          );
        };
      },
    }),
  };
});
vi.mock('element-plus/es/components/select-v2/style/css', () => ({}));

describe('select adapter', () => {
  it('forwards options to Element Plus Select V2', async () => {
    const options = [
      { label: 'Regular', value: 'REGULAR' },
      { label: 'Gold', value: 'GOLD' },
    ];
    const wrapper = mount(Select, { attrs: { options } });
    await flushPromises();

    expect(wrapper.find('[data-options]').attributes('data-options')).toBe(
      JSON.stringify(options),
    );
    expect(wrapper.text()).toContain('Regular');
    expect(wrapper.text()).toContain('Gold');
  });
});
