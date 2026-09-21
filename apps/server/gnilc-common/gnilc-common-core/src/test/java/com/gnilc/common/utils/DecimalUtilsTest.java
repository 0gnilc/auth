package com.gnilc.common.utils;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 区分有效小数位、展示补零和业务舍入，保护金额格式化的无损契约。 */
class DecimalUtilsTest {
    @Test
    void formatsWithoutRoundingAndKeepsPlainNotation() {
        assertThat(DecimalUtils.toPlainString(new BigDecimal("100.1"))).isEqualTo("100.10");
        assertThat(DecimalUtils.format(new BigDecimal("100.1200"))).isEqualTo(new BigDecimal("100.12"));
        assertThat(DecimalUtils.toPlainString(new BigDecimal("100.0"), 0)).isEqualTo("100");
        assertThat(DecimalUtils.toPlainString(new BigDecimal("100.1"), 4)).isEqualTo("100.1000");
        assertThat(DecimalUtils.toTrimmedString(new BigDecimal("1E+30"))).isEqualTo("1000000000000000000000000000000");
        assertThat(DecimalUtils.toTrimmedString(new BigDecimal("-0.0100"), 4)).isEqualTo("-0.01");
    }

    /** 100.12000 可无损变为两位，而 100.123 必须失败；负精度与 null 也不能被静默修正。 */
    @Test
    void checksEffectivePrecisionAndRejectsLossyFormatting() {
        assertThat(DecimalUtils.hasAtMostDecimalPlaces(new BigDecimal("100.12000"))).isTrue();
        assertThat(DecimalUtils.hasAtMostDecimalPlaces(new BigDecimal("100.123"))).isFalse();
        assertThat(DecimalUtils.hasAtMostDecimalPlaces(new BigDecimal("100.0"), 0)).isTrue();
        assertThat(DecimalUtils.hasAtMostDecimalPlaces(null)).isFalse();
        assertThatThrownBy(() -> DecimalUtils.format(new BigDecimal("100.123"))).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> DecimalUtils.format(new BigDecimal("0.1"), 0)).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> DecimalUtils.hasAtMostDecimalPlaces(BigDecimal.ZERO, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DecimalUtils.format(BigDecimal.ZERO, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DecimalUtils.format(null)).isInstanceOf(NullPointerException.class);
    }
}
