<script setup lang="ts">
import type { I18nMessageFormDrawerData } from './modules/form.vue';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { I18nMessageApi } from '#/api';

import { onMounted, ref } from 'vue';

import { Page, useVbenDrawer, VbenButton } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { ElMessage, ElTag } from 'element-plus';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import {
  getI18nMessageCategories,
  getI18nMessagePage,
  removeI18nMessage,
} from '#/api';
import { $t, SUPPORTED_LOCALES } from '#/locales';
import { reloadDynamicMessages } from '#/locales/dynamic';

import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const categories = ref<string[]>([]);

interface I18nMessageTableRow extends I18nMessageApi.MessageItem {
  rowKey: string;
}

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid<I18nMessageTableRow>({
  formOptions: {
    schema: useGridFormSchema(categories),
    submitOnChange: false,
  },
  gridOptions: {
    columns: useColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: {},
    proxyConfig: {
      ajax: {
        async query({ page }, args) {
          const result = await getI18nMessagePage({
            currentPage: page.currentPage,
            pageSize: page.pageSize,
            ...args,
          });
          return {
            list: result.list.map((item) => ({
              ...item,
              rowKey: item.messageKey,
            })),
            total: result.totalCount,
          };
        },
      },
      showLoading: false,
    },
    rowConfig: { keyField: 'rowKey' },
    toolbarConfig: {
      custom: true,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<I18nMessageTableRow>,
});

function valueFor(row: I18nMessageApi.MessageItem, locale: string) {
  return row.values.find((item) => item.locale === locale)?.value ?? '';
}

function openForm(row?: I18nMessageApi.MessageItem) {
  const data: I18nMessageFormDrawerData = {
    categories: categories.value,
    row,
  };
  formDrawerApi.setData(data).open();
}

async function onDelete(row: I18nMessageApi.MessageItem) {
  await removeI18nMessage(row.messageKey);
  ElMessage.success($t('page.i18nMessage.messages.removeSuccess'));
  if (row.category === 'admin') {
    try {
      await reloadDynamicMessages();
    } catch {
      ElMessage.warning($t('page.i18nMessage.messages.runtimeReloadFailed'));
    }
  }
  await gridApi.query();
}

function refresh() {
  void gridApi.query();
}

onMounted(async () => {
  categories.value = await getI18nMessageCategories();
});
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="refresh" />
    <Grid :table-title="$t('page.i18nMessage.title')">
      <template #toolbar-tools>
        <VbenButton
          v-access:code="'system:i18n-message:save'"
          size="sm"
          :disabled="categories.length === 0"
          @click="openForm()"
        >
          <IconifyIcon icon="lucide:plus" class="mr-2 size-4" />
          {{ $t('page.i18nMessage.actions.create') }}
        </VbenButton>
      </template>

      <template #category="{ row }">
        <ElTag effect="plain">{{ row.category }}</ElTag>
      </template>

      <template #messageKey="{ row }">
        <code class="break-all text-xs">{{ row.messageKey }}</code>
      </template>

      <template
        v-for="locale in SUPPORTED_LOCALES"
        :key="locale"
        #[locale]="{ row }"
      >
        <span class="line-clamp-2 break-words">{{
          valueFor(row, locale)
        }}</span>
      </template>

      <template #action="{ row }">
        <VbenTableAction
          :actions="[
            {
              auth: 'system:i18n-message:save',
              text: $t('page.rbacCommon.edit'),
              onClick: () => openForm(row),
            },
          ]"
          :dropdown-actions="[
            {
              auth: 'system:i18n-message:remove',
              danger: true,
              text: $t('page.i18nMessage.actions.remove'),
              popConfirm: {
                title: $t('page.i18nMessage.messages.removeConfirm', {
                  key: row.messageKey,
                }),
                confirm: () => onDelete(row),
              },
            },
          ]"
          align="center"
        />
      </template>
    </Grid>
  </Page>
</template>
