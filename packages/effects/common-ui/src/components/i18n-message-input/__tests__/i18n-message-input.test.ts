import type { I18nMessageLoader, I18nMessageSaver } from '../types';

import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, h, ref } from 'vue';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import I18nMessageInput from '../i18n-message-input.vue';

const dialog = vi.hoisted(() => ({
  confirm: vi.fn(),
}));

vi.mock('@vben/locales', () => ({
  $t: (key: string) => key,
}));

vi.mock('@vben-core/popup-ui', () => ({
  confirm: dialog.confirm,
}));

vi.mock('@vben-core/shadcn-ui', async () => {
  const { defineComponent, h } = await import('vue');
  const Input = defineComponent({
    inheritAttrs: false,
    props: {
      disabled: Boolean,
      modelValue: { default: '', type: String },
      readonly: Boolean,
    },
    emits: ['update:modelValue'],
    setup(props, { attrs, emit }) {
      return () =>
        h('input', {
          ...attrs,
          disabled: props.disabled,
          readonly: props.readonly,
          value: props.modelValue,
          onInput: (event: Event) =>
            emit('update:modelValue', (event.target as HTMLInputElement).value),
        });
    },
  });
  const Textarea = defineComponent({
    inheritAttrs: false,
    props: {
      disabled: Boolean,
      modelValue: { default: '', type: String },
    },
    emits: ['update:modelValue'],
    setup(props, { attrs, emit }) {
      return () =>
        h('textarea', {
          ...attrs,
          disabled: props.disabled,
          value: props.modelValue,
          onInput: (event: Event) =>
            emit(
              'update:modelValue',
              (event.target as HTMLTextAreaElement).value,
            ),
        });
    },
  });
  const Button = defineComponent({
    inheritAttrs: false,
    props: { disabled: Boolean },
    setup(props, { attrs, slots }) {
      return () =>
        h('button', { ...attrs, disabled: props.disabled }, slots.default?.());
    },
  });
  const VbenPopover = defineComponent({
    name: 'VbenPopover',
    props: { open: Boolean },
    emits: ['update:open'],
    setup(props, { emit, slots }) {
      return () =>
        h('div', [
          h(
            'div',
            {
              'data-test': 'trigger',
              onClick: () => emit('update:open', true),
            },
            slots.trigger?.(),
          ),
          props.open ? h('section', slots.default?.()) : null,
        ]);
    },
  });
  const Empty = defineComponent({
    setup(_, { slots }) {
      return () => h('span', slots.default?.());
    },
  });
  return {
    Button,
    Input,
    Label: Empty,
    Textarea,
    VbenIcon: Empty,
    VbenIconButton: Button,
    VbenPopover,
    VbenSpinner: Empty,
  };
});

function mountInput(options: {
  load?: I18nMessageLoader;
  modelValue?: string;
  rows?: number;
  save?: I18nMessageSaver;
}) {
  const modelValue = ref(options.modelValue ?? '');
  const load =
    options.load ?? vi.fn<I18nMessageLoader>().mockResolvedValue(null);
  const save =
    options.save ??
    vi.fn<I18nMessageSaver>().mockImplementation(async (value) => value);
  const Host = defineComponent({
    setup() {
      return () =>
        h(I18nMessageInput, {
          load,
          modelValue: modelValue.value,
          'onUpdate:modelValue': (value: string) => {
            modelValue.value = value;
          },
          rows: options.rows,
          save,
        });
    },
  });
  const wrapper = mount(Host);
  return { modelValue, wrapper };
}

function buttonByText(wrapper: ReturnType<typeof mount>, text: string) {
  const button = wrapper
    .findAll('button')
    .find((candidate) => candidate.text().includes(text));
  if (!button) throw new Error(`Button not found: ${text}`);
  return button;
}

async function open(wrapper: ReturnType<typeof mount>) {
  await wrapper.get('[data-test="trigger"]').trigger('click');
  await flushPromises();
}

describe('i18n message input', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    dialog.confirm.mockRejectedValue(new Error('keep editing'));
  });

  it('loads the current v-model key every time the popover opens', async () => {
    const load = vi.fn<I18nMessageLoader>().mockResolvedValue({
      messageKey: 'menu.example.title',
      values: [{ locale: 'zh-CN', value: '示例' }],
    });
    const { wrapper } = mountInput({
      load,
      modelValue: 'menu.example.title',
    });

    await open(wrapper);

    expect(load).toHaveBeenCalledTimes(1);
    expect(load).toHaveBeenLastCalledWith('menu.example.title');
    expect(wrapper.get('[data-locale="zh-CN"]').element).toHaveProperty(
      'value',
      '示例',
    );

    await buttonByText(wrapper, 'ui.i18nMessageInput.cancel').trigger('click');
    await open(wrapper);

    expect(load).toHaveBeenCalledTimes(2);
  });

  it('keeps committed values for null and clears them for an empty value list', async () => {
    const load = vi
      .fn<I18nMessageLoader>()
      .mockResolvedValueOnce({
        messageKey: 'menu.example.title',
        values: [{ locale: 'zh-CN', value: '示例' }],
      })
      .mockResolvedValueOnce(null)
      .mockResolvedValueOnce({
        messageKey: 'menu.example.title',
        values: [],
      });
    const { wrapper } = mountInput({
      load,
      modelValue: 'menu.example.title',
    });

    await open(wrapper);
    await buttonByText(wrapper, 'ui.i18nMessageInput.cancel').trigger('click');
    await open(wrapper);

    expect(wrapper.get('[data-locale="zh-CN"]').element).toHaveProperty(
      'value',
      '示例',
    );

    await buttonByText(wrapper, 'ui.i18nMessageInput.cancel').trigger('click');
    await open(wrapper);

    expect(wrapper.get('[data-locale="zh-CN"]').element).toHaveProperty(
      'value',
      '',
    );
  });

  it('opens an empty draft without loading when v-model is empty', async () => {
    const load = vi.fn<I18nMessageLoader>();
    const { wrapper } = mountInput({ load });

    await open(wrapper);

    expect(load).not.toHaveBeenCalled();
    expect(
      wrapper.get('[data-test="i18n-message-key"]').element,
    ).toHaveProperty('value', '');
    expect(wrapper.get('[data-test="i18n-message-key-required"]').text()).toBe(
      '*',
    );
    expect(
      buttonByText(wrapper, 'ui.i18nMessageInput.save').attributes('disabled'),
    ).toBeDefined();
  });

  it('shows the value error directly below the locale inputs', async () => {
    const { wrapper } = mountInput({});
    await open(wrapper);

    expect(
      wrapper.find('[data-test="i18n-message-value-error"]').exists(),
    ).toBe(false);

    await wrapper.get('[data-locale="en-US"]').setValue('x'.repeat(4001));

    const error = wrapper.get('[data-test="i18n-message-value-error"]');
    expect(error.text()).toBe('ui.i18nMessageInput.valueTooLong');
    expect(error.classes()).toContain('mt-1');
  });

  it('uses two rows by default and supports a custom row count', async () => {
    const defaultInput = mountInput({});
    const customInput = mountInput({ rows: 4 });

    await open(defaultInput.wrapper);
    await open(customInput.wrapper);

    expect(
      defaultInput.wrapper.get('[data-locale="zh-CN"]').attributes('rows'),
    ).toBe('2');
    expect(
      customInput.wrapper.get('[data-locale="zh-CN"]').attributes('rows'),
    ).toBe('4');
  });

  it('keeps an existing key read-only and does not leak draft values', async () => {
    const load = vi.fn<I18nMessageLoader>().mockResolvedValue({
      messageKey: 'menu.example.title',
      values: [{ locale: 'zh-CN', value: '旧标题' }],
    });
    const { modelValue, wrapper } = mountInput({
      load,
      modelValue: 'menu.example.title',
    });
    await open(wrapper);

    expect(
      wrapper.get('[data-test="i18n-message-key"]').attributes('disabled'),
    ).toBeDefined();
    await wrapper.get('[data-locale="zh-CN"]').setValue('新标题');

    expect(modelValue.value).toBe('menu.example.title');
  });

  it('closes a clean draft without asking for confirmation', async () => {
    const { wrapper } = mountInput({});
    await open(wrapper);

    await buttonByText(wrapper, 'ui.i18nMessageInput.cancel').trigger('click');
    await flushPromises();

    expect(dialog.confirm).not.toHaveBeenCalled();
    expect(wrapper.find('[data-test="i18n-message-key"]').exists()).toBe(false);
  });

  it('asks before discarding a changed draft', async () => {
    const { wrapper } = mountInput({});
    await open(wrapper);
    await wrapper
      .get('[data-test="i18n-message-key"]')
      .setValue('menu.draft.title');

    await buttonByText(wrapper, 'ui.i18nMessageInput.cancel').trigger('click');
    await flushPromises();

    expect(dialog.confirm).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'ui.i18nMessageInput.unsavedTitle',
      }),
    );
    expect(
      wrapper.get('[data-test="i18n-message-key"]').element,
    ).toHaveProperty('value', 'menu.draft.title');

    dialog.confirm.mockResolvedValueOnce(undefined);
    await buttonByText(wrapper, 'ui.i18nMessageInput.cancel').trigger('click');
    await flushPromises();

    expect(wrapper.find('[data-test="i18n-message-key"]').exists()).toBe(false);
  });

  it('uses the same discard confirmation when the popover requests closing', async () => {
    const { wrapper } = mountInput({});
    await open(wrapper);
    await wrapper
      .get('[data-test="i18n-message-key"]')
      .setValue('menu.draft.title');

    wrapper
      .getComponent({ name: 'VbenPopover' })
      .vm.$emit('update:open', false);
    await flushPromises();

    expect(dialog.confirm).toHaveBeenCalledOnce();
    expect(wrapper.find('[data-test="i18n-message-key"]').exists()).toBe(true);
  });

  it('saves the internal draft and updates v-model with the saved key', async () => {
    const save = vi
      .fn<I18nMessageSaver>()
      .mockImplementation(async (input) => input);
    const { modelValue, wrapper } = mountInput({ save });
    await open(wrapper);
    await wrapper
      .get('[data-test="i18n-message-key"]')
      .setValue('menu.new.title');
    await wrapper.get('[data-locale="zh-CN"]').setValue('新标题');
    await wrapper.get('[data-locale="en-US"]').setValue('New title');

    await buttonByText(wrapper, 'ui.i18nMessageInput.save').trigger('click');
    await flushPromises();

    expect(save).toHaveBeenCalledWith({
      messageKey: 'menu.new.title',
      values: [
        { locale: 'zh-CN', value: '新标题' },
        { locale: 'en-US', value: 'New title' },
      ],
    });
    expect(modelValue.value).toBe('menu.new.title');
    expect(wrapper.find('[data-test="i18n-message-key"]').exists()).toBe(false);
  });

  it('keeps the draft open when saving fails', async () => {
    const save = vi
      .fn<I18nMessageSaver>()
      .mockRejectedValue(new Error('save failed'));
    const { modelValue, wrapper } = mountInput({ save });
    await open(wrapper);
    await wrapper
      .get('[data-test="i18n-message-key"]')
      .setValue('menu.draft.title');

    await buttonByText(wrapper, 'ui.i18nMessageInput.save').trigger('click');
    await flushPromises();

    expect(modelValue.value).toBe('');
    expect(
      wrapper.get('[data-test="i18n-message-key"]').element,
    ).toHaveProperty('value', 'menu.draft.title');
  });
});
