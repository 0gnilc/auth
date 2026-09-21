import type { SupportedLocale } from '@vben-core/shared/constants';

export type SupportedLanguagesType = SupportedLocale;

export type ImportLocaleFn = () => Promise<{ default: Record<string, string> }>;

export type LoadMessageFn = (
  lang: SupportedLanguagesType,
) => Promise<Record<string, string> | undefined>;

export interface LocaleSetupOptions {
  /**
   * Default language
   * @default zh-CN
   */
  defaultLocale?: SupportedLanguagesType;
  /**
   * Load message function
   * @param lang
   * @returns
   */
  loadMessages?: LoadMessageFn;
  /**
   * Whether to warn when the key is not found
   */
  missingWarn?: boolean;
}
