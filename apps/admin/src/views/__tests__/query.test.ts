import { setCurrentTimezone } from '@vben/utils';

import { afterEach, describe, expect, it } from 'vitest';

import { toUtcRange } from '../query';

describe('finance query ranges', () => {
  afterEach(() => {
    setCurrentTimezone();
  });

  it('显示时区中的闭区间两端都转换为 UTC', () => {
    setCurrentTimezone('Africa/Lagos');

    expect(toUtcRange(['2026-08-01 08:00:00', '2026-08-01 08:00:00'])).toEqual({
      from: '2026-08-01T07:00:00.000Z',
      to: '2026-08-01T07:00:00.000Z',
    });
  });

  it('maps a complete range to UTC instants', () => {
    expect(
      toUtcRange(['2026-08-01T10:15:30+08:00', '2026-08-02T10:15:30+08:00']),
    ).toEqual({
      from: '2026-08-01T02:15:30.000Z',
      to: '2026-08-02T02:15:30.000Z',
    });
  });

  it('returns no range for a non-array value', () => {
    expect(toUtcRange(undefined)).toEqual({});
    expect(toUtcRange('2026-08-01T10:15:30+08:00')).toEqual({});
  });

  it('未提供或不可解析的边界保持省略', () => {
    expect(toUtcRange([])).toEqual({ from: undefined, to: undefined });
    expect(toUtcRange(['2026-08-01T10:15:30+08:00'])).toEqual({
      from: '2026-08-01T02:15:30.000Z',
      to: undefined,
    });
    expect(toUtcRange(['not-a-date', ''])).toEqual({
      from: undefined,
      to: undefined,
    });
  });

  it('ignores range values after the supported start and end positions', () => {
    expect(
      toUtcRange([
        '2026-08-01T10:15:30+08:00',
        undefined,
        '2026-09-01T10:15:30+08:00',
      ]),
    ).toEqual({
      from: '2026-08-01T02:15:30.000Z',
      to: undefined,
    });
  });
});
