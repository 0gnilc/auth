import { requestClient } from '#/api/request';

/** 按语言标识组织的管理员动态消息树；消息键全局唯一，分类不构成键身份。 */
export type I18nMessageBundle = Record<string, Record<string, unknown>>;

export async function getI18nMessageBundle() {
  return requestClient.post<I18nMessageBundle>(
    '/sys/i18n-message/bundle/admin',
  );
}
