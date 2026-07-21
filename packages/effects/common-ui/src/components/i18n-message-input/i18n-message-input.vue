<script setup lang="ts">
import type {
  I18nMessage,
  I18nMessageChange,
  I18nMessageChangeHandler,
  I18nMessageConfirm,
  I18nMessageInputPosition,
  I18nMessageInputTexts,
  I18nMessageLoader,
  I18nMessageSource,
} from './types';

import { computed, ref, toValue, watch } from 'vue';

import { confirm as confirmDialog } from '@vben-core/popup-ui';
import {
  Button,
  Input,
  Label,
  Textarea,
  VbenIcon,
  VbenIconButton,
  VbenPopover,
  VbenSpinner,
} from '@vben-core/shadcn-ui';

interface Props {
  confirm?: I18nMessageConfirm;
  data?: I18nMessageSource;
  defaultLocale?: string;
  disabled?: boolean;
  height?: number | string;
  load?: I18nMessageLoader;
  locales?: string[];
  onChange?: I18nMessageChangeHandler;
  placeholder?: string;
  position?: I18nMessageInputPosition;
  presetKey?: string;
  texts?: Partial<I18nMessageInputTexts>;
  width?: number | string;
}

interface Snapshot {
  i18nKey: string;
  modelValue: string;
  values: I18nMessage['values'];
}

const props = withDefaults(defineProps<Props>(), {
  confirm: undefined,
  data: undefined,
  defaultLocale: 'zh-CN',
  disabled: false,
  height: 360,
  load: undefined,
  locales: () => ['zh-CN', 'en-US'],
  onChange: undefined,
  placeholder: '',
  position: 'right',
  presetKey: '',
  texts: () => ({}),
  width: 420,
});

const emit = defineEmits<{
  change: [I18nMessageChange];
}>();

const modelValue = defineModel<string>({ default: '' });
const i18nKey = defineModel<string>('i18nKey', { default: '' });

const defaultTexts: I18nMessageInputTexts = {
  cancel: 'Cancel',
  confirm: 'Confirm',
  discard: 'Discard',
  discardDescription: 'Your unsaved changes will be lost.',
  discardTitle: 'Discard unsaved changes?',
  i18nKey: 'Internationalization key',
  keyInvalid: 'Key must be a valid dot-separated path',
  keyPlaceholder: 'Enter a dot-separated key',
  keyRequired: 'Internationalization key is required',
  keyReserved: 'Key contains a reserved path segment',
  keyTooLong: 'Key must not exceed 191 characters',
  loadError: 'Failed to load translations',
  loading: 'Loading',
  keepEditing: 'Keep editing',
  retry: 'Retry',
  valueTooLong: 'A translation must not exceed 4000 characters',
  valuePlaceholder: 'Enter the translation',
};

const text = computed(() => ({ ...defaultTexts, ...props.texts }));
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const loadError = ref<null | unknown>(null);
const dirty = ref(false);
const draftKey = ref('');
const draftValues = ref<I18nMessage['values']>([]);
const previousKey = ref('');
const snapshot = ref<Snapshot>();
let loadSequence = 0;
let activeOpen: null | Promise<void> = null;
let activeClose: null | Promise<void> = null;

const panelStyle = computed(() => ({
  height: toCssSize(props.height),
  maxHeight: 'calc(100vh - 32px)',
  maxWidth: 'calc(100vw - 32px)',
  width: toCssSize(props.width),
}));

const contentClass = computed(() =>
  props.position === 'center'
    ? '!fixed !left-1/2 !top-1/2 !-translate-x-1/2 !-translate-y-1/2'
    : '',
);

const contentProps = computed(() => ({
  align: 'center' as const,
  side: props.position === 'center' ? ('bottom' as const) : props.position,
  sideOffset: 8,
  style: panelStyle.value,
}));

const keyError = computed(() => {
  const key = draftKey.value.trim();
  if (!key) return text.value.keyRequired;
  if (key.length > 191) return text.value.keyTooLong;
  if (!/^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)*$/.test(key)) {
    return text.value.keyInvalid;
  }
  if (
    key
      .split('.')
      .some((segment) =>
        ['__proto__', 'constructor', 'prototype'].includes(segment),
      )
  ) {
    return text.value.keyReserved;
  }
  return '';
});

const valueError = computed(() =>
  draftValues.value.some(({ value }) => value.length > 4000)
    ? text.value.valueTooLong
    : '',
);

const cannotConfirm = computed(
  () =>
    loading.value ||
    saving.value ||
    !!loadError.value ||
    !!keyError.value ||
    !!valueError.value,
);

watch(
  () => modelValue.value,
  (value) => {
    if (!visible.value) {
      setDraftValue(props.defaultLocale, value ?? '', false);
    }
  },
  { immediate: true },
);

watch(
  () => i18nKey.value,
  (value) => {
    if (!visible.value) {
      draftKey.value = value ?? '';
    }
  },
  { immediate: true },
);

function toCssSize(value: number | string) {
  return typeof value === 'number' ? `${value}px` : value;
}

function copyValues(values: I18nMessage['values']) {
  return values.map((item) => ({ ...item }));
}

function getDraftMessage(): I18nMessage {
  return {
    i18nKey: draftKey.value,
    values: copyValues(draftValues.value),
  };
}

function getValue(locale: string) {
  return draftValues.value.find((item) => item.locale === locale)?.value ?? '';
}

function setDraftValue(locale: string, value: string, notify = true) {
  const values = copyValues(draftValues.value);
  const existing = values.find((item) => item.locale === locale);
  if (existing) {
    existing.value = value;
  } else {
    values.push({ locale, value });
  }
  draftValues.value = values;
  if (locale === props.defaultLocale && modelValue.value !== value) {
    modelValue.value = value;
  }
  if (notify) {
    if (visible.value) dirty.value = true;
    notifyChange({
      i18nKey: draftKey.value,
      locale,
      message: getDraftMessage(),
      type: 'value',
      value,
    });
  }
}

function setDraftKey(value: string, notify = true) {
  draftKey.value = value;
  if (i18nKey.value !== value) {
    i18nKey.value = value;
  }
  if (notify) {
    if (visible.value) dirty.value = true;
    notifyChange({
      i18nKey: value,
      message: getDraftMessage(),
      type: 'key',
    });
  }
}

function notifyChange(change: I18nMessageChange) {
  props.onChange?.(change);
  emit('change', change);
}

function initializeDraft() {
  const source = props.load ? undefined : toValue(props.data);
  const sourceValues = source?.values ?? [];
  draftValues.value = copyValues(sourceValues);
  if (!draftValues.value.some((item) => item.locale === props.defaultLocale)) {
    setDraftValue(props.defaultLocale, modelValue.value ?? '', false);
  }
  draftKey.value =
    i18nKey.value || source?.i18nKey || (source ? '' : props.presetKey) || '';
}

async function performLoad() {
  if (!props.load || !previousKey.value) return;
  const sequence = ++loadSequence;
  loading.value = true;
  loadError.value = null;
  try {
    const result = await props.load(previousKey.value);
    if (sequence !== loadSequence || !visible.value) return;
    if (result) {
      draftKey.value = result.i18nKey;
      draftValues.value = copyValues(result.values);
      modelValue.value = getValue(props.defaultLocale);
    }
  } catch (error) {
    if (sequence === loadSequence && visible.value) {
      loadError.value = error;
    }
    throw error;
  } finally {
    if (sequence === loadSequence) {
      loading.value = false;
    }
  }
}

function open() {
  if (activeOpen) return activeOpen;
  if (visible.value) return Promise.resolve();
  previousKey.value = i18nKey.value ?? '';
  dirty.value = false;
  snapshot.value = {
    i18nKey: i18nKey.value ?? '',
    modelValue: modelValue.value ?? '',
    values: copyValues(draftValues.value),
  };
  initializeDraft();
  visible.value = true;
  activeOpen = performLoad().finally(() => {
    activeOpen = null;
  });
  return activeOpen;
}

function restoreSnapshot() {
  const original = snapshot.value;
  if (!original) return;
  draftKey.value = original.i18nKey;
  draftValues.value = copyValues(original.values);
  i18nKey.value = original.i18nKey;
  modelValue.value = original.modelValue;
}

function discardAndClose() {
  ++loadSequence;
  restoreSnapshot();
  visible.value = false;
  dirty.value = false;
  loading.value = false;
  loadError.value = null;
  activeOpen = null;
  activeClose = null;
}

function close() {
  if (!visible.value || saving.value || activeClose) return;
  if (!dirty.value) {
    discardAndClose();
    return;
  }
  activeClose = confirmDialog({
    cancelText: text.value.keepEditing,
    confirmText: text.value.discard,
    content: text.value.discardDescription,
    icon: 'warning',
    showCancel: true,
    title: text.value.discardTitle,
  })
    .then(discardAndClose)
    .catch(() => {})
    .finally(() => {
      activeClose = null;
    });
}

async function retryLoad() {
  try {
    await performLoad();
  } catch {
    // The visible failure state owns the error presentation.
  }
}

async function handleConfirm() {
  if (cannotConfirm.value) return;
  saving.value = true;
  try {
    const input = {
      i18nKey: draftKey.value.trim(),
      previousKey: previousKey.value || undefined,
      values: props.locales.map((locale) => ({
        locale,
        value: getValue(locale),
      })),
    };
    const result = props.confirm
      ? await props.confirm(input)
      : { i18nKey: input.i18nKey, values: input.values };
    draftKey.value = result.i18nKey;
    draftValues.value = copyValues(result.values);
    i18nKey.value = result.i18nKey;
    modelValue.value = getValue(props.defaultLocale);
    previousKey.value = result.i18nKey;
    snapshot.value = {
      i18nKey: result.i18nKey,
      modelValue: modelValue.value,
      values: copyValues(result.values),
    };
    dirty.value = false;
    visible.value = false;
  } catch {
    // The injected confirm boundary owns error presentation.
  } finally {
    saving.value = false;
  }
}

function handleVisibilityChange(opened: boolean) {
  if (opened) {
    void open().catch(() => {});
  } else if (visible.value && !saving.value) {
    close();
  }
}

defineExpose({ close, open });
</script>

<template>
  <VbenPopover
    :open="visible"
    :content-class="contentClass"
    :content-props="contentProps"
    trigger-class="w-full"
    @update:open="handleVisibilityChange"
  >
    <template #trigger>
      <div class="relative w-full">
        <Input
          :model-value="modelValue"
          :disabled="disabled"
          :placeholder="placeholder"
          class="w-full pr-10"
          @update:model-value="setDraftValue(defaultLocale, String($event))"
        />
        <VbenIconButton
          :disabled="disabled"
          :tooltip="text.i18nKey"
          class="absolute top-1/2 right-1 size-8 -translate-y-1/2 rounded-md"
          type="button"
          @click.stop="handleVisibilityChange(true)"
        >
          <VbenIcon icon="lucide:languages" class="size-4" />
        </VbenIconButton>
      </div>
    </template>

    <div class="flex h-full min-h-0 flex-col gap-4 overflow-hidden p-4">
      <div class="space-y-2">
        <Label for="i18n-message-key">{{ text.i18nKey }}</Label>
        <Input
          id="i18n-message-key"
          :model-value="draftKey"
          :placeholder="text.keyPlaceholder"
          :disabled="loading || saving"
          @update:model-value="setDraftKey(String($event))"
        />
        <p v-if="keyError" class="text-destructive text-xs">{{ keyError }}</p>
      </div>

      <div
        v-if="loading"
        class="text-muted-foreground flex min-h-0 flex-1 items-center justify-center gap-2 text-sm"
      >
        <VbenSpinner class="size-4" />
        <span>{{ text.loading }}</span>
      </div>

      <div
        v-else-if="loadError"
        class="flex min-h-0 flex-1 flex-col items-center justify-center gap-3"
      >
        <p class="text-destructive text-sm">{{ text.loadError }}</p>
        <Button variant="outline" size="sm" type="button" @click="retryLoad">
          <VbenIcon icon="lucide:refresh-cw" class="mr-2 size-4" />
          {{ text.retry }}
        </Button>
      </div>

      <div v-else class="min-h-0 flex-1 space-y-3 overflow-y-auto pr-1">
        <div v-for="locale in locales" :key="locale" class="space-y-1.5">
          <Label :for="`i18n-message-${locale}`">{{ locale }}</Label>
          <Textarea
            :id="`i18n-message-${locale}`"
            :model-value="getValue(locale)"
            :placeholder="text.valuePlaceholder"
            :disabled="saving"
            class="min-h-20 resize-y"
            @update:model-value="setDraftValue(locale, String($event))"
          />
        </div>
        <p v-if="valueError" class="text-destructive text-xs">
          {{ valueError }}
        </p>
      </div>

      <div class="flex shrink-0 justify-end gap-2 border-t pt-3">
        <Button
          variant="outline"
          type="button"
          :disabled="saving"
          @click="close"
        >
          {{ text.cancel }}
        </Button>
        <Button type="button" :disabled="cannotConfirm" @click="handleConfirm">
          <VbenIcon icon="lucide:check" class="mr-2 size-4" />
          {{ text.confirm }}
        </Button>
      </div>
    </div>
  </VbenPopover>
</template>
