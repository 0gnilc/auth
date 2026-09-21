import type { PageParams, PageResult } from '#/api/types';

import { requestClient } from '#/api/request';

export namespace I18nMessageApi {
  /** 一个语言标识对应的动态消息文本。 */
  export interface MessageValue {
    /** 内容语言标识。 */
    locale: string;
    /** 该语言对应的消息文本。 */
    value: string;
  }

  /** 全局唯一消息键及其各语言值。 */
  export interface Message {
    /** 跨语言、跨分类唯一的动态消息键。 */
    messageKey: string;
    /** 各语言的消息值列表。 */
    values: MessageValue[];
  }

  /** 动态消息管理记录，增加可变的管理分类。 */
  export interface MessageItem extends Message {
    /** 动态消息的管理分类，不参与消息键身份。 */
    category: string;
  }
}

export async function getI18nMessageCategories() {
  return requestClient.post<string[]>('/sys/i18n-message/categories');
}

/** 动态消息分页筛选条件；分类继承自消息记录，其他过滤条件均可省略。 */
export async function getI18nMessagePage(
  params?: {
    /** 消息键筛选文本；省略时不限定。 */
    key?: string;
    /** 按语言筛选；省略时不限定。 */
    locale?: string;
    /** 消息文本筛选条件；省略时不限定。 */
    value?: string;
  } & PageParams &
    Partial<Pick<I18nMessageApi.MessageItem, 'category'>>,
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
