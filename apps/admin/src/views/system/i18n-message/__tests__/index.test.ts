import type { I18nMessageConfirm } from '@vben/common-ui';

import { flushPromises, mount } from '@vue/test-utils';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import I18nMessageManagementPage from '../index.vue';

const api = vi.hoisted(() => ({
  getI18nMessagePage: vi.fn(),
  getI18nMessageValues: vi.fn(),
  removeI18nMessage: vi.fn(),
  saveI18nMessage: vi.fn(),
}));
const locale = vi.hoisted(() => ({
  ensureStaticKeys: vi.fn(),
}));
const runtime = vi.hoisted(() => ({
  reloadDynamicMessages: vi.fn(),
}));
const messages = vi.hoisted(() => ({
  confirm: vi.fn(),
  error: vi.fn(),
  success: vi.fn(),
  warning: vi.fn(),
}));
const editorState = vi.hoisted(() => ({
  openedKeys: [] as string[],
}));

vi.mock('#/api', () => api);
vi.mock('#/locales', () => ({
  $t: (key: string) => key,
  ensureStaticKeys: locale.ensureStaticKeys,
  SUPPORTED_LOCALES: ['zh-CN', 'en-US'],
}));
vi.mock('#/locales/dynamic', () => runtime);

vi.mock('@vben/common-ui', async () => {
  const { defineComponent, h } = await import('vue');
  const Container = defineComponent({
    setup(_, { slots }) {
      return () => h('div', slots.default?.());
    },
  });
  const Button = defineComponent({
    inheritAttrs: false,
    props: { tooltip: String },
    setup(props, { attrs, slots }) {
      return () =>
        h(
          'button',
          { ...attrs, 'data-tooltip': props.tooltip },
          slots.default?.(),
        );
    },
  });
  const I18nMessageInput = defineComponent({
    name: 'I18nMessageInput',
    props: {
      confirm: Function,
      i18nKey: { default: '', type: String },
      load: Function,
    },
    setup(props, { expose }) {
      expose({
        close: vi.fn(),
        open: vi.fn(() => {
          editorState.openedKeys.push(props.i18nKey);
        }),
      });
      return () => h('div', { 'data-test': 'i18n-message-input' });
    },
  });
  return {
    I18nMessageInput,
    Page: Container,
    VbenButton: Button,
    VbenIconButton: Button,
  };
});

vi.mock('@vben/icons', async () => {
  const { defineComponent, h } = await import('vue');
  const Icon = defineComponent({ setup: () => () => h('span') });
  return {
    Eraser: Icon,
    MessageSquareCode: Icon,
    Plus: Icon,
    RotateCw: Icon,
    Search: Icon,
  };
});

vi.mock('element-plus', async () => {
  const { defineComponent, h } = await import('vue');
  let tableRow: unknown;
  const Container = defineComponent({
    setup(_, { slots }) {
      return () => h('div', slots.default?.());
    },
  });
  const Empty = defineComponent({ setup: () => () => h('div') });
  const Table = defineComponent({
    props: { data: Array },
    setup(props, { slots }) {
      return () => {
        tableRow = props.data?.[0];
        return h('div', slots.default?.());
      };
    },
  });
  const TableColumn = defineComponent({
    setup(_, { slots }) {
      return () =>
        h(
          'div',
          tableRow === undefined
            ? undefined
            : slots.default?.({ row: tableRow }),
        );
    },
  });
  return {
    ElInput: Empty,
    ElMessage: {
      error: messages.error,
      success: messages.success,
      warning: messages.warning,
    },
    ElMessageBox: { confirm: messages.confirm },
    ElOption: Empty,
    ElPagination: Empty,
    ElSelect: Container,
    ElTable: Table,
    ElTableColumn: TableColumn,
    ElTag: Container,
  };
});

const input = {
  i18nKey: 'menu.example.title',
  values: [
    { locale: 'zh-CN', value: '示例' },
    { locale: 'en-US', value: 'Example' },
  ],
};

function mountPage() {
  return mount(I18nMessageManagementPage, {
    global: {
      directives: { loading: () => {} },
    },
  });
}

function getConfirm(wrapper: ReturnType<typeof mountPage>) {
  return wrapper
    .getComponent({ name: 'I18nMessageInput' })
    .props('confirm') as I18nMessageConfirm;
}

describe('internationalization message management page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    editorState.openedKeys.length = 0;
    api.getI18nMessagePage.mockResolvedValue({ list: [], totalCount: 0 });
    api.removeI18nMessage.mockResolvedValue(undefined);
    api.saveI18nMessage.mockImplementation(async (value) => value);
    locale.ensureStaticKeys.mockResolvedValue(new Set<string>());
    messages.confirm.mockResolvedValue(undefined);
    runtime.reloadDynamicMessages.mockResolvedValue(undefined);
  });

  it('keeps persistence successful when the runtime bundle reload fails', async () => {
    runtime.reloadDynamicMessages.mockRejectedValueOnce(
      new Error('bundle reload failed'),
    );
    const wrapper = mountPage();
    await flushPromises();

    await expect(getConfirm(wrapper)(input)).resolves.toEqual(input);

    expect(api.saveI18nMessage).toHaveBeenCalledWith(input);
    expect(api.getI18nMessagePage).toHaveBeenCalledTimes(2);
    expect(messages.warning).toHaveBeenCalledWith(
      'page.i18nMessage.messages.runtimeReloadFailed',
    );
    expect(messages.success).not.toHaveBeenCalled();
  });

  it('reports list reload failure without repeating the successful save', async () => {
    api.getI18nMessagePage
      .mockResolvedValueOnce({ list: [], totalCount: 0 })
      .mockRejectedValueOnce(new Error('list reload failed'));
    const wrapper = mountPage();
    await flushPromises();

    await expect(getConfirm(wrapper)(input)).resolves.toEqual(input);

    expect(api.saveI18nMessage).toHaveBeenCalledTimes(1);
    expect(messages.warning).toHaveBeenCalledWith(
      'page.i18nMessage.messages.listReloadFailed',
    );
    expect(messages.success).toHaveBeenCalledWith(
      'page.i18nMessage.messages.saveSuccess',
    );
  });

  it('rejects a local static key before calling the save API', async () => {
    locale.ensureStaticKeys.mockResolvedValue(new Set(['menu.example.title']));
    const wrapper = mountPage();
    await flushPromises();

    await expect(getConfirm(wrapper)(input)).rejects.toThrow(
      'Static internationalization keys cannot be saved dynamically.',
    );

    expect(api.saveI18nMessage).not.toHaveBeenCalled();
    expect(messages.error).toHaveBeenCalledWith(
      'page.i18nMessage.messages.staticKey',
    );
  });

  it('opens row editing only after the selected key reaches the shared editor', async () => {
    api.getI18nMessagePage.mockResolvedValue({
      list: [
        {
          client: 'admin',
          i18nKey: 'menu.row.title',
          values: input.values,
        },
      ],
      totalCount: 1,
    });
    const wrapper = mountPage();
    await flushPromises();

    await wrapper
      .get('[data-tooltip="page.i18nMessage.actions.edit"]')
      .trigger('click');
    await flushPromises();

    expect(editorState.openedKeys).toEqual(['menu.row.title']);
  });

  it('keeps removal successful when the list reload fails', async () => {
    api.getI18nMessagePage
      .mockResolvedValueOnce({
        list: [
          {
            client: 'admin',
            i18nKey: 'menu.row.title',
            values: input.values,
          },
        ],
        totalCount: 1,
      })
      .mockRejectedValueOnce(new Error('list reload failed'));
    const wrapper = mountPage();
    await flushPromises();

    await wrapper
      .get('[data-tooltip="page.i18nMessage.actions.remove"]')
      .trigger('click');
    await flushPromises();

    expect(api.removeI18nMessage).toHaveBeenCalledWith('menu.row.title');
    expect(messages.warning).toHaveBeenCalledWith(
      'page.i18nMessage.messages.listReloadFailed',
    );
    expect(messages.success).toHaveBeenCalledWith(
      'page.i18nMessage.messages.removeSuccess',
    );
  });

  it('treats removal confirmation cancellation as a no-op', async () => {
    messages.confirm.mockRejectedValueOnce(new Error('cancelled'));
    api.getI18nMessagePage.mockResolvedValue({
      list: [
        {
          client: 'admin',
          i18nKey: 'menu.row.title',
          values: input.values,
        },
      ],
      totalCount: 1,
    });
    const wrapper = mountPage();
    await flushPromises();

    await wrapper
      .get('[data-tooltip="page.i18nMessage.actions.remove"]')
      .trigger('click');
    await flushPromises();

    expect(api.removeI18nMessage).not.toHaveBeenCalled();
  });
});
