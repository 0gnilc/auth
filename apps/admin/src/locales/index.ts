import type { Language } from 'element-plus/es/locale';

import type { App } from 'vue';

import type { LocaleSetupOptions, SupportedLanguagesType } from '@vben/locales';

import { ref } from 'vue';

import {
  $t,
  setupI18n as coreSetup,
  i18n,
  loadCoreLocaleMessages,
  loadLocalesMapFromDir,
} from '@vben/locales';
import { preferences } from '@vben/preferences';

import dayjs from 'dayjs';
import enLocale from 'element-plus/es/locale/lang/en';
import defaultLocale from 'element-plus/es/locale/lang/zh-cn';

import { mergeMessages } from './messages';

// Element Plus 通过响应式引用消费当前语言配置，切换后无需重新挂载应用。
const elementLocale = ref<Language>(defaultLocale);
// vue-i18n 找不到当前语言消息时，统一回退到英文。
const DEFAULT_LOCALE: SupportedLanguagesType = 'en-US';
// 前后端共享同一组固定语言代码；数据库动态消息也只会返回这些语言。
const SUPPORTED_LOCALES: SupportedLanguagesType[] = ['en-US', 'zh-CN'];

// Vite 将每个语言目录包装为延迟加载函数，避免首屏一次加载全部静态 JSON。
const modules = import.meta.glob('./langs/**/*.json');
// 后端返回的是按语言组织的完整快照；替换整个对象才能同步反映已删除的 key。
const dynamicMessages = ref<Record<string, Record<string, unknown>>>({});

// 将 ./langs/{locale}/{namespace}.json 聚合为按语言延迟加载的消息树。
const localesMap = loadLocalesMapFromDir(
  /\.\/langs\/([^/]+)\/(.*)\.json$/,
  modules,
);
/**
 * 生成指定语言最终交给 vue-i18n 的消息树。
 *
 * 动态消息优先级最低，公共静态语言包居中，应用静态语言包最高。这样后台
 * 可以配置菜单等运行时内容，但不能覆盖随版本发布的按钮、表单和页面文案。
 */
async function buildMessages(lang: SupportedLanguagesType) {
  // 并行加载 Admin 应用静态消息和公共静态消息。
  const [appLocaleMessages, coreLocaleMessages] = await Promise.all([
    // 加载 apps/admin/src/locales/langs/{lang} 下的应用专属 JSON。
    localesMap[lang]?.(),
    // 加载 packages/locales/src/langs/{lang} 下的公共 JSON。
    loadCoreLocaleMessages(lang),
  ]);
  // Vite 动态导入结果以 default 包裹消息；当前语言不存在时使用空对象。
  const localMessages = appLocaleMessages?.default ?? {};
  // 按“动态 < 公共静态 < Admin 静态”的优先级生成最终消息树。
  return mergeMessages(
    mergeMessages(dynamicMessages.value[lang] ?? {}, coreLocaleMessages),
    localMessages,
  );
}

/**
 * 用户实际切换语言时，同时更新消息树和第三方组件语言。
 */
async function loadMessages(lang: SupportedLanguagesType) {
  const [messages] = await Promise.all([
    buildMessages(lang),
    loadThirdPartyMessage(lang),
  ]);
  return messages;
}

/**
 * 替换数据库动态消息快照，并重新生成每个语言的运行时消息。
 *
 * 每个语言都会按既定优先级与静态消息重新合并，并通过 setLocaleMessage
 * 原位替换。该流程不改变活动语言，也不会切换 Element Plus 或 Day.js。
 */
async function applyDynamicMessages(
  bundle: Record<string, Record<string, unknown>>,
) {
  dynamicMessages.value = bundle;
  for (const locale of SUPPORTED_LOCALES) {
    const messages = await buildMessages(locale);
    i18n.global.setLocaleMessage(locale, messages);
  }
}

/**
 * 同步切换不受 vue-i18n 消息树管理的第三方库语言环境。
 */
async function loadThirdPartyMessage(lang: SupportedLanguagesType) {
  await Promise.all([loadElementLocale(lang), loadDayjsLocale(lang)]);
}

/**
 * 按需加载 dayjs 语言模块，并更新 dayjs 的全局 locale。
 * 未知语言回退到英文，保证日期格式化始终有可用配置。
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
 * 更新 Element Plus 的响应式语言对象，组件会随该 ref 自动刷新。
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

/**
 * 装配应用级国际化能力，并固定缺失消息的回退语言。
 *
 * coreSetup 负责注册 vue-i18n 和首次加载；本模块通过 loadMessages 注入公共、
 * 应用、数据库及第三方语言包的组合逻辑。调用方仍可传入选项覆盖默认配置。
 */
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
  setupI18n,
  SUPPORTED_LOCALES,
};
