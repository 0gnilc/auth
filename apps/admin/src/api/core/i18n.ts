import type { PageParams, PageResult } from '#/api/types';

import { requestClient } from '#/api/request';

export namespace I18nApi {
  export interface MessageValue {
    locale: string;
    value: string;
  }

  export interface Message {
    i18nKey: string;
    values: MessageValue[];
  }

  export interface PageItem extends Message {
    client: string;
  }

  export type Bundle = Record<string, Record<string, unknown>>;

  export type SaveInput = Pick<Message, 'i18nKey' | 'values'> & {
    previousKey?: string;
  };
}

export async function getI18nBundle() {
  return requestClient.post<I18nApi.Bundle>('/sys/i18n/bundle');
}

export async function getI18nPage(
  params?: PageParams & {
    client?: string;
    key?: string;
    locale?: string;
    value?: string;
  },
) {
  return requestClient.post<PageResult<I18nApi.PageItem>>(
    '/sys/i18n/page',
    params,
  );
}

export async function getI18nValues(i18nKey: string) {
  return requestClient.post<I18nApi.Message | null>('/sys/i18n/values', {
    i18nKey,
  });
}

export async function saveI18nMessage(input: I18nApi.SaveInput) {
  return requestClient.post<I18nApi.Message>('/sys/i18n/save', input);
}

export async function removeI18nMessage(i18nKey: string) {
  return requestClient.post<null>('/sys/i18n/remove', { i18nKey });
}
