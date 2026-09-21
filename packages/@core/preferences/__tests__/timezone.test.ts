import { describe, expect, it } from 'vitest';

import { defaultPreferences } from '../src/config';
import { DEFAULT_TIME_ZONE_OPTIONS } from '../src/constants';

describe('timezone preferences', () => {
  it('offers Africa/Lagos without changing the existing default timezone', () => {
    expect(DEFAULT_TIME_ZONE_OPTIONS).toContainEqual({
      label: 'Africa/Lagos(GMT+1)',
      offset: 1,
      timezone: 'Africa/Lagos',
    });
    expect(defaultPreferences.app.timezone).toBe('Asia/Shanghai');
  });
});
