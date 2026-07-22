<script setup lang="ts">
import type {
  I18nMessageConfirm,
  I18nMessageInputExpose,
  I18nMessageInputTexts,
} from '@vben/common-ui';

import type { I18nMessageApi } from '#/api';

import { computed, nextTick, onMounted, reactive, ref } from 'vue';

import {
  I18nMessageInput,
  Page,
  VbenButton,
  VbenIconButton,
} from '@vben/common-ui';
import { Eraser, MessageSquareCode, Plus, RotateCw, Search } from '@vben/icons';

import {
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import {
  getI18nMessagePage,
  getI18nMessageValues,
  removeI18nMessage,
  saveI18nMessage,
} from '#/api';
import { $t, ensureStaticKeys, SUPPORTED_LOCALES } from '#/locales';
import { reloadDynamicMessages } from '#/locales/dynamic';

const ADMIN_CLIENT = 'admin';
const DEFAULT_PAGE_SIZE = 10;

const loading = ref(false);
const removingKey = ref('');
const editor = ref<I18nMessageInputExpose>();
const editorKey = ref('');
const editorValue = ref('');
const rows = ref<I18nMessageApi.PageItem[]>([]);
const total = ref(0);

const query = reactive({
  client: ADMIN_CLIENT,
  currentPage: 1,
  key: '',
  locale: '',
  pageSize: DEFAULT_PAGE_SIZE,
  value: '',
});

const editorTexts = computed<I18nMessageInputTexts>(() => ({
  cancel: $t('page.i18nMessage.editor.cancel'),
  confirm: $t('page.i18nMessage.editor.confirm'),
  discard: $t('page.i18nMessage.editor.discard'),
  discardDescription: $t('page.i18nMessage.editor.discardDescription'),
  discardTitle: $t('page.i18nMessage.editor.discardTitle'),
  i18nKey: $t('page.i18nMessage.editor.key'),
  keyInvalid: $t('page.i18nMessage.editor.keyInvalid'),
  keyPlaceholder: $t('page.i18nMessage.editor.keyPlaceholder'),
  keyRequired: $t('page.i18nMessage.editor.keyRequired'),
  keyReserved: $t('page.i18nMessage.editor.keyReserved'),
  keyTooLong: $t('page.i18nMessage.editor.keyTooLong'),
  loadError: $t('page.i18nMessage.editor.loadError'),
  loading: $t('page.i18nMessage.editor.loading'),
  keepEditing: $t('page.i18nMessage.editor.keepEditing'),
  retry: $t('page.i18nMessage.editor.retry'),
  valuePlaceholder: $t('page.i18nMessage.editor.valuePlaceholder'),
  valueTooLong: $t('page.i18nMessage.editor.valueTooLong'),
}));

function valueFor(row: I18nMessageApi.PageItem, locale: string) {
  return row.values.find((item) => item.locale === locale)?.value ?? '';
}

function toPageItem(row: unknown) {
  return row as I18nMessageApi.PageItem;
}

async function loadPage() {
  loading.value = true;
  try {
    const result = await getI18nMessagePage({ ...query });
    rows.value = result.list;
    total.value = result.totalCount;
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  query.currentPage = 1;
  await loadPage();
}

async function handleReset() {
  Object.assign(query, {
    client: ADMIN_CLIENT,
    currentPage: 1,
    key: '',
    locale: '',
    pageSize: DEFAULT_PAGE_SIZE,
    value: '',
  });
  await loadPage();
}

async function openEditor(i18nKey = '', value = '') {
  editorKey.value = i18nKey;
  editorValue.value = value;
  await nextTick();
  try {
    await editor.value?.open();
  } catch {
    // I18nMessageInput renders the recoverable load failure state.
  }
}

async function refreshAfterMutation(successMessage: string) {
  let runtimeReloadFailed = false;
  let listReloadFailed = false;
  try {
    await reloadDynamicMessages();
  } catch {
    runtimeReloadFailed = true;
  }
  try {
    await loadPage();
  } catch {
    listReloadFailed = true;
  }

  if (listReloadFailed) {
    ElMessage.warning($t('page.i18nMessage.messages.listReloadFailed'));
  }
  if (runtimeReloadFailed) {
    ElMessage.warning($t('page.i18nMessage.messages.runtimeReloadFailed'));
  } else {
    ElMessage.success(successMessage);
  }
}

const confirmMessage: I18nMessageConfirm = async (input) => {
  const staticKeys = await ensureStaticKeys();
  if (staticKeys.has(input.i18nKey)) {
    ElMessage.error($t('page.i18nMessage.messages.staticKey'));
    throw new Error(
      'Static internationalization keys cannot be saved dynamically.',
    );
  }

  const saved = await saveI18nMessage(input);
  await refreshAfterMutation($t('page.i18nMessage.messages.saveSuccess'));
  return saved;
};

async function handleRemove(row: I18nMessageApi.PageItem) {
  try {
    await ElMessageBox.confirm(
      $t('page.i18nMessage.messages.removeConfirm', { key: row.i18nKey }),
      $t('page.i18nMessage.messages.removeTitle'),
      {
        confirmButtonText: $t('page.i18nMessage.actions.remove'),
        type: 'warning',
      },
    );
  } catch {
    return;
  }

  removingKey.value = row.i18nKey;
  try {
    await removeI18nMessage(row.i18nKey);
    await refreshAfterMutation($t('page.i18nMessage.messages.removeSuccess'));
  } finally {
    removingKey.value = '';
  }
}

onMounted(loadPage);
</script>

<template>
  <Page
    :title="$t('page.i18nMessage.title')"
    auto-content-height
    content-class="p-0"
  >
    <div class="flex h-full min-h-0 flex-col bg-background">
      <form
        class="grid shrink-0 gap-3 border-b border-border px-4 py-4 md:grid-cols-2 xl:grid-cols-[minmax(180px,1fr)_minmax(180px,1fr)_180px_180px_auto]"
        @submit.prevent="handleSearch"
      >
        <ElInput
          v-model="query.key"
          clearable
          :placeholder="$t('page.i18nMessage.filters.keyPlaceholder')"
          :aria-label="$t('page.i18nMessage.filters.key')"
        />
        <ElInput
          v-model="query.value"
          clearable
          :placeholder="$t('page.i18nMessage.filters.valuePlaceholder')"
          :aria-label="$t('page.i18nMessage.filters.value')"
        />
        <ElInput
          v-model="query.client"
          :aria-label="$t('page.i18nMessage.filters.client')"
        />
        <ElSelect
          v-model="query.locale"
          :placeholder="$t('page.i18nMessage.filters.locale')"
          :aria-label="$t('page.i18nMessage.filters.locale')"
        >
          <ElOption
            :label="$t('page.i18nMessage.filters.allLocales')"
            value=""
          />
          <ElOption
            v-for="locale in SUPPORTED_LOCALES"
            :key="locale"
            :label="locale"
            :value="locale"
          />
        </ElSelect>
        <div
          class="flex items-center justify-end gap-2 md:col-span-2 xl:col-span-1"
        >
          <VbenButton type="submit" size="sm">
            <Search class="mr-2 size-4" />
            {{ $t('page.i18nMessage.actions.search') }}
          </VbenButton>
          <VbenButton
            type="button"
            size="sm"
            variant="outline"
            @click="handleReset"
          >
            {{ $t('page.i18nMessage.actions.reset') }}
          </VbenButton>
        </div>
      </form>

      <div
        class="flex shrink-0 flex-col gap-3 border-b border-border px-4 py-3 lg:flex-row lg:items-center lg:justify-between"
      >
        <div class="w-full min-w-0 lg:max-w-md">
          <I18nMessageInput
            ref="editor"
            v-model="editorValue"
            v-model:i18n-key="editorKey"
            :confirm="confirmMessage"
            :load="getI18nMessageValues"
            :locales="SUPPORTED_LOCALES"
            :texts="editorTexts"
            position="center"
            :placeholder="$t('page.i18nMessage.editor.placeholder')"
          />
        </div>
        <div class="flex shrink-0 items-center justify-end gap-2">
          <VbenButton type="button" size="sm" @click="openEditor()">
            <Plus class="mr-2 size-4" />
            {{ $t('page.i18nMessage.actions.create') }}
          </VbenButton>
          <VbenIconButton
            :tooltip="$t('page.i18nMessage.actions.reload')"
            class="size-9 rounded-md"
            @click="loadPage"
          >
            <RotateCw class="size-4" />
          </VbenIconButton>
        </div>
      </div>

      <div class="min-h-0 flex-1 overflow-auto px-4 py-3">
        <ElTable
          v-loading="loading"
          :data="rows"
          height="100%"
          table-layout="fixed"
        >
          <ElTableColumn
            prop="client"
            :label="$t('page.i18nMessage.table.client')"
            width="120"
          >
            <template #default="{ row }">
              <ElTag effect="plain" size="small">{{ row.client }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn
            prop="i18nKey"
            :label="$t('page.i18nMessage.table.key')"
            min-width="260"
          >
            <template #default="{ row }">
              <code class="break-all text-xs">{{ row.i18nKey }}</code>
            </template>
          </ElTableColumn>
          <ElTableColumn
            v-for="locale in SUPPORTED_LOCALES"
            :key="locale"
            :label="locale"
            min-width="220"
          >
            <template #default="{ row }">
              <span class="line-clamp-2 break-words">{{
                valueFor(toPageItem(row), locale)
              }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn
            fixed="right"
            :label="$t('page.i18nMessage.table.operations')"
            width="112"
            align="center"
          >
            <template #default="{ row }">
              <div class="flex justify-center gap-1">
                <VbenIconButton
                  :tooltip="$t('page.i18nMessage.actions.edit')"
                  class="size-8 rounded-md"
                  @click="
                    openEditor(row.i18nKey, valueFor(toPageItem(row), 'zh-CN'))
                  "
                >
                  <MessageSquareCode class="size-4" />
                </VbenIconButton>
                <VbenIconButton
                  :tooltip="$t('page.i18nMessage.actions.remove')"
                  class="size-8 rounded-md text-destructive"
                  :disabled="removingKey === row.i18nKey"
                  @click="handleRemove(toPageItem(row))"
                >
                  <Eraser class="size-4" />
                </VbenIconButton>
              </div>
            </template>
          </ElTableColumn>
          <template #empty>
            <span class="text-muted-foreground">{{
              $t('page.i18nMessage.table.empty')
            }}</span>
          </template>
        </ElTable>
      </div>

      <div class="flex shrink-0 justify-end border-t border-border px-4 py-3">
        <ElPagination
          v-model:current-page="query.currentPage"
          v-model:page-size="query.pageSize"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @current-change="loadPage"
          @size-change="handleSearch"
        />
      </div>
    </div>
  </Page>
</template>
