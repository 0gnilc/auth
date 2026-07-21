import { describe, expect, it } from 'vitest';

import { collectLeafKeys, mergeMessages } from './messages';

describe('internationalization message composition', () => {
  it('lets local static messages override database messages at leaf level', () => {
    const databaseMessages = {
      menu: {
        dashboard: { title: 'Database dashboard' },
        i18n: { title: 'Database i18n' },
      },
    };
    const localMessages = {
      menu: {
        dashboard: { title: 'Local dashboard' },
      },
    };

    expect(mergeMessages(databaseMessages, localMessages)).toEqual({
      menu: {
        dashboard: { title: 'Local dashboard' },
        i18n: { title: 'Database i18n' },
      },
    });
  });

  it('keeps shared and app static messages above database messages', () => {
    const databaseMessages = {
      common: { confirm: 'Database confirm' },
      page: { title: 'Database page' },
    };
    const sharedMessages = { common: { confirm: 'Shared confirm' } };
    const appMessages = { page: { title: 'App page' } };

    expect(
      mergeMessages(
        mergeMessages(databaseMessages, sharedMessages),
        appMessages,
      ),
    ).toEqual({
      common: { confirm: 'Shared confirm' },
      page: { title: 'App page' },
    });
  });

  it('collects only leaf paths as reserved local keys', () => {
    const keys = new Set<string>();

    collectLeafKeys(
      {
        page: {
          i18n: {
            title: 'Internationalization',
          },
        },
      },
      '',
      keys,
    );

    expect([...keys]).toEqual(['page.i18n.title']);
  });
});
