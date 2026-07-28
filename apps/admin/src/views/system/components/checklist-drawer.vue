<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { isEqual } from '@vben/utils';

import {
  ElCheckbox,
  ElCheckboxGroup,
  ElEmpty,
  ElInput,
  ElTag,
} from 'element-plus';

import { $t } from '#/locales';

import { confirmDrawerClose } from './dirty';

/** 多选清单中的单个候选项。 */
export interface ChecklistOption {
  description?: string;
  disabled?: boolean;
  label: string;
  value: string;
}

/** 打开清单 Drawer 时由业务页面传入的加载和保存配置。 */
export interface ChecklistDrawerData {
  load: () => Promise<{
    options: ChecklistOption[];
    selected: string[];
  }>;
  save: (selected: string[]) => Promise<void>;
  title: string;
}

const emit = defineEmits<{ success: [] }>();

const filter = ref('');
const options = ref<ChecklistOption[]>([]);
const selected = ref<string[]>([]);
// 保存打开时的选择快照，用于判断是否存在未保存修改。
const initialSelected = ref<string[]>([]);
const payload = ref<ChecklistDrawerData>();
const saved = ref(false);

const filteredOptions = computed(() => {
  const keyword = filter.value.trim().toLocaleLowerCase();
  if (!keyword) return options.value;
  return options.value.filter((option) =>
    `${option.label} ${option.value} ${option.description ?? ''}`
      .toLocaleLowerCase()
      .includes(keyword),
  );
});

const selectedOptions = computed(() => {
  const selectedSet = new Set(selected.value);
  return options.value.filter((option) => selectedSet.has(option.value));
});

// 去重并排序，避免仅顺序变化被误判为修改。
function normalized(values: string[]) {
  return [...new Set(values)].toSorted();
}

const [Drawer, drawerApi] = useVbenDrawer({
  async onBeforeClose() {
    return (
      saved.value ||
      (await confirmDrawerClose(
        !isEqual(normalized(selected.value), initialSelected.value),
      ))
    );
  },
  async onConfirm() {
    if (!payload.value) return;
    drawerApi.lock();
    try {
      await payload.value.save(normalized(selected.value));
      initialSelected.value = normalized(selected.value);
      saved.value = true;
      emit('success');
      await drawerApi.close();
    } finally {
      drawerApi.unlock();
    }
  },
  async onOpenChange(open) {
    if (!open) return;
    saved.value = false;
    filter.value = '';
    options.value = [];
    selected.value = [];
    initialSelected.value = [];
    payload.value = drawerApi.getData<ChecklistDrawerData>();
    drawerApi.setState({ loading: true, title: payload.value.title });
    try {
      const result = await payload.value.load();
      options.value = result.options;
      selected.value = normalized(result.selected);
      initialSelected.value = normalized(result.selected);
    } finally {
      drawerApi.setState({ loading: false });
    }
  },
});

function remove(option: ChecklistOption) {
  if (option.disabled) return;
  selected.value = selected.value.filter((value) => value !== option.value);
}
</script>

<template>
  <Drawer
    content-class="flex h-full min-h-0 flex-col"
    class="w-full sm:max-w-2xl"
  >
    <!-- 已选项概览。 -->
    <section class="shrink-0 border-b border-border pb-4">
      <div class="mb-3 flex items-center justify-between gap-3">
        <h3 class="text-sm font-medium">
          {{ $t('page.rbacCommon.selected') }}
        </h3>
        <span class="text-xs text-muted-foreground">
          {{ selected.length }} / {{ options.length }}
        </span>
      </div>
      <div v-if="selectedOptions.length" class="flex flex-wrap gap-2">
        <ElTag
          v-for="option in selectedOptions"
          :key="option.value"
          :closable="!option.disabled"
          effect="plain"
          @close="remove(option)"
        >
          {{ option.label }}
          <code class="ml-1 text-xs opacity-70">{{ option.value }}</code>
        </ElTag>
      </div>
      <span v-else class="text-sm text-muted-foreground">
        {{ $t('page.rbacCommon.noneSelected') }}
      </span>
    </section>

    <!-- 支持关键词过滤的候选项列表。 -->
    <section class="flex min-h-0 flex-1 flex-col pt-4">
      <ElInput
        v-model="filter"
        clearable
        :placeholder="$t('page.rbacCommon.filterPlaceholder')"
        class="mb-4 shrink-0"
      >
        <template #prefix>
          <IconifyIcon icon="lucide:search" class="size-4" />
        </template>
      </ElInput>

      <div class="min-h-0 flex-1 overflow-y-auto pr-1">
        <ElCheckboxGroup
          v-if="filteredOptions.length"
          v-model="selected"
          class="grid grid-cols-1 gap-2 sm:grid-cols-2"
        >
          <ElCheckbox
            v-for="option in filteredOptions"
            :key="option.value"
            :value="option.value"
            :disabled="option.disabled"
            border
            class="m-0! h-auto! min-h-14! w-full! px-3! py-2!"
          >
            <span class="block min-w-0 py-0.5">
              <span class="block truncate text-sm font-medium">
                {{ option.label }}
              </span>
              <code class="block truncate text-xs text-muted-foreground">
                {{ option.value }}
              </code>
              <span
                v-if="option.description"
                class="mt-1 block line-clamp-2 text-xs text-muted-foreground"
              >
                {{ option.description }}
              </span>
            </span>
          </ElCheckbox>
        </ElCheckboxGroup>
        <ElEmpty
          v-else
          :description="$t('page.rbacCommon.noMatches')"
          :image-size="72"
        />
      </div>
    </section>
  </Drawer>
</template>
