import { describe, expect, it } from 'vitest';

import { trimToNull } from '../object';

describe('trimToNull', () => {
  it('normalizes own top-level string properties and returns the same object', () => {
    const inherited = { inherited: '  keep inherited  ' };
    const target = Object.assign(Object.create(inherited), {
      blank: '   ',
      nested: { value: '  keep nested  ' },
      name: '  Alice  ',
      nullable: null,
      order: 1,
    });

    const result = trimToNull(target);

    expect(result).toBe(target);
    expect(target).toMatchObject({
      blank: null,
      nested: { value: '  keep nested  ' },
      name: 'Alice',
      nullable: null,
      order: 1,
    });
    expect(target.inherited).toBe('  keep inherited  ');
  });

  it('preserves every excluded string property', () => {
    const target = {
      password: '  Strong#123  ',
      token: '   ',
      username: '  alice  ',
    };

    trimToNull(target, 'password', 'token');

    expect(target).toEqual({
      password: '  Strong#123  ',
      token: '   ',
      username: 'alice',
    });
  });
});
