import { ref } from 'vue';

import { describe, expect, it, vi } from 'vitest';

import { useColumns, useGridFormSchema } from '../data';

vi.mock('#/locales', () => ({
  $t: (key: string) => key,
  SUPPORTED_LOCALES: ['en-US', 'zh-CN'],
}));

describe('internationalization message management grid', () => {
  it('shows category, message identity, every supported locale and actions', () => {
    const columns = useColumns() ?? [];

    expect(columns.map((column) => column.field)).toEqual([
      'category',
      'messageKey',
      'en-US',
      'zh-CN',
      'operation',
    ]);
  });

  it('uses the categories returned by the backend in the search form', () => {
    const categories = ref(['default', 'admin']);
    const schema = useGridFormSchema(categories);
    const categoryField = schema.find(
      ({ fieldName }) => fieldName === 'category',
    );
    const componentProps = categoryField?.componentProps;

    expect(typeof componentProps).toBe('function');
    expect(
      typeof componentProps === 'function'
        ? componentProps({}, {} as never)
        : componentProps,
    ).toMatchObject({
      options: [
        { label: 'default', value: 'default' },
        { label: 'admin', value: 'admin' },
      ],
    });
  });
});
