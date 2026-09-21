import { flushPromises, mount } from '@vue/test-utils';

import { describe, expect, it, vi } from 'vitest';

import {
  DateTimeRange,
  splitRangeAttribute,
  updateDateTimeRangeValue,
} from '../index';

vi.mock('element-plus/es/components/date-picker/index', async () => {
  const { defineComponent, h } = await import('vue');
  return {
    ElDatePicker: defineComponent({
      name: 'ElDatePicker',
      inheritAttrs: false,
      props: {
        clearable: Boolean,
        id: { default: undefined, type: String },
        modelValue: { default: undefined, type: String },
        name: { default: undefined, type: String },
        placeholder: { default: undefined, type: String },
        type: { default: undefined, type: String },
      },
      emits: ['update:modelValue'],
      setup(props, { attrs, slots }) {
        return () =>
          h(
            'button',
            {
              ...attrs,
              'data-clearable': String(props.clearable),
              'data-picker': props.id,
              type: 'button',
            },
            slots.default?.(),
          );
      },
    }),
  };
});
vi.mock('element-plus/es/components/date-picker/style/css', () => ({}));

describe('date-time range', () => {
  it('范围两端独立更新或清空，并忽略多余位置', () => {
    expect(updateDateTimeRangeValue(['start'], 1, 'end')).toEqual([
      'start',
      'end',
    ]);
    expect(updateDateTimeRangeValue(['start'], 0, undefined)).toEqual([]);
    expect(
      updateDateTimeRangeValue(['start', 'end', 'ignored'], 0, 'new'),
    ).toEqual(['new', 'end']);
  });

  it('splits scalar form attributes across the two controls', () => {
    expect(splitRangeAttribute('created', 'end')).toEqual([
      'created',
      'created_end',
    ]);
    expect(splitRangeAttribute(['from', 'to'], 'end')).toEqual(['from', 'to']);
  });

  it('forwards picker attributes, events, and slots to both controls', async () => {
    const onBlur = vi.fn();
    const wrapper = mount(DateTimeRange, {
      attrs: {
        'data-capability': 'forwarded',
        onBlur,
        readonly: true,
      },
      props: {
        id: 'created',
        modelValue: ['start'],
        name: 'created',
      },
      slots: { default: '<span class="picker-slot">slot</span>' },
    });
    await flushPromises();

    const pickers = wrapper.findAll('[data-picker]');
    expect(pickers).toHaveLength(2);
    expect(
      pickers.map((picker) => picker.attributes('data-capability')),
    ).toEqual(['forwarded', 'forwarded']);
    expect(pickers.map((picker) => picker.attributes('readonly'))).toEqual([
      '',
      '',
    ]);
    expect(pickers.map((picker) => picker.attributes('data-picker'))).toEqual([
      'created',
      'created_end',
    ]);
    expect(
      pickers.map((picker) => picker.attributes('data-clearable')),
    ).toEqual(['true', 'true']);
    expect(wrapper.findAll('.picker-slot')).toHaveLength(2);

    await pickers[0]?.trigger('blur');
    expect(onBlur).toHaveBeenCalledOnce();
  });
});
