package com.gnilc.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证字符串原样校验所采用的码点和空白规则，避免把校验误写成 trim 或规范化。 */
class StringRulesTest {
    @Test
    void singleSpacedTextAcceptsUnicodeAndSingleInternalSpaces() {
        assertThat(StringRules.isSingleSpacedText(
                "Àda 😀 Okafor", 100)).isTrue();
    }

    @Test
    void singleSpacedTextRejectsEdgeSpacesRepeatedSpacesAndTabs() {
        assertThat(StringRules.isSingleSpacedText(" Ada", 100)).isFalse();
        assertThat(StringRules.isSingleSpacedText("Ada  Okafor", 100))
                .isFalse();
        assertThat(StringRules.isSingleSpacedText("Ada\tOkafor", 100))
                .isFalse();
    }

    @Test
    void edgeWhitespaceUsesUnicodeCodePoints() {
        assertThat(StringRules.hasEdgeWhitespace(" value\u2003"))
                .isTrue();
        assertThat(StringRules.hasEdgeWhitespace("value"))
                .isFalse();
    }
}
