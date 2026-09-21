package com.gnilc.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 十进制精度校验与无损格式化；业务舍入须由拥有该规则的流程显式执行。 */
public final class DecimalUtils {
    public static final int DEFAULT_DECIMAL_PLACES = 2;

    private DecimalUtils() {
    }

    /** 判断非空数值是否最多具有两位有效小数，尾随零不算额外精度。 */
    public static boolean hasAtMostDecimalPlaces(BigDecimal value) {
        return hasAtMostDecimalPlaces(value, DEFAULT_DECIMAL_PLACES);
    }

    /** 判断有效小数位是否超限；空值返回 false，负的小数位上限属于调用错误。 */
    public static boolean hasAtMostDecimalPlaces(BigDecimal value, int decimalPlaces) {
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("Decimal places must not be negative");
        }
        return value != null && Math.max(value.stripTrailingZeros().scale(), 0) <= decimalPlaces;
    }

    public static BigDecimal format(BigDecimal value) {
        return format(value, DEFAULT_DECIMAL_PLACES);
    }

    /** 调整到指定小数位，允许补零或移除尾随零；需要舍入非零小数时抛出异常。 */
    public static BigDecimal format(BigDecimal value, int decimalPlaces) {
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("Decimal places must not be negative");
        }
        return value.setScale(decimalPlaces, RoundingMode.UNNECESSARY);
    }

    public static String toPlainString(BigDecimal value) {
        return toPlainString(value, DEFAULT_DECIMAL_PLACES);
    }

    /** 无损格式化为固定小数位的普通十进制字符串，不使用科学计数法。 */
    public static String toPlainString(BigDecimal value, int decimalPlaces) {
        return format(value, decimalPlaces).toPlainString();
    }

    public static String toTrimmedString(BigDecimal value) {
        return toTrimmedString(value, DEFAULT_DECIMAL_PLACES);
    }

    /** 先校验可无损容纳的精度，再移除展示用尾随零；此方法不执行业务舍入。 */
    public static String toTrimmedString(BigDecimal value, int decimalPlaces) {
        return format(value, decimalPlaces).stripTrailingZeros().toPlainString();
    }
}
