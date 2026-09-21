package com.gnilc.core.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 展示脱敏工具测试。 */
class MaskUtilsTest {
    /** 单 Code Point 昵称完全隐藏。 */
    @Test
    void maskNicknameHidesSingleCodePoint() {
        assertThat(MaskUtils.maskNickname("Z")).isEqualTo("*");
    }

    /** 补充平面字符按 Unicode Code Point 保留首字符。 */
    @Test
    void maskNicknameHandlesSupplementaryUnicodeCodePoints() {
        assertThat(MaskUtils.maskNickname("😀Alpha")).isEqualTo("😀*****");
    }

    /** 不足四位的本地手机号全部保留。 */
    @Test
    void maskPhoneKeepsEveryDigitForShortNumbers() {
        assertThat(MaskUtils.maskPhone("234", "123")).isEqualTo("+234 123");
    }

    /** 超过四位的本地手机号仅展示末四位。 */
    @Test
    void maskPhoneKeepsOnlyLastFourDigitsForLongNumbers() {
        assertThat(MaskUtils.maskPhone("234", "8051234582"))
                .isEqualTo("+234 ******4582");
    }
}
