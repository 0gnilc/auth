<script setup lang="ts">
import type {
  I18nMessage,
  I18nMessageInputProps,
  I18nMessageValue,
} from './types';

import { computed, ref, useId, watch } from 'vue';

import { $t, i18n } from '@vben/locales';

import { confirm as confirmDialog } from '@vben-core/popup-ui';
import {
  Button,
  Input,
  Label,
  Textarea,
  VbenIcon,
  VbenPopover,
  VbenSpinner,
} from '@vben-core/shadcn-ui';

// 未声明属性由组件手动透传给外部展示输入框。
defineOptions({ inheritAttrs: false });

// 合并组件属性与默认配置。
const props = withDefaults(defineProps<I18nMessageInputProps>(), {
  disabled: false,
  locales: () => ['en-US', 'zh-CN'],
  placeholder: '',
  rows: 2,
  size: 'default',
});

// 业务表单双向绑定的 Message Key。
const modelValue = defineModel<string>({ default: '' });
// 关联 Label 与输入控件的组件实例唯一 ID。
const fieldId = useId();
// 浮层是否已打开。
const visible = ref(false);
// 是否正在加载 Message 数据。
const loading = ref(false);
// 是否正在保存当前草稿。
const saving = ref(false);
// 最近一次加载是否失败。
const loadError = ref(false);
// 最近一次成功查询对应的 Message Key；Key 改动后必须重新查询。
const searchedKey = ref<string>();
// 最近一次查询是否返回了可供重置的语言文本。
const canResetLookup = ref(false);
// 浮层内正在编辑的独立草稿。
const draft = ref<I18nMessage>(createMessage());
// 本次打开或加载完成时的初始快照。
const initial = ref<I18nMessage>(createMessage());
// 最近一次加载或保存成功的数据，用于外部输入框显示。
const committed = ref<I18nMessage>();
// 加载序号，用于忽略关闭浮层后返回的过期请求。
let loadSequence = 0;

// 浮层固定显示在外部输入框下方并左对齐。
const contentProps = {
  // 浮层与输入框左边缘对齐。
  align: 'start' as const,
  // 浮层固定展示在输入框下方。
  side: 'bottom' as const,
  // 浮层与输入框保留 6px 间距。
  sideOffset: 6,
};

// 采用与 Element Plus 标准表单控件一致的 24/32/40px 高度。
const controlSize = computed(
  () =>
    ({
      default: {
        action: 'w-[32px]',
        editor: 'px-3 py-1.5 text-sm',
        height: 'h-[32px]',
        icon: 'right-2.5 size-4',
        trigger: 'pr-9',
      },
      large: {
        action: 'w-[40px]',
        editor: 'px-3 py-2 text-sm',
        height: 'h-[40px]',
        icon: 'right-3 size-4',
        trigger: 'pr-10',
      },
      small: {
        action: 'w-[24px]',
        editor: 'px-2 py-1 text-xs',
        height: 'h-[24px]',
        icon: 'right-2 size-3.5',
        trigger: 'pr-7',
      },
    })[props.size],
);

// en-US 始终作为编辑区第一项和必填兜底语言。
const orderedLocales = computed(() => [
  'en-US',
  ...props.locales.filter((locale) => locale !== 'en-US'),
]);

// 外部输入框按当前界面语言、en-US、Message Key 的顺序显示。
const displayValue = computed(() => {
  if (committed.value?.messageKey === modelValue.value) {
    return (
      getMessageValue(
        committed.value.values,
        String(i18n.global.locale.value),
      ) ||
      getMessageValue(committed.value.values, 'en-US') ||
      modelValue.value
    );
  }
  if (!modelValue.value) return '';
  const active = $t(modelValue.value);
  if (active !== modelValue.value) return active;
  const fallback = $t(modelValue.value, {}, { locale: 'en-US' });
  return fallback === modelValue.value ? modelValue.value : fallback;
});

// 校验非空 Message Key 的长度、格式和保留路径段。
const keyError = computed(() => {
  // 去除首尾空格后的待校验 Message Key。
  const key = draft.value.messageKey.trim();
  if (!key) return '';
  if (key.length > 191) return $t('ui.i18nMessageInput.keyTooLong');
  if (!/^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)*$/.test(key)) {
    return $t('ui.i18nMessageInput.keyInvalid');
  }
  if (
    key
      .split('.')
      .some((segment) =>
        ['__proto__', 'constructor', 'prototype'].includes(segment),
      )
  ) {
    return $t('ui.i18nMessageInput.keyReserved');
  }
  return '';
});

// 校验兜底语言必填与各语言文本的最大长度。
const valueError = computed(() => {
  if (draft.value.values.some(({ value }) => value.length > 4000)) {
    return $t('ui.i18nMessageInput.valueTooLong');
  }
  if (
    draft.value.messageKey.trim() &&
    !getMessageValue(draft.value.values, 'en-US').trim()
  ) {
    return $t('ui.i18nMessageInput.fallbackRequired');
  }
  return '';
});

// 草稿是否相对初始快照发生变化。
const dirty = computed(() => !messagesEqual(draft.value, initial.value));

// 加载、保存或校验未通过时禁止保存。
const cannotSave = computed(
  () =>
    loading.value ||
    saving.value ||
    loadError.value ||
    !draft.value.messageKey.trim() ||
    !!keyError.value ||
    !!valueError.value ||
    searchedKey.value !== draft.value.messageKey.trim(),
);

// 外部 Message Key 变化后清除不再匹配的已提交数据。
watch(modelValue, (value) => {
  if (committed.value?.messageKey !== value) {
    committed.value = undefined;
  }
});

/** 创建一份不会复用 values 引用的 Message 数据。 */
function createMessage(messageKey = '', values: I18nMessageValue[] = []) {
  return { messageKey, values: copyValues(values) };
}

/** 深拷贝一层 Message 及其语言文本。 */
function copyMessage(message: I18nMessage) {
  return createMessage(message.messageKey, message.values);
}

/** 复制语言文本列表，避免直接修改调用方数据。 */
function copyValues(values: I18nMessageValue[]) {
  return values.map((item) => ({ ...item }));
}

/** 获取指定语言的文本，不存在时返回空字符串。 */
function getMessageValue(values: I18nMessageValue[], locale: string) {
  return values.find((item) => item.locale === locale)?.value ?? '';
}

/** 更新草稿中的 Message Key。 */
function setDraftKey(value: string) {
  ++loadSequence;
  loadError.value = false;
  loading.value = false;
  searchedKey.value = undefined;
  canResetLookup.value = false;
  draft.value = { ...draft.value, messageKey: value };
}

/** 更新或补充草稿中指定语言的文本。 */
function setDraftValue(locale: string, value: string) {
  // 基于副本更新语言文本，保持草稿更新可追踪。
  const values = copyValues(draft.value.values);
  // 草稿中当前语言已有的文本项。
  const current = values.find((item) => item.locale === locale);
  if (current) {
    current.value = value;
  } else {
    values.push({ locale, value });
  }
  draft.value = { ...draft.value, values };
}

/** 按 Message Key 和各语言文本判断两份数据是否一致。 */
function messagesEqual(left: I18nMessage, right: I18nMessage) {
  if (left.messageKey !== right.messageKey) return false;
  // 两份数据中出现过的全部语言集合。
  const locales = new Set([
    ...left.values.map(({ locale }) => locale),
    ...right.values.map(({ locale }) => locale),
  ]);
  return [...locales].every(
    (locale) =>
      getMessageValue(left.values, locale) ===
      getMessageValue(right.values, locale),
  );
}

/** 使用当前 v-model 加载本次打开所需的独立草稿。 */
async function loadDraft() {
  // 当前表单绑定并去除首尾空格后的 Message Key。
  const messageKey = modelValue.value.trim();
  // 优先使用最近已提交的数据初始化草稿。
  const currentMessage =
    committed.value?.messageKey === messageKey
      ? copyMessage(committed.value)
      : createMessage(messageKey);
  draft.value = currentMessage;
  initial.value = copyMessage(currentMessage);
  loadError.value = false;
  searchedKey.value = undefined;
  canResetLookup.value = false;

  if (!messageKey) return;

  await lookup(messageKey, true);
}

/** 查询指定 Key；非空结果覆盖全部语言，空结果保留当前草稿。 */
async function lookup(messageKey: string, initializing = false) {
  const key = messageKey.trim();
  if (!key || keyError.value) return;
  // 本次查询的序号，用于识别 Key 变化、关闭或后续查询产生的过期响应。
  const sequence = ++loadSequence;
  loading.value = true;
  try {
    // 使用方返回的当前 Message 数据或不存在标记。
    const result = await props.load(key);
    if (sequence !== loadSequence || !visible.value) return;
    loadError.value = false;
    searchedKey.value = key;
    canResetLookup.value = !!result?.values.length;
    if (result?.values.length) {
      // 只接受当前请求 Key，避免加载函数返回错误的 Message Key。
      const message = createMessage(key, result.values);
      draft.value = message;
      committed.value = copyMessage(message);
    }
    if (initializing) {
      initial.value = copyMessage(draft.value);
    }
  } catch {
    if (sequence === loadSequence && visible.value) {
      loadError.value = true;
      searchedKey.value = undefined;
      canResetLookup.value = false;
    }
  } finally {
    if (sequence === loadSequence) {
      loading.value = false;
    }
  }
}

/** 打开浮层并触发本次数据加载。 */
function open() {
  if (visible.value || props.disabled) return;
  visible.value = true;
  void loadDraft();
}

/** 关闭浮层并使尚未完成的加载结果失效。 */
function close() {
  ++loadSequence;
  visible.value = false;
  loading.value = false;
  saving.value = false;
  loadError.value = false;
  searchedKey.value = undefined;
  canResetLookup.value = false;
}

/** 清空非空查询结果，保留 Key 与已完成的查询状态供重新填写。 */
function resetLookupValues() {
  if (!canResetLookup.value || loading.value || saving.value) return;
  draft.value = createMessage(draft.value.messageKey);
  canResetLookup.value = false;
}

/** 根据当前查询状态执行查询或清空已回填的语言文本。 */
function handleLookupAction() {
  if (canResetLookup.value) {
    resetLookupValues();
    return;
  }
  void lookup(draft.value.messageKey);
}

/** 取消编辑；草稿变化时先请求用户确认。 */
async function cancel() {
  if (!dirty.value) {
    close();
    return;
  }
  try {
    await confirmDialog({
      cancelText: $t('ui.i18nMessageInput.keepEditing'),
      confirmText: $t('ui.i18nMessageInput.confirmCancel'),
      content: $t('ui.i18nMessageInput.unsavedDescription'),
      icon: 'warning',
      showCancel: true,
      title: $t('ui.i18nMessageInput.unsavedTitle'),
    });
    close();
  } catch {
    // 用户拒绝确认时继续保留当前草稿。
  }
}

/** 在加载失败状态下重新加载当前 Message。 */
async function retryLoad() {
  await lookup(draft.value.messageKey);
}

/** 保存草稿，成功后同步 v-model 并关闭浮层。 */
async function save() {
  if (cannotSave.value) return;
  saving.value = true;
  try {
    // 后端保存并确认后的完整 Message 数据。
    const result = await props.save({
      messageKey: draft.value.messageKey.trim(),
      values: orderedLocales.value.map((locale) => ({
        locale,
        value: getMessageValue(draft.value.values, locale).trim(),
      })),
    });
    // 保存结果副本，避免组件状态与调用方对象共享引用。
    const message = copyMessage(result);
    committed.value = message;
    draft.value = copyMessage(message);
    initial.value = copyMessage(message);
    modelValue.value = message.messageKey;
    close();
  } catch {
    // 保存函数负责展示错误，组件保留当前草稿供用户重试。
  } finally {
    saving.value = false;
  }
}

/** 响应 Popover 的打开和外部关闭请求。 */
function handleVisibilityChange(opened: boolean) {
  if (opened) {
    open();
  } else if (visible.value && !saving.value) {
    void cancel();
  }
}
</script>

<template>
  <VbenPopover
    :open="visible"
    :content-props="contentProps"
    content-class="w-[420px] max-w-[calc(100vw-32px)] p-0"
    trigger-class="w-full"
    @update:open="handleVisibilityChange"
  >
    <template #trigger>
      <div class="relative w-full">
        <Input
          v-bind="$attrs"
          :model-value="displayValue"
          :disabled="disabled"
          :placeholder="placeholder || $t('ui.i18nMessageInput.placeholder')"
          :aria-expanded="visible"
          :class="[controlSize.height, controlSize.editor, controlSize.trigger]"
          class="w-full cursor-pointer"
          readonly
          role="combobox"
        />
        <VbenIcon
          icon="lucide:languages"
          :class="controlSize.icon"
          class="pointer-events-none absolute top-1/2 -translate-y-1/2"
        />
      </div>
    </template>

    <div class="flex max-h-[min(520px,calc(100vh-32px))] flex-col gap-4 p-4">
      <div class="space-y-2">
        <Label :for="`${fieldId}-key`" class="inline-flex items-center gap-0.5">
          {{ $t('ui.i18nMessageInput.key') }}
          <span
            aria-hidden="true"
            class="text-destructive -translate-y-px text-xs leading-none"
            data-test="i18n-message-key-required"
          >
            *
          </span>
        </Label>
        <div
          :class="controlSize.height"
          class="i18n-message-key-group border-input bg-background flex w-full items-center overflow-hidden rounded-md border"
          data-test="i18n-message-key-group"
        >
          <Input
            :id="`${fieldId}-key`"
            aria-required="true"
            :class="controlSize.editor"
            class="i18n-message-key-input h-full min-w-0 flex-1 rounded-none border-0 shadow-none"
            data-test="i18n-message-key"
            :model-value="draft.messageKey"
            :placeholder="$t('ui.i18nMessageInput.keyPlaceholder')"
            :disabled="loading || saving"
            @update:model-value="setDraftKey(String($event))"
          />
          <Button
            type="button"
            variant="ghost"
            size="icon"
            :class="controlSize.action"
            class="h-full shrink-0 cursor-pointer rounded-none disabled:pointer-events-auto disabled:cursor-not-allowed"
            data-test="i18n-message-search"
            :title="
              $t(
                canResetLookup
                  ? 'ui.i18nMessageInput.reset'
                  : 'ui.i18nMessageInput.search',
              )
            "
            :disabled="
              loading || saving || !draft.messageKey.trim() || !!keyError
            "
            @click="handleLookupAction"
          >
            <VbenIcon
              :icon="canResetLookup ? 'lucide:rotate-ccw' : 'lucide:search'"
              class="size-4"
            />
          </Button>
        </div>
        <p v-if="keyError" class="text-destructive text-xs">{{ keyError }}</p>
      </div>

      <div
        v-if="loading"
        class="text-muted-foreground flex min-h-40 items-center justify-center gap-2 text-sm"
      >
        <VbenSpinner class="size-4" />
        <span>{{ $t('ui.i18nMessageInput.loading') }}</span>
      </div>

      <div
        v-else-if="loadError"
        class="flex min-h-40 flex-col items-center justify-center gap-3"
      >
        <p class="text-destructive text-sm">
          {{ $t('ui.i18nMessageInput.loadError') }}
        </p>
        <Button variant="outline" size="sm" type="button" @click="retryLoad">
          <VbenIcon icon="lucide:refresh-cw" class="mr-2 size-4" />
          {{ $t('ui.i18nMessageInput.retry') }}
        </Button>
      </div>

      <div v-else class="min-h-0 overflow-y-auto pr-1">
        <div class="space-y-3">
          <div
            v-for="locale in orderedLocales"
            :key="locale"
            class="space-y-1.5"
          >
            <Label :for="`${fieldId}-${locale}`">
              {{ locale }}
              <span
                v-if="locale === 'en-US'"
                aria-hidden="true"
                class="text-destructive ml-0.5 text-xs"
              >
                *
              </span>
            </Label>
            <Textarea
              :id="`${fieldId}-${locale}`"
              :data-locale="locale"
              :model-value="getMessageValue(draft.values, locale)"
              :placeholder="$t('ui.i18nMessageInput.valuePlaceholder')"
              :disabled="saving"
              :rows="rows"
              :class="controlSize.editor"
              class="i18n-message-textarea resize-y bg-background shadow-none transition-none placeholder:text-muted-foreground/50 focus-visible:border-input focus-visible:ring-0 dark:bg-background"
              @update:model-value="setDraftValue(locale, String($event))"
            />
          </div>
        </div>
        <p
          v-if="valueError"
          class="text-destructive mt-1 text-xs"
          data-test="i18n-message-value-error"
        >
          {{ valueError }}
        </p>
      </div>

      <div class="flex shrink-0 justify-end gap-2 border-t pt-3">
        <Button
          variant="outline"
          type="button"
          :disabled="saving"
          @click="cancel"
        >
          {{ $t('ui.i18nMessageInput.cancel') }}
        </Button>
        <Button type="button" :disabled="cannotSave" @click="save">
          <VbenIcon icon="lucide:check" class="mr-2 size-4" />
          {{ $t('ui.i18nMessageInput.save') }}
        </Button>
      </div>
    </div>
  </VbenPopover>
</template>

<style scoped lang="scss">
.i18n-message-textarea {
  --ring: var(--primary);

  field-sizing: fixed;
  min-height: 0;

  &:focus-visible {
    box-shadow: inset 0 0 0 1px hsl(var(--ring));
  }
}

.i18n-message-key-group {
  --ring: var(--primary);

  &:focus-within {
    border-color: hsl(var(--ring));
  }
}

.i18n-message-key-input:focus-visible {
  box-shadow: none;
}
</style>
