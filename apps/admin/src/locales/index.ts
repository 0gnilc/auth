import type { Language } from 'element-plus/es/locale';

import type { App } from 'vue';

import type { LocaleSetupOptions, SupportedLanguagesType } from '@vben/locales';

import { ref } from 'vue';

import {
  $t,
  i18n,
  loadCoreLocaleMessages,
  loadLocaleMessages,
  setupI18n as coreSetup,
  loadLocalesMapFromDir,
} from '@vben/locales';
import { preferences } from '@vben/preferences';

import dayjs from 'dayjs';
import enLocale from 'element-plus/es/locale/lang/en';
import defaultLocale from 'element-plus/es/locale/lang/zh-cn';

import { collectLeafKeys, mergeMessages } from './messages';

const elementLocale = ref<Language>(defaultLocale);
const DEFAULT_LOCALE: SupportedLanguagesType = 'zh-CN';
const SUPPORTED_LOCALES: SupportedLanguagesType[] = ['zh-CN', 'en-US'];

const modules = import.meta.glob('./langs/**/*.json');
const dynamicMessages = ref<Record<string, Record<string, unknown>>>({});
const staticKeys = new Set<string>();

const localesMap = loadLocalesMapFromDir(
  /\.\/langs\/([^/]+)\/(.*)\.json$/,
  modules,
);
/**
 * 加载应用特有的语言包
 * 这里也可以改造为从服务端获取翻译数据
 * @param lang
 */
async function loadMessages(lang: SupportedLanguagesType) {
  const [appLocaleMessages, coreLocaleMessages] = await Promise.all([
    localesMap[lang]?.(),
    loadCoreLocaleMessages(lang),
    loadThirdPartyMessage(lang),
  ]);
  const localMessages = appLocaleMessages?.default ?? {};
  collectLeafKeys(coreLocaleMessages, '', staticKeys);
  collectLeafKeys(localMessages, '', staticKeys);
  return mergeMessages(
    mergeMessages(dynamicMessages.value[lang] ?? {}, coreLocaleMessages),
    localMessages,
  );
}

async function ensureStaticKeys() {
  await Promise.all(
    SUPPORTED_LOCALES.map(async (locale) => {
      const [appMessages, coreMessages] = await Promise.all([
        localesMap[locale]?.(),
        loadCoreLocaleMessages(locale),
      ]);
      collectLeafKeys(coreMessages, '', staticKeys);
      collectLeafKeys(appMessages?.default ?? {}, '', staticKeys);
    }),
  );
  return staticKeys;
}

async function applyDynamicMessages(
  bundle: Record<string, Record<string, unknown>>,
) {
  dynamicMessages.value = bundle;
  const activeLocale = i18n.global.locale.value as SupportedLanguagesType;
  for (const locale of SUPPORTED_LOCALES) {
    await loadLocaleMessages(locale);
  }
  if (SUPPORTED_LOCALES.includes(activeLocale)) {
    await loadLocaleMessages(activeLocale);
  }
}

/**
 * 加载第三方组件库的语言包
 * @param lang
 */
async function loadThirdPartyMessage(lang: SupportedLanguagesType) {
  await Promise.all([loadElementLocale(lang), loadDayjsLocale(lang)]);
}

/**
 * 加载dayjs的语言包
 * @param lang
 */
async function loadDayjsLocale(lang: SupportedLanguagesType) {
  let locale;
  switch (lang) {
    case 'en-US': {
      locale = await import('dayjs/locale/en');
      break;
    }
    case 'zh-CN': {
      locale = await import('dayjs/locale/zh-cn');
      break;
    }
    // 默认使用英语
    default: {
      locale = await import('dayjs/locale/en');
    }
  }
  if (locale) {
    dayjs.locale(locale);
  } else {
    console.error(`Failed to load dayjs locale for ${lang}`);
  }
}

/**
 * 加载element-plus的语言包
 * @param lang
 */
async function loadElementLocale(lang: SupportedLanguagesType) {
  switch (lang) {
    case 'en-US': {
      elementLocale.value = enLocale;
      break;
    }
    case 'zh-CN': {
      elementLocale.value = defaultLocale;
      break;
    }
  }
}

async function setupI18n(app: App, options: LocaleSetupOptions = {}) {
  await coreSetup(app, {
    defaultLocale: preferences.app.locale,
    loadMessages: async (lang) =>
      (await loadMessages(lang)) as Record<string, string>,
    missingWarn: !import.meta.env.PROD,
    ...options,
  });
  i18n.global.fallbackLocale.value = DEFAULT_LOCALE;
}

export {
  $t,
  applyDynamicMessages,
  elementLocale,
  ensureStaticKeys,
  setupI18n,
  SUPPORTED_LOCALES,
};
