import type { MaybeRef } from 'vue';

export interface I18nMessageValue {
  locale: string;
  value: string;
}

export interface I18nMessage {
  i18nKey: string;
  values: I18nMessageValue[];
}

export type I18nMessageLoader = (
  i18nKey: string,
) => Promise<I18nMessage | null>;

export type I18nMessageConfirm = (input: {
  i18nKey: string;
  previousKey?: string;
  values: I18nMessageValue[];
}) => Promise<I18nMessage>;

export type I18nMessageChange =
  | {
      i18nKey: string;
      message: I18nMessage;
      type: 'key';
    }
  | {
      i18nKey: string;
      locale: string;
      message: I18nMessage;
      type: 'value';
      value: string;
    };

export type I18nMessageChangeHandler = (change: I18nMessageChange) => void;

export interface I18nMessageInputExpose {
  close: () => void;
  open: () => Promise<void>;
}

export interface I18nMessageInputTexts {
  cancel: string;
  confirm: string;
  discard: string;
  discardDescription: string;
  discardTitle: string;
  i18nKey: string;
  keyInvalid: string;
  keyPlaceholder: string;
  keyRequired: string;
  keyReserved: string;
  keyTooLong: string;
  loadError: string;
  loading: string;
  keepEditing: string;
  retry: string;
  valueTooLong: string;
  valuePlaceholder: string;
}

export type I18nMessageSource = MaybeRef<I18nMessage | null | undefined>;

export type I18nMessageInputPosition =
  | 'bottom'
  | 'center'
  | 'left'
  | 'right'
  | 'top';
