import { getI18nMessageBundle } from '#/api/core/i18n-message';

import { applyDynamicMessages } from './index';

// loaded 表示当前会话至少成功获取并应用过一次完整动态消息快照。
let loaded = false;
// 共享正在进行的请求，避免路由守卫等并发入口重复拉取同一份语言包。
let loading: null | Promise<void> = null;

/**
 * 强制从后端重新获取完整动态消息，并用新快照刷新运行时语言包。
 *
 * 该函数不吞掉请求或合并失败，保存页面可以据此分别提示“数据已保存但
 * 运行时刷新失败”。只有获取和应用都完成后才标记为已加载。
 */
async function reloadDynamicMessages() {
  const bundle = await getI18nMessageBundle();
  await applyDynamicMessages(bundle);
  loaded = true;
}

/**
 * 确保当前会话已经加载动态消息。
 *
 * 已成功加载时直接返回；首次并发调用共享同一个 Promise。失败会返回 false
 * 且清除进行中状态，后续导航可以再次尝试，不会被一次网络错误永久阻塞。
 */
async function ensureDynamicMessages() {
  if (loaded) {
    return true;
  }
  if (!loading) {
    loading = reloadDynamicMessages().finally(() => {
      loading = null;
    });
  }
  try {
    await loading;
    return true;
  } catch (error) {
    console.warn(
      'Failed to load dynamic internationalization messages.',
      error,
    );
    return false;
  }
}

/**
 * 清空当前客户端的动态消息快照，并允许后续流程重新加载。
 *
 * 典型调用场景是退出登录：本地静态语言包仍然有效，但上一个登录会话从
 * 数据库加载的菜单翻译必须立即移除，避免泄漏到下一个会话。
 */
async function clearDynamicMessages() {
  loaded = false;
  await applyDynamicMessages({});
}

export { clearDynamicMessages, ensureDynamicMessages, reloadDynamicMessages };
