import { getI18nMessageBundle } from '#/api/core/i18n-message';

import { applyDynamicMessages } from './index';

let loaded = false;
let loading: null | Promise<void> = null;

async function reloadDynamicMessages() {
  const bundle = await getI18nMessageBundle();
  await applyDynamicMessages(bundle);
  loaded = true;
}

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

async function clearDynamicMessages() {
  loaded = false;
  await applyDynamicMessages({});
}

export { clearDynamicMessages, ensureDynamicMessages, reloadDynamicMessages };
