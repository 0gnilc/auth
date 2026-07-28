import { requestClient } from '#/api/request';

export type I18nMessageBundle = Record<string, Record<string, unknown>>;

export async function getI18nMessageBundle() {
  return requestClient.post<I18nMessageBundle>(
    '/sys/i18n-message/bundle/admin',
  );
}
