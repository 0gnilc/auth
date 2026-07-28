/* eslint-disable vue/one-component-per-file -- Inline stubs keep this component test self-contained. */

import type { I18nMessageLoader, I18nMessageSaver } from '../types';

import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, h, nextTick, ref } from 'vue';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import I18nMessageInput from '../i18n-message-input.vue';

const dialog = vi.hoisted(() => ({
  confirm: vi.fn(),
}));

vi.mock('@vben/locales', () => ({
  $t: (key: string) => key,
  i18n: { global: { locale: { value: 'zh-CN' } } },
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
    props: {
      contentProps: { default: undefined, type: Object },
      open: Boolean,
    },
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
  size?: 'default' | 'large' | 'small';
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
          size: options.size,
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

  it('keeps current values when lookup returns null or an empty value list', async () => {
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
      '示例',
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

  it('keeps the key input and lookup action as separate grouped controls', async () => {
    const { wrapper } = mountInput({});
    await open(wrapper);

    const group = wrapper.get('[data-test="i18n-message-key-group"]');
    expect(group.element.children).toHaveLength(2);
    expect(group.element.children[0]).toBe(
      wrapper.get('[data-test="i18n-message-key"]').element,
    );
    expect(group.element.children[1]).toBe(
      wrapper.get('[data-test="i18n-message-search"]').element,
    );
    expect(
      wrapper.get('[data-test="i18n-message-search"]').classes(),
    ).toContain('cursor-pointer');
    expect(
      wrapper.get('[data-test="i18n-message-search"]').classes(),
    ).toContain('disabled:pointer-events-auto');
  });

  it('applies one size to the external input and popover editors', async () => {
    const defaultInput = mountInput({});
    const smallInput = mountInput({ size: 'small' });
    const largeInput = mountInput({ size: 'large' });

    expect(defaultInput.wrapper.get('[role="combobox"]').classes()).toContain(
      'h-[32px]',
    );
    expect(smallInput.wrapper.get('[role="combobox"]').classes()).toContain(
      'h-[24px]',
    );
    expect(largeInput.wrapper.get('[role="combobox"]').classes()).toContain(
      'h-[40px]',
    );

    await open(defaultInput.wrapper);
    await open(smallInput.wrapper);
    await open(largeInput.wrapper);

    expect(
      defaultInput.wrapper
        .get('[data-test="i18n-message-key-group"]')
        .classes(),
    ).toContain('h-[32px]');
    expect(
      smallInput.wrapper.get('[data-test="i18n-message-key-group"]').classes(),
    ).toContain('h-[24px]');
    expect(
      largeInput.wrapper.get('[data-test="i18n-message-key-group"]').classes(),
    ).toContain('h-[40px]');
    expect(
      defaultInput.wrapper.get('[data-test="i18n-message-search"]').classes(),
    ).toContain('w-[32px]');
    expect(smallInput.wrapper.get('[data-locale="en-US"]').classes()).toContain(
      'text-xs',
    );
    expect(largeInput.wrapper.get('[data-locale="en-US"]').classes()).toContain(
      'text-sm',
    );
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

  it('does not override the shared popover and alert stacking levels', async () => {
    const { wrapper } = mountInput({});

    expect(
      wrapper.getComponent({ name: 'VbenPopover' }).props('contentProps'),
    ).not.toHaveProperty('style');

    await open(wrapper);
    await wrapper
      .get('[data-test="i18n-message-key"]')
      .setValue('menu.layered.title');
    await buttonByText(wrapper, 'ui.i18nMessageInput.cancel').trigger('click');
    await flushPromises();

    expect(dialog.confirm).toHaveBeenCalledOnce();
    expect(dialog.confirm.mock.calls[0]?.[0]).not.toHaveProperty(
      'containerClass',
    );
  });

  it('keeps an existing key editable and only looks up a changed key on demand', async () => {
    const load = vi
      .fn<I18nMessageLoader>()
      .mockResolvedValueOnce({
        messageKey: 'menu.example.title',
        values: [
          { locale: 'zh-CN', value: '旧标题' },
          { locale: 'en-US', value: 'Old title' },
        ],
      })
      .mockResolvedValueOnce({
        messageKey: 'menu.other.title',
        values: [{ locale: 'en-US', value: 'Other title' }],
      });
    const { modelValue, wrapper } = mountInput({
      load,
      modelValue: 'menu.example.title',
    });
    await open(wrapper);

    expect(
      wrapper.get('[data-test="i18n-message-key"]').attributes('disabled'),
    ).toBeUndefined();
    await wrapper
      .get('[data-test="i18n-message-key"]')
      .setValue('menu.other.title');

    expect(load).toHaveBeenCalledTimes(1);
    expect(wrapper.get('[data-locale="zh-CN"]').element).toHaveProperty(
      'value',
      '旧标题',
    );

    await wrapper.get('[data-test="i18n-message-search"]').trigger('click');
    await flushPromises();

    expect(load).toHaveBeenLastCalledWith('menu.other.title');
    expect(wrapper.get('[data-locale="zh-CN"]').element).toHaveProperty(
      'value',
      '',
    );
    expect(wrapper.get('[data-locale="en-US"]').element).toHaveProperty(
      'value',
      'Other title',
    );

    expect(modelValue.value).toBe('menu.example.title');
  });

  it('switches a non-empty lookup to reset without requiring another lookup', async () => {
    const load = vi.fn<I18nMessageLoader>().mockResolvedValue({
      messageKey: 'menu.example.title',
      values: [
        { locale: 'en-US', value: 'Example' },
        { locale: 'zh-CN', value: '示例' },
      ],
    });
    const { wrapper } = mountInput({ load });
    await open(wrapper);
    await wrapper
      .get('[data-test="i18n-message-key"]')
      .setValue('menu.example.title');

    const action = wrapper.get('[data-test="i18n-message-search"]');
    await action.trigger('click');
    await flushPromises();

    expect(action.attributes('title')).toBe('ui.i18nMessageInput.reset');
    expect(wrapper.get('[data-locale="en-US"]').element).toHaveProperty(
      'value',
      'Example',
    );

    await action.trigger('click');

    expect(action.attributes('title')).toBe('ui.i18nMessageInput.search');
    expect(
      wrapper.get('[data-test="i18n-message-key"]').element,
    ).toHaveProperty('value', 'menu.example.title');
    expect(wrapper.get('[data-locale="en-US"]').element).toHaveProperty(
      'value',
      '',
    );
    expect(wrapper.get('[data-locale="zh-CN"]').element).toHaveProperty(
      'value',
      '',
    );

    await wrapper.get('[data-locale="en-US"]').setValue('Replacement');
    expect(
      buttonByText(wrapper, 'ui.i18nMessageInput.save').attributes('disabled'),
    ).toBeUndefined();
  });

  it('keeps the lookup action in search mode for an empty result', async () => {
    const load = vi.fn<I18nMessageLoader>().mockResolvedValue(null);
    const { wrapper } = mountInput({ load });
    await open(wrapper);
    await wrapper
      .get('[data-test="i18n-message-key"]')
      .setValue('menu.missing.title');
    await wrapper.get('[data-locale="en-US"]').setValue('Draft');

    const action = wrapper.get('[data-test="i18n-message-search"]');
    await action.trigger('click');
    await flushPromises();

    expect(action.attributes('title')).toBe('ui.i18nMessageInput.search');
    expect(wrapper.get('[data-locale="en-US"]').element).toHaveProperty(
      'value',
      'Draft',
    );
  });

  it('requires en-US and a successful manual lookup after the key changes', async () => {
    const load = vi.fn<I18nMessageLoader>().mockResolvedValue(null);
    const { wrapper } = mountInput({ load });
    await open(wrapper);
    await wrapper
      .get('[data-test="i18n-message-key"]')
      .setValue('menu.new.title');
    await wrapper.get('[data-locale="en-US"]').setValue('New title');

    expect(
      buttonByText(wrapper, 'ui.i18nMessageInput.save').attributes('disabled'),
    ).toBeDefined();

    await wrapper.get('[data-test="i18n-message-search"]').trigger('click');
    await flushPromises();

    expect(
      buttonByText(wrapper, 'ui.i18nMessageInput.save').attributes('disabled'),
    ).toBeUndefined();

    await wrapper.get('[data-locale="en-US"]').setValue('   ');
    expect(wrapper.get('[data-test="i18n-message-value-error"]').text()).toBe(
      'ui.i18nMessageInput.fallbackRequired',
    );
  });

  it('shows the active locale, then en-US, then the Message Key', async () => {
    const load = vi.fn<I18nMessageLoader>().mockResolvedValue({
      messageKey: 'menu.example.title',
      values: [{ locale: 'en-US', value: 'Example' }],
    });
    const { wrapper } = mountInput({
      load,
      modelValue: 'menu.example.title',
    });

    await open(wrapper);

    expect(wrapper.get('[role="combobox"]').element).toHaveProperty(
      'value',
      'Example',
    );
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
    await wrapper.get('[data-test="i18n-message-search"]').trigger('click');
    await flushPromises();
    await wrapper.get('[data-locale="zh-CN"]').setValue('  新标题  ');
    await wrapper.get('[data-locale="en-US"]').setValue('  New title  ');

    await buttonByText(wrapper, 'ui.i18nMessageInput.save').trigger('click');
    await flushPromises();

    expect(save).toHaveBeenCalledWith({
      messageKey: 'menu.new.title',
      values: [
        { locale: 'en-US', value: 'New title' },
        { locale: 'zh-CN', value: '新标题' },
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
    await wrapper.get('[data-test="i18n-message-search"]').trigger('click');
    await flushPromises();
    await wrapper.get('[data-locale="en-US"]').setValue('Draft title');

    await buttonByText(wrapper, 'ui.i18nMessageInput.save').trigger('click');
    await flushPromises();

    expect(modelValue.value).toBe('');
    expect(
      wrapper.get('[data-test="i18n-message-key"]').element,
    ).toHaveProperty('value', 'menu.draft.title');
  });

  it('ignores an older lookup response after closing and reopening for another key', async () => {
    let resolveOlder!: (value: {
      messageKey: string;
      values: Array<{ locale: string; value: string }>;
    }) => void;
    const older = new Promise<{
      messageKey: string;
      values: Array<{ locale: string; value: string }>;
    }>((resolve) => {
      resolveOlder = resolve;
    });
    const load = vi
      .fn<I18nMessageLoader>()
      .mockReturnValueOnce(older)
      .mockResolvedValueOnce({
        messageKey: 'menu.new.title',
        values: [{ locale: 'en-US', value: 'New title' }],
      });
    const { modelValue, wrapper } = mountInput({
      load,
      modelValue: 'menu.old.title',
    });

    await open(wrapper);
    wrapper
      .getComponent({ name: 'VbenPopover' })
      .vm.$emit('update:open', false);
    await flushPromises();
    modelValue.value = 'menu.new.title';
    await nextTick();
    await open(wrapper);

    expect(wrapper.get('[role="combobox"]').element).toHaveProperty(
      'value',
      'New title',
    );

    resolveOlder({
      messageKey: 'menu.old.title',
      values: [{ locale: 'en-US', value: 'Old title' }],
    });
    await flushPromises();

    expect(wrapper.get('[role="combobox"]').element).toHaveProperty(
      'value',
      'New title',
    );
  });

  it('clears a lookup failure when the user changes the key', async () => {
    const load = vi
      .fn<I18nMessageLoader>()
      .mockRejectedValueOnce(new Error('unavailable'))
      .mockResolvedValueOnce(null);
    const { wrapper } = mountInput({
      load,
      modelValue: 'menu.failed.title',
    });
    await open(wrapper);

    expect(wrapper.text()).toContain('ui.i18nMessageInput.loadError');
    await wrapper
      .get('[data-test="i18n-message-key"]')
      .setValue('menu.retry.title');

    expect(wrapper.text()).not.toContain('ui.i18nMessageInput.loadError');
    await wrapper.get('[data-test="i18n-message-search"]').trigger('click');
    await flushPromises();

    expect(load).toHaveBeenLastCalledWith('menu.retry.title');
  });

  it('prevents duplicate saves while the first save is pending', async () => {
    let resolveSave!: (value: {
      messageKey: string;
      values: Array<{ locale: string; value: string }>;
    }) => void;
    const save = vi.fn<I18nMessageSaver>().mockImplementation(
      (input) =>
        new Promise((resolve) => {
          resolveSave = () => resolve(input);
        }),
    );
    const { wrapper } = mountInput({ save });
    await open(wrapper);
    await wrapper
      .get('[data-test="i18n-message-key"]')
      .setValue('menu.pending.title');
    await wrapper.get('[data-test="i18n-message-search"]').trigger('click');
    await flushPromises();
    await wrapper.get('[data-locale="en-US"]').setValue('Pending title');

    const saveButton = buttonByText(wrapper, 'ui.i18nMessageInput.save');
    await saveButton.trigger('click');
    await saveButton.trigger('click');

    expect(save).toHaveBeenCalledOnce();
    expect(saveButton.attributes('disabled')).toBeDefined();

    resolveSave({ messageKey: 'menu.pending.title', values: [] });
    await flushPromises();
  });
});
