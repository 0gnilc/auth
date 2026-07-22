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

// Element Plus 通过响应式引用消费当前语言配置，切换后无需重新挂载应用。
const elementLocale = ref<Language>(defaultLocale);
// vue-i18n 找不到当前语种消息时，统一回退到中文。
const DEFAULT_LOCALE: SupportedLanguagesType = 'zh-CN';
// 前后端共享同一组固定语言代码；数据库动态消息也只会返回这些语种。
const SUPPORTED_LOCALES: SupportedLanguagesType[] = ['zh-CN', 'en-US'];

// Vite 将每个语种目录包装为延迟加载函数，避免首屏一次加载全部静态 JSON。
const modules = import.meta.glob('./langs/**/*.json');
// 后端返回的是按语种组织的完整快照；替换整个对象才能同步反映已删除的 key。
const dynamicMessages = ref<Record<string, Record<string, unknown>>>({});
// 汇总公共和应用静态语言包的叶子 key，用于阻止保存不会生效的动态同名 key。
const staticKeys = new Set<string>();

// 将 ./langs/{locale}/{namespace}.json 聚合为按语种延迟加载的消息树。
const localesMap = loadLocalesMapFromDir(
  /\.\/langs\/([^/]+)\/(.*)\.json$/,
  modules,
);
/**
 * 生成指定语种最终交给 vue-i18n 的消息树。
 *
 * 动态消息优先级最低，公共静态语言包居中，应用静态语言包最高。这样后台
 * 可以配置菜单等运行时内容，但不能覆盖随版本发布的按钮、表单和页面文案。
 * 第三方库语言包与消息树并行加载，它们由各自的全局状态管理。
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

/**
 * 加载所有支持语种的静态语言包并收集叶子 key。
 *
 * 不能只检查当前语种，否则某个 key 只存在于另一语种时仍可能被错误写入
 * 数据库。Set 会自动去重，多次调用是幂等的，并复用已加载的模块结果。
 */
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

/**
 * 替换数据库动态消息快照，并重新生成每个语种的运行时消息。
 *
 * loadLocaleMessages 会重新调用上面的 loadMessages，因此每个语种都会按既定
 * 优先级与静态消息合并。该底层函数在加载过程中会切换当前语种，所以最后
 * 再加载进入函数前的活动语种，保证用户界面不会停留在循环中的最后一项。
 */
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
 * 同步切换不受 vue-i18n 消息树管理的第三方库语言环境。
 */
async function loadThirdPartyMessage(lang: SupportedLanguagesType) {
  await Promise.all([loadElementLocale(lang), loadDayjsLocale(lang)]);
}

/**
 * 按需加载 dayjs 语言模块，并更新 dayjs 的全局 locale。
 * 未知语种回退到英文，保证日期格式化始终有可用配置。
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
 * 装配应用级国际化能力，并固定缺失消息的回退语种。
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
  ensureStaticKeys,
  setupI18n,
  SUPPORTED_LOCALES,
};
