import type { Ref } from 'vue';

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { I18nMessageApi } from '#/api';

import { $t, SUPPORTED_LOCALES } from '#/locales';

export function useGridFormSchema(categories: Ref<string[]>): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => ({
        clearable: true,
        options: categories.value.map((value) => ({ label: value, value })),
      }),
      fieldName: 'category',
      label: $t('page.i18nMessage.filters.category'),
    },
    {
      component: 'Input',
      fieldName: 'key',
      label: $t('page.i18nMessage.filters.key'),
    },
    {
      component: 'Select',
      componentProps: {
        clearable: true,
        options: SUPPORTED_LOCALES.map((value) => ({ label: value, value })),
      },
      fieldName: 'locale',
      label: $t('page.i18nMessage.filters.locale'),
    },
    {
      component: 'Input',
      fieldName: 'value',
      label: $t('page.i18nMessage.filters.value'),
    },
  ];
}

export function useColumns(): VxeTableGridColumns<I18nMessageApi.MessageItem> {
  return [
    {
      field: 'category',
      slots: { default: 'category' },
      title: $t('page.i18nMessage.table.category'),
      width: 130,
    },
    {
      field: 'messageKey',
      minWidth: 260,
      slots: { default: 'messageKey' },
      title: $t('page.i18nMessage.table.key'),
    },
    ...SUPPORTED_LOCALES.map((locale) => ({
      field: locale,
      minWidth: 220,
      slots: { default: locale },
      title: locale,
    })),
    {
      align: 'center' as const,
      field: 'operation',
      fixed: 'right' as const,
      slots: { default: 'action' },
      title: $t('page.i18nMessage.table.operations'),
      width: 150,
    },
  ];
}
