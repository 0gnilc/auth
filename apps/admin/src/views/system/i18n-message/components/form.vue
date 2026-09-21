<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { I18nMessageApi } from '#/api/system';

import { nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { isEqual, trimToNull } from '@vben/utils';

import { ElMessage } from 'element-plus';

import { confirmDiscardChanges } from '#/adapter/confirm-discard-changes';
import { useVbenForm, z } from '#/adapter/form';
import { createI18nMessage, saveI18nMessage } from '#/api/system';
import { $t } from '#/locales';
import { reloadDynamicMessages } from '#/locales/dynamic';

import {
  I18N_MESSAGE_INPUT_MAX_LENGTH,
  I18N_MESSAGE_MAX_CODE_POINTS,
} from '../validation';

/** 动态消息创建或编辑抽屉的分类候选和当前消息。 */
export interface I18nMessageFormDrawerData {
  /** 当前已有的动态消息分类。 */
  categories: string[];
  /** 待编辑的动态消息；省略时创建新消息。 */
  row?: I18nMessageApi.MessageItem;
}

/** 动态消息编辑草稿，消息键身份独立于分类。 */
interface MessageForm {
  /** 动态消息的管理分类，不参与消息键身份。 */
  category: string;
  /** 是否编辑已有消息，用于限制更改消息键身份。 */
  editing: boolean;
  /** 英语消息文本。 */
  enUS: string;
  /** 跨语言、跨分类唯一的动态消息键。 */
  messageKey: string;
  /** 可选的简体中文消息文本。 */
  zhCN?: string;
}

const emit = defineEmits<{ success: [] }>();

const categories = ref<string[]>([]);
const initialValues = ref<MessageForm>();
const saved = ref(false);

const messageKeyRule = z
  .string()
  .trim()
  .min(1, { message: $t('i18nMessage.validation.keyRequired') })
  .max(191, { message: $t('i18nMessage.validation.keyTooLong') })
  .regex(/^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)*$/, {
    message: $t('i18nMessage.validation.keyInvalid'),
  });

const schema: VbenFormSchema<MessageForm>[] = [
  { component: 'Input', fieldName: 'editing', hide: true },
  {
    component: 'Select',
    componentProps: () => ({
      options: categories.value.map((value) => ({ label: value, value })),
    }),
    fieldName: 'category',
    label: $t('i18nMessage.form.category'),
    rules: 'selectRequired',
  },
  {
    component: 'Input',
    dependencies: {
      resolve: ({ values }) => ({
        componentProps: { disabled: Boolean(values.editing) },
      }),
      triggerFields: ['editing'],
    },
    fieldName: 'messageKey',
    label: $t('i18nMessage.form.messageKey'),
    rules: messageKeyRule,
  },
  {
    component: 'Input',
    componentProps: {
      maxlength: I18N_MESSAGE_INPUT_MAX_LENGTH,
      rows: 4,
      type: 'textarea',
    },
    fieldName: 'enUS',
    label: 'en-US',
    rules: z
      .string()
      .trim()
      .min(1, { message: $t('i18nMessage.validation.enRequired') })
      .refine((value) => [...value].length <= I18N_MESSAGE_MAX_CODE_POINTS, {
        message: $t('i18nMessage.validation.valueTooLong'),
      }),
  },
  {
    component: 'Input',
    componentProps: {
      maxlength: I18N_MESSAGE_INPUT_MAX_LENGTH,
      rows: 4,
      type: 'textarea',
    },
    fieldName: 'zhCN',
    label: 'zh-CN',
    rules: z
      .string()
      .trim()
      .refine((value) => [...value].length <= I18N_MESSAGE_MAX_CODE_POINTS, {
        message: $t('i18nMessage.validation.valueTooLong'),
      })
      .optional(),
  },
];

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' } },
  schema,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1',
});

function valueFor(row: I18nMessageApi.MessageItem, locale: string) {
  return row.values.find((item) => item.locale === locale)?.value ?? '';
}

const [Drawer, drawerApi] = useVbenDrawer({
  async onBeforeClose() {
    if (saved.value) return true;
    return confirmDiscardChanges(
      !isEqual(await formApi.getValues(), initialValues.value),
    );
  },
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues<MessageForm>();
    trimToNull(values);
    drawerApi.lock();
    try {
      const persistMessage = values.editing
        ? saveI18nMessage
        : createI18nMessage;
      try {
        await persistMessage({
          category: values.category,
          messageKey: values.messageKey,
          values: [
            { locale: 'en-US', value: values.enUS },
            { locale: 'zh-CN', value: values.zhCN ?? '' },
          ],
        });
      } catch {
        return;
      }
      // 持久化已成功；后续运行时语言包刷新失败只提示刷新问题，不能把已保存的消息当作未保存。
      saved.value = true;
      ElMessage.success($t('i18nMessage.messages.saveSuccess'));
      if (
        values.category === 'admin' ||
        initialValues.value?.category === 'admin'
      ) {
        try {
          await reloadDynamicMessages();
        } catch {
          ElMessage.warning($t('i18nMessage.messages.runtimeReloadFailed'));
        }
      }
      emit('success');
      await drawerApi.close();
    } finally {
      drawerApi.unlock();
    }
  },
  async onOpenChange(open) {
    if (!open) return;
    saved.value = false;
    const payload = drawerApi.getData<I18nMessageFormDrawerData>();
    categories.value = payload.categories;
    const row = payload.row;
    const values: MessageForm = {
      category: row?.category ?? categories.value[0] ?? '',
      editing: !!row,
      enUS: row ? valueFor(row, 'en-US') : '',
      messageKey: row?.messageKey ?? '',
      zhCN: row ? valueFor(row, 'zh-CN') : '',
    };
    drawerApi.setState({
      title: row
        ? $t('i18nMessage.drawer.editTitle')
        : $t('i18nMessage.drawer.createTitle'),
    });
    await formApi.reset();
    await nextTick();
    await formApi.setValues(values, false);
    initialValues.value = await formApi.getValues<MessageForm>();
  },
});
</script>

<template>
  <Drawer class="w-full sm:max-w-2xl">
    <Form />
  </Drawer>
</template>
