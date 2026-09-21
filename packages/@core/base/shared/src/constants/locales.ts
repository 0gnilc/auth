export const FALLBACK_LOCALE = 'en-US' as const;

export const SUPPORTED_LOCALES = ['en-US', 'zh-CN', 'ha-NG', 'yo-NG'] as const;

export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number];
