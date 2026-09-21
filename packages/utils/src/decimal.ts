import DecimalJs from 'decimal.js';

// 80 位计算精度覆盖 SQL DECIMAL 的 65 位聚合结果及当前百分比乘算，避免中间计算先丢失有效位。
export const Decimal = DecimalJs.clone({ precision: 80 });
export type Decimal = DecimalJs;

export const DEFAULT_DECIMAL_PLACES = 2;

/** 检查有效小数位而非字符串尾零数量；缺值返回 false，非法十进制字符串保留解析异常。 */
export function hasAtMostDecimalPlaces(
  value: Decimal | null | string | undefined,
  decimalPlaces = DEFAULT_DECIMAL_PLACES,
): boolean {
  if (!Number.isInteger(decimalPlaces) || decimalPlaces < 0) {
    throw new RangeError('Decimal places must be a non-negative integer.');
  }
  if (value === null || value === undefined) return false;
  const decimal = typeof value === 'string' ? new Decimal(value) : value;
  return decimal.isFinite() && decimal.decimalPlaces() <= decimalPlaces;
}

/** 仅格式化已满足精度的有限值；有效超精度会抛错，不通过截断或舍入掩盖输入错误。 */
export function format(
  value: Decimal,
  decimalPlaces = DEFAULT_DECIMAL_PLACES,
): Decimal {
  if (!hasAtMostDecimalPlaces(value, decimalPlaces)) {
    throw new RangeError(
      'Value must be finite and fit the requested decimal places.',
    );
  }
  return value.toDecimalPlaces(decimalPlaces, Decimal.ROUND_DOWN);
}

/** 输出固定小数位的普通十进制字符串，不使用科学计数法；超精度输入沿用 format 的失败契约。 */
export function toPlainString(
  value: Decimal,
  decimalPlaces = DEFAULT_DECIMAL_PLACES,
): `${number}` {
  return format(value, decimalPlaces).toFixed(decimalPlaces) as `${number}`;
}

/** 在同一精度校验后输出去除小数尾零的普通字符串，不改变金额有效值。 */
export function toTrimmedString(
  value: Decimal,
  decimalPlaces = DEFAULT_DECIMAL_PLACES,
): `${number}` {
  const decimal = format(value, decimalPlaces);
  return decimal.toFixed(decimal.decimalPlaces()) as `${number}`;
}
