import type { SupportedLocale } from '@vben-core/shared/constants';

export type Locale = SupportedLocale;

export const messages: Record<Locale, Record<string, string>> = {
  'en-US': {
    cancel: 'Cancel',
    collapse: 'Collapse',
    confirm: 'Confirm',
    expand: 'Expand',
    prompt: 'Prompt',
    reset: 'Reset',
    submit: 'Submit',
    confirmTitle: 'Please Confirm',
  },
  'ha-NG': {
    cancel: 'Soke',
    collapse: 'Rufe',
    confirm: 'Tabbatar',
    expand: 'Buɗe',
    prompt: 'Sanarwa',
    reset: 'Sake saita',
    submit: 'Aika',
    confirmTitle: 'Da fatan a tabbatar',
  },
  'yo-NG': {
    cancel: 'Fagilé',
    collapse: 'Kó jọ',
    confirm: 'Jẹ́rìí',
    expand: 'Fẹ̀ síi',
    prompt: 'Ìfitónilétí',
    reset: 'Tún ṣètò',
    submit: 'Fi ránṣẹ́',
    confirmTitle: 'Jọ̀wọ́ jẹ́rìí',
  },
  'zh-CN': {
    cancel: '取消',
    collapse: '收起',
    confirm: '确认',
    expand: '展开',
    prompt: '提示',
    reset: '重置',
    submit: '提交',
    confirmTitle: '请确认',
  },
};

export const getMessages = (locale: Locale) => messages[locale];
