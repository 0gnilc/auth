import type { PageParams, PageResult } from '#/api/types';

import { requestClient } from '#/api/request';

export namespace I18nMessageApi {
  export interface MessageValue {
    locale: string;
    value: string;
  }

  export interface Message {
    i18nKey: string;
    values: MessageValue[];
  }

  export interface MessageItem extends Message {
    client: string;
  }

  export type Bundle = Record<string, Record<string, unknown>>;

  export type SaveInput = Pick<Message, 'i18nKey' | 'values'> & {
    previousKey?: string;
  };
}

export async function getI18nMessageBundle() {
  return requestClient.post<I18nMessageApi.Bundle>('/sys/i18n-message/bundle');
}

export async function getI18nMessagePage(
  params?: PageParams & {
    client?: string;
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

export async function getI18nMessageValues(i18nKey: string) {
  return requestClient.post<I18nMessageApi.Message | null>(
    `/sys/i18n-message/values/${encodeURIComponent(i18nKey)}`,
  );
}

export async function saveI18nMessage(input: I18nMessageApi.SaveInput) {
  return requestClient.post<I18nMessageApi.Message>(
    '/sys/i18n-message/save',
    input,
  );
}

export async function removeI18nMessage(i18nKey: string) {
  return requestClient.post<null>(
    `/sys/i18n-message/remove/${encodeURIComponent(i18nKey)}`,
  );
}
