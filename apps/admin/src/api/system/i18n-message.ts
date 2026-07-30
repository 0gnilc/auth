import type { PageParams, PageResult } from '#/api/types';

import { requestClient } from '#/api/request';

export namespace I18nMessageApi {
  export interface MessageValue {
    locale: string;
    value: string;
  }

  export interface Message {
    messageKey: string;
    values: MessageValue[];
  }

  export interface MessageItem extends Message {
    category: string;
  }
}

export async function getI18nMessageCategories() {
  return requestClient.post<string[]>('/sys/i18n-message/categories');
}

export async function getI18nMessagePage(
  params?: PageParams &
    Partial<Pick<I18nMessageApi.MessageItem, 'category'>> & {
      key?: string;
      locale?: string;
      value?: string;
    },
) {
  return requestClient.post<PageResult<I18nMessageApi.MessageItem>>(
    '/sys/i18n-message/page',
    params,
  );
}

export async function getI18nMessageValues(messageKey: string) {
  return requestClient.post<I18nMessageApi.MessageItem | null>(
    `/sys/i18n-message/values/${encodeURIComponent(messageKey)}`,
  );
}

export async function createI18nMessage(data: I18nMessageApi.MessageItem) {
  return requestClient.post<I18nMessageApi.MessageItem>(
    '/sys/i18n-message/create',
    data,
  );
}

export async function saveI18nMessage(data: I18nMessageApi.MessageItem) {
  return requestClient.post<I18nMessageApi.MessageItem>(
    '/sys/i18n-message/save',
    data,
  );
}

export async function removeI18nMessage(messageKey: string) {
  return requestClient.post<null>(
    `/sys/i18n-message/remove/${encodeURIComponent(messageKey)}`,
  );
}
