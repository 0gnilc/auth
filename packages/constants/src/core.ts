import type { SupportedLocale } from '@vben-core/shared/constants';

import { SUPPORTED_LOCALES } from '@vben-core/shared/constants';

/**
 * @zh_CN 登录页面 url 地址
 */
export const LOGIN_PATH = '/auth/login';

export interface LanguageOption {
  label: string;
  value: SupportedLocale;
}

/**
 * Supported languages
 */
const LANGUAGE_LABELS: Record<SupportedLocale, string> = {
  'en-US': 'English',
  'ha-NG': 'Hausa',
  'yo-NG': 'Yorùbá',
  'zh-CN': '简体中文',
};

export const SUPPORT_LANGUAGES: LanguageOption[] = SUPPORTED_LOCALES.map(
  (value) => ({ label: LANGUAGE_LABELS[value], value }),
);
