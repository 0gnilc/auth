import type { DatePickerProps } from 'element-plus';

import type { PropType } from 'vue';

import { defineAsyncComponent, defineComponent, h } from 'vue';

export type DateTimeRangeValue = Array<null | string | undefined>;

export type DateTimeRangeProps = Omit<
  DatePickerProps,
  'modelValue' | 'placeholder' | 'type'
> & {
  endPlaceholder?: string;
  id?: string | string[];
  modelValue?: DateTimeRangeValue;
  name?: string | string[];
  startPlaceholder?: string;
};

export const ElDatePicker = defineAsyncComponent(() =>
  Promise.all([
    import('element-plus/es/components/date-picker/index'),
    import('element-plus/es/components/date-picker/style/css'),
  ]).then(([res]) => res.ElDatePicker),
);

export const DateTimeRange = defineComponent({
  name: 'DateTimeRange',
  inheritAttrs: false,
  props: {
    clearable: { default: true, type: Boolean },
    disabled: Boolean,
    endPlaceholder: { default: undefined, type: String },
    format: { default: 'YYYY-MM-DD HH:mm:ss', type: String },
    id: {
      default: undefined,
      type: [Array, String] as PropType<string | string[]>,
    },
    modelValue: {
      default: () => [],
      type: Array as PropType<DateTimeRangeValue>,
    },
    name: {
      default: undefined,
      type: [Array, String] as PropType<string | string[]>,
    },
    startPlaceholder: { default: undefined, type: String },
    valueFormat: { default: 'YYYY-MM-DD HH:mm:ss', type: String },
  },
  emits: ['update:modelValue'],
  setup(props, { attrs, emit, slots }) {
    const ids = () => splitRangeAttribute(props.id, 'end');
    const names = () => splitRangeAttribute(props.name, 'end');

    function update(index: 0 | 1, value: null | string | undefined) {
      emit(
        'update:modelValue',
        updateDateTimeRangeValue(props.modelValue, index, value),
      );
    }

    return () =>
      h(
        'div',
        {
          class: ['grid w-full grid-cols-1 gap-2 sm:grid-cols-2', attrs.class],
        },
        [
          h(
            ElDatePicker,
            {
              ...attrs,
              clearable: props.clearable,
              disabled: props.disabled,
              format: props.format,
              id: ids()[0],
              modelValue: props.modelValue[0],
              name: names()[0],
              placeholder: props.startPlaceholder,
              style: [attrs.style, { width: '100%' }],
              type: 'datetime',
              valueFormat: props.valueFormat,
              'onUpdate:modelValue': (value: null | string | undefined) =>
                update(0, value),
            },
            slots,
          ),
          h(
            ElDatePicker,
            {
              ...attrs,
              clearable: props.clearable,
              disabled: props.disabled,
              format: props.format,
              id: ids()[1],
              modelValue: props.modelValue[1],
              name: names()[1],
              placeholder: props.endPlaceholder,
              style: [attrs.style, { width: '100%' }],
              type: 'datetime',
              valueFormat: props.valueFormat,
              'onUpdate:modelValue': (value: null | string | undefined) =>
                update(1, value),
            },
            slots,
          ),
        ],
      );
  },
});

/** 独立保留范围两端：清空一端不连带删除另一端，两端都空才回到空数组。 */
export function updateDateTimeRangeValue(
  current: DateTimeRangeValue,
  index: 0 | 1,
  value: null | string | undefined,
) {
  const next: DateTimeRangeValue = [current[0], current[1]];
  next[index] = value || undefined;
  return next.some((item) => item !== undefined) ? next : [];
}

export function splitRangeAttribute(
  value: string | string[] | undefined,
  suffix: string,
) {
  if (Array.isArray(value)) return value;
  return value ? [value, `${value}_${suffix}`] : [];
}
