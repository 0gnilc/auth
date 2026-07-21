import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, h, ref } from 'vue';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import type {
  I18nMessageConfirm,
  I18nMessageInputExpose,
  I18nMessageLoader,
} from '../types';
import I18nMessageInput from '../i18n-message-input.vue';

const dialog = vi.hoisted(() => ({
  confirm: vi.fn(),
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
    },
    emits: ['update:modelValue'],
    setup(props, { attrs, emit }) {
      return () =>
        h('input', {
          ...attrs,
          disabled: props.disabled,
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
    props: { open: Boolean },
    setup(props, { slots }) {
      return () =>
        h('div', [
          slots.trigger?.(),
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
  confirm?: I18nMessageConfirm;
  i18nKey?: string;
  load?: I18nMessageLoader;
  modelValue?: string;
  presetKey?: string;
}) {
  const editor = ref<I18nMessageInputExpose>();
  const i18nKey = ref(options.i18nKey ?? '');
  const modelValue = ref(options.modelValue ?? '');
  const Host = defineComponent({
    setup() {
      return () =>
        h(I18nMessageInput, {
          confirm: options.confirm,
          i18nKey: i18nKey.value,
          load: options.load,
          modelValue: modelValue.value,
          'onUpdate:i18nKey': (value: string) => {
            i18nKey.value = value;
          },
          'onUpdate:modelValue': (value: string) => {
            modelValue.value = value;
          },
          presetKey: options.presetKey,
          ref: editor,
        });
    },
  });
  const wrapper = mount(Host);
  return { editor, i18nKey, modelValue, wrapper };
}

function buttonByText(wrapper: ReturnType<typeof mount>, text: string) {
  const button = wrapper
    .findAll('button')
    .find((candidate) => candidate.text().includes(text));
  if (!button) throw new Error(`Button not found: ${text}`);
  return button;
}

describe('i18n message input', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    dialog.confirm.mockRejectedValue(new Error('dialog cancelled'));
  });

  it('uses presetKey only as an empty draft suggestion and skips loading', async () => {
    const load = vi.fn<I18nMessageLoader>();
    const { editor, i18nKey, wrapper } = mountInput({
      load,
      presetKey: 'menu.suggested.title',
    });

    await editor.value?.open();

    expect(load).not.toHaveBeenCalled();
    expect(wrapper.get('#i18n-message-key').element).toHaveProperty(
      'value',
      'menu.suggested.title',
    );
    expect(i18nKey.value).toBe('');
  });

  it('loads once per opening and distinguishes null from empty values', async () => {
    const load = vi
      .fn<I18nMessageLoader>()
      .mockResolvedValueOnce(null)
      .mockResolvedValueOnce({ i18nKey: 'menu.example.title', values: [] });
    const { editor, modelValue } = mountInput({
      i18nKey: 'menu.example.title',
      load,
      modelValue: 'Opening value',
    });

    await editor.value?.open();
    await editor.value?.open();
    expect(load).toHaveBeenCalledTimes(1);
    expect(modelValue.value).toBe('Opening value');

    editor.value?.close();
    await editor.value?.open();

    expect(load).toHaveBeenCalledTimes(2);
    expect(modelValue.value).toBe('');
  });

  it('submits key and value changes together with the opening key', async () => {
    const confirm = vi
      .fn<I18nMessageConfirm>()
      .mockImplementation(async (input) => ({
        i18nKey: input.i18nKey,
        values: input.values,
      }));
    const load = vi.fn<I18nMessageLoader>().mockResolvedValue({
      i18nKey: 'menu.old.title',
      values: [
        { locale: 'zh-CN', value: '旧标题' },
        { locale: 'en-US', value: 'Old title' },
      ],
    });
    const { editor, wrapper } = mountInput({
      confirm,
      i18nKey: 'menu.old.title',
      load,
    });
    await editor.value?.open();

    await wrapper.get('#i18n-message-key').setValue('menu.new.title');
    await wrapper.get('#i18n-message-zh-CN').setValue('新标题');
    await buttonByText(wrapper, 'Confirm').trigger('click');
    await flushPromises();

    expect(confirm).toHaveBeenCalledWith({
      i18nKey: 'menu.new.title',
      previousKey: 'menu.old.title',
      values: [
        { locale: 'zh-CN', value: '新标题' },
        { locale: 'en-US', value: 'Old title' },
      ],
    });
    expect(wrapper.find('#i18n-message-key').exists()).toBe(false);
  });

  it('keeps the draft open after confirmation fails', async () => {
    const confirm = vi
      .fn<I18nMessageConfirm>()
      .mockRejectedValue(new Error('save failed'));
    const { editor, wrapper } = mountInput({ confirm });
    await editor.value?.open();
    await wrapper.get('#i18n-message-key').setValue('menu.draft.title');
    await wrapper.get('#i18n-message-en-US').setValue('Draft title');

    await buttonByText(wrapper, 'Confirm').trigger('click');
    await flushPromises();

    expect(wrapper.get('#i18n-message-key').element).toHaveProperty(
      'value',
      'menu.draft.title',
    );
    expect(wrapper.get('#i18n-message-en-US').element).toHaveProperty(
      'value',
      'Draft title',
    );
  });

  it('preserves the opening snapshot after load failure and supports retry', async () => {
    const load = vi
      .fn<I18nMessageLoader>()
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce({
        i18nKey: 'menu.retry.title',
        values: [{ locale: 'zh-CN', value: '重试成功' }],
      });
    const { editor, modelValue, wrapper } = mountInput({
      i18nKey: 'menu.retry.title',
      load,
      modelValue: 'Opening value',
    });

    await expect(editor.value?.open()).rejects.toThrow('load failed');
    expect(modelValue.value).toBe('Opening value');
    await buttonByText(wrapper, 'Retry').trigger('click');
    await flushPromises();

    expect(load).toHaveBeenCalledTimes(2);
    expect(modelValue.value).toBe('重试成功');
  });

  it('asks before discarding changes and restores the opening snapshot', async () => {
    const { editor, i18nKey, modelValue, wrapper } = mountInput({
      i18nKey: 'menu.old.title',
      modelValue: 'Old title',
    });
    await editor.value?.open();
    await wrapper.get('#i18n-message-key').setValue('menu.new.title');
    await wrapper.get('#i18n-message-zh-CN').setValue('New title');

    await buttonByText(wrapper, 'Cancel').trigger('click');
    await flushPromises();

    expect(dialog.confirm).toHaveBeenCalledWith(
      expect.objectContaining({
        confirmText: 'Discard',
        content: 'Your unsaved changes will be lost.',
      }),
    );
    expect(wrapper.get('#i18n-message-key').element).toHaveProperty(
      'value',
      'menu.new.title',
    );

    dialog.confirm.mockResolvedValueOnce(undefined);
    await buttonByText(wrapper, 'Cancel').trigger('click');
    await flushPromises();

    expect(wrapper.find('#i18n-message-key').exists()).toBe(false);
    expect(i18nKey.value).toBe('menu.old.title');
    expect(modelValue.value).toBe('Old title');
  });
});
