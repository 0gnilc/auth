import { describe, expect, it } from 'vitest';

import {
  Decimal,
  format,
  hasAtMostDecimalPlaces,
  toPlainString,
  toTrimmedString,
} from './decimal';

describe('exact decimal formatting', () => {
  // 尾零不增加有效小数位；整数提现、平台两位金额与 Provider 六位金额共享这一判断。
  it.each([
    ['100.1200', undefined, true],
    ['100.123', undefined, false],
    ['100.000', 0, true],
    ['100.1', 0, false],
    ['100.123456', 6, true],
    ['100.1234567', 6, false],
    ['99999999999999999.99', undefined, true],
  ])('按有效小数位检查 %s', (value, places, expected) => {
    expect(hasAtMostDecimalPlaces(value, places)).toBe(expected);
  });

  it('非法十进制字符串保留解析错误，不伪装成精度不符', () => {
    expect(() => hasAtMostDecimalPlaces('aaa124')).toThrow(
      '[DecimalError] Invalid argument: aaa124',
    );
    expect(hasAtMostDecimalPlaces('NaN')).toBe(false);
    expect(hasAtMostDecimalPlaces('Infinity')).toBe(false);
    expect(hasAtMostDecimalPlaces(undefined)).toBe(false);
    expect(() => hasAtMostDecimalPlaces('1', -1)).toThrow(RangeError);
  });

  it.each([
    ['100.1200', 2, '100.12', '100.12'],
    ['100.000', 0, '100', '100'],
    ['100.1', 4, '100.1000', '100.1'],
    ['-0.01', 2, '-0.01', '-0.01'],
  ])('格式化 %s 时不进行数值舍入', (input, places, fixed, trimmed) => {
    const value = new Decimal(input);
    expect(hasAtMostDecimalPlaces(value, places)).toBe(true);
    expect(format(value, places).eq(value)).toBe(true);
    expect(toPlainString(value, places)).toBe(fixed);
    expect(toTrimmedString(value, places)).toBe(trimmed);
  });

  it('默认两位精度，拒绝会丢失有效数字的格式化', () => {
    expect(toPlainString(new Decimal('100.1'))).toBe('100.10');
    expect(toTrimmedString(new Decimal('1e30'))).toBe(
      '1000000000000000000000000000000',
    );
    expect(() => format(new Decimal('100.123'))).toThrow(RangeError);
    expect(() => format(new Decimal('0.1'), 0)).toThrow(RangeError);
    expect(() => hasAtMostDecimalPlaces(new Decimal(0), -1)).toThrow(
      RangeError,
    );
    expect(hasAtMostDecimalPlaces(null)).toBe(false);
    expect(() => format(new Decimal(Infinity))).toThrow(RangeError);
    expect(() => format(new Decimal(Number.NaN))).toThrow(RangeError);
  });

  it('金额、百分比与 SQL 聚合中间结果保留有效精度', () => {
    expect(new Decimal('0.1').plus('0.2').toFixed(undefined)).toBe('0.3');
    expect(
      new Decimal('99999999999999999.99').times('99.99').toFixed(undefined),
    ).toBe('9998999999999999999.0001');
    expect(
      new Decimal(`${'9'.repeat(63)}.99`)
        .times('99.99')
        .dividedBy(100)
        .toFixed(undefined),
    ).toBe(`9998${'9'.repeat(59)}.990001`);
  });
});
